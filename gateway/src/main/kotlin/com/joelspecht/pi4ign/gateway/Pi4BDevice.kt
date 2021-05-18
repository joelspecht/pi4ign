package com.joelspecht.pi4ign.gateway

import com.inductiveautomation.ignition.gateway.opcua.server.api.DeviceContext
import com.inductiveautomation.ignition.gateway.opcua.server.api.DeviceType
import com.inductiveautomation.ignition.gateway.opcua.server.api.ManagedDevice
import com.pi4j.Pi4J
import com.pi4j.context.Context
import com.pi4j.io.gpio.digital.DigitalInputConfigBuilder
import com.pi4j.io.gpio.digital.DigitalOutputConfigBuilder
import com.pi4j.util.Console
import org.eclipse.milo.opcua.sdk.core.AccessLevel
import org.eclipse.milo.opcua.sdk.core.Reference
import org.eclipse.milo.opcua.sdk.server.api.DataItem
import org.eclipse.milo.opcua.sdk.server.api.MonitoredItem
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode
import org.eclipse.milo.opcua.sdk.server.nodes.filters.AttributeFilterContext
import org.eclipse.milo.opcua.sdk.server.nodes.filters.AttributeFilters
import org.eclipse.milo.opcua.sdk.server.util.SubscriptionModel
import org.eclipse.milo.opcua.stack.core.BuiltinDataType
import org.eclipse.milo.opcua.stack.core.Identifiers
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Raspberry Pi Version 4 Model B Device
 */
class Pi4BDevice(
    deviceType: DeviceType,
    deviceContext: DeviceContext,
    private val deviceSettings: Pi4BDeviceSettings
) : ManagedDevice(deviceType, deviceContext) {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    private val subscriptionModel: SubscriptionModel = SubscriptionModel(deviceContext.getServer(), this)

    private lateinit var piContext: Context

    init {
        lifecycleManager.addStartupTask(this::onStartup)
        lifecycleManager.addShutdownTask(this::onShutdown)
    }

    private fun onStartup() {
        val gatewayContext = deviceContext.getGatewayContext()
        val hook = GatewayHook.get(gatewayContext)
        piContext = hook.runWithModuleClassLoader {
            Pi4J.newContextBuilder()
                .autoDetect()
                .setDefaultPlatform(deviceSettings.getPlatform())
                .build()
        }

        subscriptionModel.startup()

        // create a folder node for our configured device
        val rootNode = UaFolderNode(
            nodeContext,
            deviceContext.nodeId(getName()),
            deviceContext.qualifiedName(String.format("[%s]", getName())),
            LocalizedText(String.format("[%s]", getName()))
        )

        // add the folder node to the server
        nodeManager.addNode(rootNode)

        // add a reference to the root "Devices" folder node
        rootNode.addReference(
            Reference(
                rootNode.nodeId,
                Identifiers.Organizes,
                deviceContext.getRootNodeId().expanded(),
                Reference.Direction.INVERSE
            )
        )

        val gpioFolderName = "GPIO"
        val gpioFolderNode = UaFolderNode(
            nodeContext,
            deviceContext.nodeId(gpioFolderName),
            deviceContext.qualifiedName(gpioFolderName),
            LocalizedText(gpioFolderName)
        )
        nodeManager.addNode(gpioFolderNode)

        // addOrganizes is just a helper method to an OPC UA "Organizes" references to a folder node
        rootNode.addOrganizes(gpioFolderNode)

        val gpioInputProvider = deviceSettings.getDigitalInputProvider()
        val gpioOutputProvider = deviceSettings.getDigitalOutputProvider()

        for (i in 0..27) {
            val gpioName = "GPIO%02d".format(i)

            val gpioInputName = "${gpioName}-IN"
            val gpioInputConfig = DigitalInputConfigBuilder.newInstance(piContext)
                .id(gpioInputName)
                .name(gpioInputName)
                .address(i)
                .provider(gpioInputProvider)
                .build()
            val gpioInput = piContext.create(gpioInputConfig)

            val gpioOutputName = "${gpioName}-OUT"
            val gpioOutputConfig = DigitalOutputConfigBuilder.newInstance(piContext)
                .id(gpioOutputName)
                .name(gpioOutputName)
                .address(i)
                .provider(gpioOutputProvider)
                .build()
            val gpioOutput = piContext.create(gpioOutputConfig)

            val gpioNode: UaVariableNode = UaVariableNode.builder(nodeContext)
                .setNodeId(deviceContext.nodeId(gpioName))
                .setBrowseName(deviceContext.qualifiedName(gpioName))
                .setDisplayName(LocalizedText(gpioName))
                .setDataType(BuiltinDataType.Boolean.nodeId)
                .setTypeDefinition(Identifiers.BaseDataVariableType)
                .setAccessLevel(AccessLevel.READ_WRITE)
                .setUserAccessLevel(AccessLevel.READ_WRITE)
                .build()

            gpioNode.filterChain.addLast(
                AttributeFilters.getValue {
                    DataValue(Variant(gpioInput.isHigh))
                }
            )

            gpioNode.filterChain.addLast(AttributeFilters.setValue { _: AttributeFilterContext.SetAttributeContext, value: DataValue ->
                logger.debug("setValue: {}", value.value.value)
                // ctx.setAttribute(AttributeId.Value, value)
                gpioOutput.setState(value.value.value as Boolean)
            })

            nodeManager.addNode(gpioNode)
            gpioFolderNode.addOrganizes(gpioNode)
        }

        // fire initial subscription creation
        val dataItems = deviceContext.getSubscriptionModel().getDataItems(getName()).toMutableList()
        onDataItemsCreated(dataItems)

        val console = Console()
        PrintInfo.printLoadedPlatforms(console, piContext)
        PrintInfo.printDefaultPlatform(console, piContext)
        PrintInfo.printProviders(console, piContext)
        PrintInfo.printRegistry(console, piContext)
    }

    private fun onShutdown() {
        subscriptionModel.shutdown()

        piContext.shutdown()
    }

    override fun getStatus(): String {
        return if (subscriptionModel.isRunning) {
            "Running"
        } else {
            "Stopped"
        }
    }

    override fun onDataItemsCreated(dataItems: MutableList<DataItem>?) {
        subscriptionModel.onDataItemsCreated(dataItems)
    }

    override fun onDataItemsDeleted(dataItems: MutableList<DataItem>?) {
        subscriptionModel.onDataItemsDeleted(dataItems)
    }

    override fun onDataItemsModified(dataItems: MutableList<DataItem>?) {
        subscriptionModel.onDataItemsModified(dataItems)
    }

    override fun onMonitoringModeChanged(monitoredItems: MutableList<MonitoredItem>?) {
        subscriptionModel.onMonitoringModeChanged(monitoredItems)
    }

}
