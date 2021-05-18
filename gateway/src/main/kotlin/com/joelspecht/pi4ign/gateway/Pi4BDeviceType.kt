package com.joelspecht.pi4ign.gateway

import com.inductiveautomation.ignition.gateway.localdb.persistence.PersistentRecord
import com.inductiveautomation.ignition.gateway.localdb.persistence.RecordMeta
import com.inductiveautomation.ignition.gateway.localdb.persistence.ReferenceField
import com.inductiveautomation.ignition.gateway.opcua.server.api.Device
import com.inductiveautomation.ignition.gateway.opcua.server.api.DeviceContext
import com.inductiveautomation.ignition.gateway.opcua.server.api.DeviceSettingsRecord
import com.inductiveautomation.ignition.gateway.opcua.server.api.DeviceType

class Pi4BDeviceType : DeviceType(TYPE_ID, NAME_KEY, DESCRIPTION_KEY) {

    companion object {
        const val TYPE_ID: String = "com.joelspecht.pi4ign.gateway.Pi4BDeviceType"
        private const val NAME_KEY: String = "pi4ign.Pi4BDeviceType.Name"
        private const val DESCRIPTION_KEY: String = "pi4ign.Pi4BDeviceType.Desc"
    }

    override fun createDevice(context: DeviceContext, settings: DeviceSettingsRecord): Device {
        val pi4BDeviceSettings: Pi4BDeviceSettings = findProfileSettingsRecord(context.getGatewayContext(), settings)
        return Pi4BDevice(this, context, pi4BDeviceSettings)
    }

    override fun getSettingsRecordForeignKey(): ReferenceField<*> {
        return Pi4BDeviceSettings.DEVICE_SETTINGS
    }

    override fun getSettingsRecordType(): RecordMeta<out PersistentRecord> {
        return Pi4BDeviceSettings.META
    }

}
