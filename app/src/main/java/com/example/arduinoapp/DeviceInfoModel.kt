package com.droiduino.bluetoothconn

class DeviceInfoModel {

    private var deviceName: String = ""
        private set

    private var deviceHardwareAddress: String = ""
        private set

    constructor() {}

    constructor(deviceName: String, deviceHardwareAddress: String) {
        this.deviceName = deviceName
        this.deviceHardwareAddress = deviceHardwareAddress
    }

    // Custom getter for deviceName
    fun getDeviceName(): String {
        return deviceName
    }

    // Custom getter for deviceHardwareAddress
    fun getDeviceHardwareAddress(): String {
        return deviceHardwareAddress
    }
}
