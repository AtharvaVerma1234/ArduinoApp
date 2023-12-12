package com.droiduino.bluetoothconn

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.arduinoapp.R

class DeviceListAdapter(private val context: Context, private val deviceList: List<Any>) :
    RecyclerView.Adapter<DeviceListAdapter.DeviceViewHolder>() {
    class DeviceViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var textName: TextView
        var textAddress: TextView
        var linearLayout: LinearLayout
        init {
            textName = v.findViewById(R.id.textViewDeviceName)
            textAddress = v.findViewById(R.id.textViewDeviceAddress)
            linearLayout = v.findViewById(R.id.linearLayoutDeviceInfo)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val v: View =
            LayoutInflater.from(parent.context).inflate(R.layout.device_info_layout, parent)
        return DeviceViewHolder(v)
    }
    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val itemHolder = holder
        val deviceInfoModel: DeviceInfoModel = deviceList[position] as DeviceInfoModel
        itemHolder.textName.setText(deviceInfoModel.getDeviceName())
        itemHolder.textAddress.setText(deviceInfoModel.getDeviceHardwareAddress())

        // When a device is selected
        itemHolder.linearLayout.setOnClickListener {
            val intent = Intent(context, MainActivity::class.java)
            // Send device details to the MainActivity
            intent.putExtra("deviceName", deviceInfoModel.getDeviceName())
            intent.putExtra("deviceAddress", deviceInfoModel.getDeviceHardwareAddress())
            // Call MainActivity
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return deviceList.size
    }
}