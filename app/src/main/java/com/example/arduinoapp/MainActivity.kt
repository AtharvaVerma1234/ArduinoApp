package com.droiduino.bluetoothconn

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.arduinoapp.R
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

private var deviceName: String? = null
private lateinit var deviceAddress: String
private lateinit var handler: Handler
private lateinit var mmSocket: BluetoothSocket
private lateinit var connectedThread: MainActivity.ConnectedThread
private lateinit var createConnectThread: MainActivity.CreateConnectThread

private val CONNECTING_STATUS = 1 // used in Bluetooth handler to identify message status
private val MESSAGE_READ = 2 // used in Bluetooth handler to identify message update

class MainActivity : AppCompatActivity() {

//    private var deviceName: String? = null
//    private lateinit var deviceAddress: String
//    private lateinit var handler: Handler
//    private lateinit var mmSocket: BluetoothSocket
//    private lateinit var connectedThread: ConnectedThread
//    private lateinit var createConnectThread: CreateConnectThread
//
//    private val CONNECTING_STATUS = 1 // used in Bluetooth handler to identify message status
//    private val MESSAGE_READ = 2 // used in Bluetooth handler to identify message update

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // UI Initialization
        val buttonConnect = findViewById<Button>(R.id.buttonConnect)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        progressBar.visibility = View.GONE
        val textViewInfo = findViewById<TextView>(R.id.textViewInfo)
        val buttonToggle = findViewById<Button>(R.id.buttonToggle)
        buttonToggle.isEnabled = false
        val imageView = findViewById<ImageView>(R.id.imageView)
        imageView.setBackgroundColor(resources.getColor(R.color.colorOff))

        handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    CONNECTING_STATUS -> when (msg.arg1) {
                        1 -> {
                            toolbar.subtitle = "Connected to $deviceName"
                            progressBar.visibility = View.GONE
                            buttonConnect.isEnabled = true
                            buttonToggle.isEnabled = true
                        }
                        -1 -> {
                            toolbar.subtitle = "Device fails to connect"
                            progressBar.visibility = View.GONE
                            buttonConnect.isEnabled = true
                        }
                    }
                    MESSAGE_READ -> {
                        val arduinoMsg = msg.obj.toString() // Read message from Arduino
                        when (arduinoMsg.lowercase()) {
                            "led is turned on" -> {
                                imageView.setBackgroundColor(resources.getColor(R.color.colorOn))
                                textViewInfo.text = "Arduino Message : $arduinoMsg"
                            }
                            "led is turned off" -> {
                                imageView.setBackgroundColor(resources.getColor(R.color.colorOff))
                                textViewInfo.text = "Arduino Message : $arduinoMsg"
                            }
                        }
                    }
                }
            }
        }

        // If a Bluetooth device has been selected from SelectDeviceActivity
        deviceName = intent.getStringExtra("deviceName")
        if (deviceName != null) {
            // Get the device address to make BT Connection
            deviceAddress = intent.getStringExtra("deviceAddress")!!
            // Show progress and connection status
            toolbar.subtitle = "Connecting to $deviceName..."
            progressBar.visibility = View.VISIBLE
            buttonConnect.isEnabled = false

            /*
            This is the most important piece of code. When "deviceName" is found
            the code will call a new thread to create a Bluetooth connection to the
            selected device (see the thread code below)
             */
            val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
            createConnectThread = CreateConnectThread(bluetoothAdapter!!, deviceAddress,handler)
            createConnectThread.start()
        }

        /*
        Second most important piece of Code. GUI Handler
         */


        // Select Bluetooth Device
        buttonConnect.setOnClickListener {
            // Move to adapter list
            val intent = Intent(this@MainActivity, SelectDeviceActivity::class.java)
            startActivity(intent)
        }

        // Button to ON/OFF LED on Arduino Board
        buttonToggle.setOnClickListener {
            var cmdText: String? = null
            val btnState = buttonToggle.text.toString().lowercase()
            when (btnState) {
                "turn on" -> {
                    buttonToggle.text = "Turn Off"
                    // Command to turn on LED on Arduino. Must match with the command in Arduino code
                    cmdText = "<turn on>"
                }
                "turn off" -> {
                    buttonToggle.text = "Turn On"
                    // Command to turn off LED on Arduino. Must match with the command in Arduino code
                    cmdText = "<turn off>"
                }
            }
            // Send command to Arduino board
            connectedThread.write(cmdText!!)
        }
    }

    /* ============================ Thread to Create Bluetooth Connection =================================== */
    @SuppressLint("MissingPermission")
    internal class CreateConnectThread(bluetoothAdapter: BluetoothAdapter, address: String,handler: Handler) : Thread() {

        /*
        Use a temporary object that is later assigned to mmSocket
        because mmSocket is final.
         */
        private val handler1:Handler=handler
        private val mmSocket: BluetoothSocket
        private val uuid: UUID = bluetoothAdapter.getRemoteDevice(address).uuids[0].uuid

        init {
            var tmp: BluetoothSocket? = null
            try {
                /*
                Get a BluetoothSocket to connect with the given BluetoothDevice.
                Due to Android device varieties,the method below may not work for different devices.
                You should try using other methods i.e. :
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                 */
                tmp = bluetoothAdapter.getRemoteDevice(address)
                    .createInsecureRfcommSocketToServiceRecord(uuid)
            } catch (e: IOException) {
                Log.e(TAG, "Socket's create() method failed", e)
            }
            mmSocket = tmp!!
        }

        override fun run() {
            // Cancel discovery because it otherwise slows down the connection.
            val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            bluetoothAdapter.cancelDiscovery()
            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect()
                Log.e("Status", "Device connected")
                handler1.obtainMessage(CONNECTING_STATUS, 1, -1).sendToTarget()
            } catch (connectException: IOException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close()
                    Log.e("Status", "Cannot connect to device")
                    handler1.obtainMessage(CONNECTING_STATUS, -1, -1).sendToTarget()
                } catch (closeException: IOException) {
                    Log.e(TAG, "Could not close the client socket", closeException)
                }
                return
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            connectedThread = ConnectedThread(mmSocket)
            connectedThread.run()
        }

        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            try {
                mmSocket.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the client socket", e)
            }
        }
    }

    /* =============================== Thread for Data Transfer =========================================== */
    internal class ConnectedThread(socket: BluetoothSocket) : Thread() {
        private val mmSocket: BluetoothSocket = socket
        private val mmInStream: InputStream
        private val mmOutStream: OutputStream

        init {
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.inputStream
                tmpOut = socket.outputStream
            } catch (e: IOException) {
            }

            mmInStream = tmpIn!!
            mmOutStream = tmpOut!!
        }

        override fun run() {
            val buffer = ByteArray(1024)  // buffer store for the stream
            var bytes: Int // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    /*
                    Read from the InputStream from Arduino until the termination character is reached.
                    Then send the whole String message to GUI Handler.
                     */
                    bytes = mmInStream.read(buffer)
                    var readMessage: String
                    if (buffer[bytes].toChar() == '\n') {
                        readMessage = String(buffer, 0, bytes)
                        Log.e("Arduino Message", readMessage)
                        handler.obtainMessage(MESSAGE_READ, readMessage).sendToTarget()
                        bytes = 0
                    } else {
                        bytes++
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    break
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        fun write(input: String) {
            val bytes = input.toByteArray() // converts entered String into bytes
            try {
                mmOutStream.write(bytes)
            } catch (e: IOException) {
                Log.e("Send Error", "Unable to send message", e)
            }
        }

        /* Call this from the main activity to shutdown the connection */
        fun cancel() {
            try {
                mmSocket.close()
            } catch (e: IOException) {
            }
        }
    }

    /* ============================ Terminate Connection at BackPress ====================== */
    override fun onBackPressed() {
        // Terminate Bluetooth Connection and close app
        if (createConnectThread != null) {
            createConnectThread.cancel()
        }
        val a = Intent(Intent.ACTION_MAIN)
        a.addCategory(Intent.CATEGORY_HOME)
        a.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(a)
    }
}
