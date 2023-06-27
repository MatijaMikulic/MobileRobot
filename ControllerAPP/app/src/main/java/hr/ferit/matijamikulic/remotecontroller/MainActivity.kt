package hr.ferit.matijamikulic.remotecontroller

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.charset.Charset

class MainActivity : AppCompatActivity() {

    companion object{
        const val DEBUG_TAG = "BL CONNECTION"
        //esp32  BLUETOOTH DEVICE
        const val MAC_ADDRESS = "7C:87:CE:30:79:56"
        const val UUID = "00001101-0000-1000-8000-00805f9b34fb"

        //HC-06 BLUETOOTH DEVICE
        //const val MAC_ADDRESS = "98:DA:60:04:F8:EA"
        //const val UUID = "00001101-0000-1000-8000-00805f9b34fb"
    }

    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var device: BluetoothDevice
    private lateinit var connectBtn: Button
    private lateinit var disconnectBtn: Button
    private var bluetoothService:BluetoothService? = null

    //handling received and sent data
    private val handler = object: Handler(Looper.getMainLooper()){
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MESSAGE_READ -> {
                    val numBytes = msg.arg1
                    val readBuf = msg.obj as ByteArray
                    val readMsg = readBuf.copyOfRange(0, numBytes).toString(Charset.defaultCharset())
                    //textView.text=readMsg
                    // Handle incoming message
                }
                MESSAGE_WRITE -> {
                    val writeBuf = msg.obj as ByteArray
                    // Handle outgoing message
                }
                MESSAGE_TOAST -> {
                    val toastMsg = msg.data.getString("toast")
                    Toast.makeText(applicationContext, toastMsg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    //Creating Coroutine that connects android device to HC-06 BL module
    private fun connectDevices() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                //getting device and opening socket for communication
                device = bluetoothAdapter.getRemoteDevice(MAC_ADDRESS)
                val socket = device.createRfcommSocketToServiceRecord(java.util.UUID.fromString(UUID))
                bluetoothAdapter.cancelDiscovery()

                //connecting devices
                socket.connect()
                bluetoothService = BluetoothService(handler)
                bluetoothService?.startCommunication(socket!!)
            } catch (e: Exception) {
                Log.e(DEBUG_TAG, "Could not connect to device ${device.name}", e)
            }
        }
    }

    //checking if android device has bluetooth enabled, if not aks permission to enable it
    private val enableBTLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){ result ->
        if(result.resultCode == Activity.RESULT_OK) {
            connectDevices()
        }else{
            Log.d(DEBUG_TAG,"Cannot connect to bluetooth.")
            Toast.makeText(this,"Cannot connect to bluetooth.", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initComponents()

        if(!bluetoothAdapter.isEnabled){
            val enableBTIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBTLauncher.launch(enableBTIntent)
        }else{
            //connectDevices()
        }

        connectBtn.setOnClickListener {
            connectDevices()
        }
        disconnectBtn.setOnClickListener {
            bluetoothService?.cancel()
        }
    }

    private fun initComponents(){
        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        connectBtn = findViewById(R.id.connect)
        disconnectBtn = findViewById(R.id.disconnect_btn)
    }

    //close socket
    override fun onDestroy() {
        super.onDestroy()

        bluetoothService?.cancel()

    }
}