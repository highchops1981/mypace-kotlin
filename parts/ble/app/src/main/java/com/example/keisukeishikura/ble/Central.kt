package com.example.keisukeishikura.ble

/**
 * Created by keisukeishikura on 2017/11/15.
 */

import android.annotation.SuppressLint
import android.util.Log
import android.content.Context
import java.util.*

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothGatt
import android.bluetooth.le.*

@SuppressLint("StaticFieldLeak")
object Central {

    private val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private val scanner: BluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
    private var deviceList: MutableList<BluetoothDevice> = mutableListOf()
    private var bluetoothGattByDevice: BluetoothGatt? = null
    private lateinit var sbxContext: Context
    private lateinit var scanCallback: ScanCallback
    private lateinit var sbxImei: String

    private const val LOCAL_NAME = "YOUR_LOCAL_NAME"
    private const val RSSI_BORDER = -100
    private const val SERVICE_UUID = "AAAAAAAA-7DD0-4B74-81D7-5E2114AB267E"
    private val CHAR_UUIDS = mapOf("CHAR1" to "BBBBBBBB-62E1-45CC-9EF8-6747C01AD55A",
            "CHAR2" to "CCCCCCCC-A596-4B5D-92BE-4D3A1892F989",
            "CHAR3" to "DDDDDDDD-9B1E-428F-96EC-BD708B34EF87")

    private var isSendable = false
    private lateinit var callBack: (Boolean) -> Unit

    fun init(context: Context, imei: String) {

        sbxContext = context
        sbxImei = imei

//        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            // 権限がない場合はリクエスト
//            // requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
//            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),1)
//        }
//
//        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
//
//        if (mBluetoothAdapter == null) {
//            // Device does not support Bluetooth
//            Log.d("print", "Device does not support Bluetooth")
//        }
//
//        if (!mBluetoothAdapter.isEnabled()) {
//            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
//            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
//        }

    }


    fun send(type: String, data: ByteArray, isReSend: Boolean) {

        Log.d("print","::::send:::data:::" + data)

        if (isSendable && bluetoothGattByDevice != null) {

            val gottenService: BluetoothGattService = bluetoothGattByDevice?.getService(UUID.fromString(SERVICE_UUID)) ?: return
            val characteristic = gottenService.getCharacteristic(UUID.fromString(CHAR_UUIDS[type]))

            characteristic?.setValue(data)

            if (characteristic != null) {

                val writeOk = bluetoothGattByDevice?.writeCharacteristic(characteristic) ?: return

                Log.d("print", "writeOk::::" + writeOk)
                if (!writeOk && isReSend) {

                    Log.d("print", "reSend::::Go")

                    send(type,data,isReSend)

                }

            }

        } else {

        }

    }

    fun scan(body: (Boolean) -> Unit ) {

        Log.d("print", "::::scan start")

        callBack = body

        scanCallback = initScanCallbacks()
        scanner.startScan(scanCallback)

    }

    fun reScan(){

        isSendable = false
        bluetoothGattByDevice?.close()
        deviceList = mutableListOf()
        scanner.stopScan(scanCallback)
        scanner.startScan(scanCallback)

    }

    fun stop() {

        if (isSendable) {

            isSendable = false
            bluetoothGattByDevice?.close()
            deviceList = mutableListOf()
            scanner.stopScan(scanCallback)

        }

    }

    private fun initScanCallbacks(): ScanCallback {

        return object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {

                if (result != null && result.device != null) {

                    if (result.device.name == LOCAL_NAME) {

                        Log.d("print", "::::rssi:::" + result.rssi.toString())


                        if (result.rssi >= RSSI_BORDER) {

                            if (!deviceList.contains(result.device)) {

                                deviceList.add(result.device)
                                connect(result.device)

                            }

                        } else {

                            if (isSendable) {

                                Log.d("print", "::::device far away")

                                reScan()

                            }

                        }

                    }
                }

            }

            override fun onBatchScanResults(results: List<ScanResult>) {
            }

            override fun onScanFailed(errorCode: Int) {
            }

        }

    }

    private fun connect(device: BluetoothDevice) {

        val gattCallback = gattCallback()
        bluetoothGattByDevice = device.connectGatt(sbxContext, false, gattCallback) ?: return
        bluetoothGattByDevice?.connect()
    }

    private fun gattCallback(): BluetoothGattCallback {


        return object : BluetoothGattCallback() {

            override fun onConnectionStateChange (gatt: BluetoothGatt, status: Int, newState: Int) {

                if (newState == BluetoothProfile.STATE_CONNECTED) {

                    Log.d("print", "::::connected")
                    gatt.discoverServices()

                } else {

                    if (newState == BluetoothProfile.STATE_DISCONNECTED) {

                        Log.d("print", "::::bluetooth disconnected")

                        reScan()

                    }

                }

            }

            override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {

            }

            override fun onServicesDiscovered (gatt: BluetoothGatt, status:Int) {

                Log.d("print", "::::services discoverd")

                val gottenService: BluetoothGattService = bluetoothGattByDevice?.getService(UUID.fromString(SERVICE_UUID)) ?: return

                CHAR_UUIDS.forEach {

                    if (gottenService.getCharacteristic(UUID.fromString(it.value)) != null) {

                        if (!isSendable && it.value == CHAR_UUIDS["CHAR1"]) {

                            Log.d("print", "::::char discoverd")
                            isSendable = true
                            callBack(isSendable)

                        }

                    }

                }

            }

        }

    }

}

