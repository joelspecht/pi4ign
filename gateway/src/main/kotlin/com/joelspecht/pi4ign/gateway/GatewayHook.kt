package com.joelspecht.pi4ign.gateway

import com.inductiveautomation.ignition.common.BundleUtil
import com.inductiveautomation.ignition.common.licensing.LicenseState
import com.inductiveautomation.ignition.common.util.LoggerEx
import com.inductiveautomation.ignition.gateway.model.GatewayContext
import com.inductiveautomation.ignition.gateway.opcua.server.api.AbstractDeviceModuleHook
import com.inductiveautomation.ignition.gateway.opcua.server.api.DeviceType
import com.joelspecht.pi4ign.Pi4IgnModule
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.util.function.Supplier

class GatewayHook : AbstractDeviceModuleHook() {

    private val log = LoggerEx.newBuilder().build(javaClass)

    private lateinit var pathToPluginsDir: Path
    private lateinit var classLoader: URLClassLoader

    override fun setup(context: GatewayContext) {
        super.setup(context)

        BundleUtil.get().addBundle("pi4ign", javaClass, "pi4ign")

        val pathToModuleDir = context.systemManager.dataDir.toPath()
            .resolve("modules")
            .resolve(Pi4IgnModule.MODULE_ID)
        val modulesDirIsNew = Files.notExists(pathToModuleDir)
        Files.createDirectories(pathToModuleDir)

        pathToPluginsDir = pathToModuleDir.resolve("plugins")
        Files.createDirectories(pathToPluginsDir)

        if (modulesDirIsNew) {
            val libDirUrl = javaClass.getResource("")
            if (libDirUrl != null) {
                val fileSystem = FileSystems.newFileSystem(libDirUrl.toURI(), emptyMap<String, Any>())
                fileSystem.use {
                    val pathToLibDir = fileSystem.getPath("/lib")
                    val ds = Files.newDirectoryStream(pathToLibDir, Files::isRegularFile)
                    ds.use {
                        ds.forEach {
                            val dest = pathToPluginsDir.resolve(it.fileName.toString())
                            log.infof("Copying default plugin %s", dest)
                            Files.copy(it, dest)
                        }
                    }
                }
            }
        }
    }

    override fun startup(activationState: LicenseState) {
        val pluginJarUrls: Array<URL>
        val ds = Files.newDirectoryStream(pathToPluginsDir, Files::isRegularFile)
        ds.use {
            pluginJarUrls = ds.map { it.toUri().toURL() }.toTypedArray()
        }
        classLoader = URLClassLoader(pluginJarUrls, javaClass.classLoader)

        super.startup(activationState)
    }

    override fun shutdown() {
        super.shutdown()

        BundleUtil.get().removeBundle("pi4ign")

        classLoader.close()
    }

    override fun isFreeModule(): Boolean {
        return true
    }

    override fun isMakerEditionCompatible(): Boolean {
        return true
    }

    override fun getDeviceTypes(): List<DeviceType> {
        return mutableListOf(Pi4BDeviceType())
    }

    fun <T> runWithModuleClassLoader(s: Supplier<T>): T {
        // Pi4J uses a ServiceLoader which uses the current thread's context class loader to load runtime plugins
        // we need to set the current thread's class loader to the Module's class loader which was used to instantiate
        // the hook in order for PI4J and its Service Loader to be able to discover all of the necessary plugins
        val previousClassLoader = Thread.currentThread().contextClassLoader
        try {
            Thread.currentThread().contextClassLoader = classLoader
            return s.get()
        } finally {
            Thread.currentThread().contextClassLoader = previousClassLoader
        }
    }

    companion object {

        fun get(gatewayContext: GatewayContext): GatewayHook {
            return gatewayContext.moduleManager.getModule(Pi4IgnModule.MODULE_ID).hook as GatewayHook
        }

    }

}
