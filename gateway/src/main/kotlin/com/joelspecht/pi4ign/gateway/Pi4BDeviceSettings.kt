package com.joelspecht.pi4ign.gateway

import com.inductiveautomation.ignition.gateway.localdb.persistence.*
import com.inductiveautomation.ignition.gateway.opcua.server.api.DeviceSettingsRecord
import com.pi4j.io.IOType
import com.pi4j.io.gpio.digital.DigitalInputProvider
import com.pi4j.io.gpio.digital.DigitalOutputProvider
import com.pi4j.platform.Platform
import simpleorm.dataset.SFieldFlags

class Pi4BDeviceSettings : PersistentRecord() {

    companion object {

        val META: RecordMeta<Pi4BDeviceSettings> = RecordMeta(
            Pi4BDeviceSettings::class.java,
            "Pi4BDeviceSettings"
        )

        private val DEVICE_SETTINGS_ID: LongField = LongField(META, "DeviceSettingsId", SFieldFlags.SPRIMARY_KEY)

        val DEVICE_SETTINGS: ReferenceField<DeviceSettingsRecord> = ReferenceField(
            META,
            DeviceSettingsRecord.META,
            "DeviceSettings",
            DEVICE_SETTINGS_ID
        )

        private val PLATFORM: StringField = StringField(META, "Platform", SFieldFlags.SMANDATORY)

        private val DIGITAL_INPUT_PROVIDER: StringField = StringField(
            META,
            "DigitalInputProvider",
            SFieldFlags.SMANDATORY
        )

        private val DIGITAL_OUTPUT_PROVIDER: StringField = StringField(
            META,
            "DigitalOutputProvider",
            SFieldFlags.SMANDATORY
        )

        init {
            Category("Pi4BDeviceSettings.Category.Main", 1001)
                .include(PLATFORM, DIGITAL_INPUT_PROVIDER, DIGITAL_OUTPUT_PROVIDER)
            DEVICE_SETTINGS.formMeta.isVisible = false
            PLATFORM.formMeta.editorSource = DropDownEditorSource { piContext ->
                val defaultPlatform = piContext.defaultPlatform<Platform>()
                piContext.platforms()
                    .all
                    .values
                    .map { Pair(it.id, it.name) }
                    .toList()
                    .sortedWith(comparator(defaultPlatform.id))
            }
            DIGITAL_INPUT_PROVIDER.formMeta.editorSource = DropDownEditorSource { piContext ->
                val defaultDigitalInputProvider = piContext.getDigitalInputProvider<DigitalInputProvider>()
                piContext.providers()
                    .all<DigitalInputProvider>(IOType.DIGITAL_INPUT)
                    .values
                    .map { Pair(it.id, it.name) }
                    .toList()
                    .sortedWith(comparator(defaultDigitalInputProvider.id))
            }
            DIGITAL_OUTPUT_PROVIDER.formMeta.editorSource = DropDownEditorSource { piContext ->
                val defaultDigitalOutputProvider = piContext.getDigitalOutputProvider<DigitalOutputProvider>()
                piContext.providers()
                    .all<DigitalOutputProvider>(IOType.DIGITAL_OUTPUT)
                    .values
                    .map { Pair(it.id, it.name) }
                    .toList()
                    .sortedWith(comparator(defaultDigitalOutputProvider.id))
            }
        }

        private fun comparator(defaultValue: String): Comparator<Pair<String, String>> {
            return Comparator { a, b ->
                when {
                    a.first == defaultValue -> -1
                    b.first == defaultValue -> 1
                    else -> 0
                }
            }
        }

    }

    override fun getMeta(): RecordMeta<Pi4BDeviceSettings> {
        return META
    }

    fun getPlatform(): String {
        return getString(PLATFORM)
    }

    fun getDigitalInputProvider(): String {
        return getString(DIGITAL_INPUT_PROVIDER)
    }

    fun getDigitalOutputProvider(): String {
        return getString(DIGITAL_OUTPUT_PROVIDER)
    }

}
