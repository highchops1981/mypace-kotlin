package com.example.keisukeishikura.ble

/**
 * Created by keisukeishikura on 2017/11/15.
 */

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import java.util.*
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.le.BluetoothLeAdvertiser
import kotlin.concurrent.schedule


@SuppressLint("StaticFieldLeak")
object Peripheral {

    private const val SERVICE_UUID = "AAAAAAAA-7DD0-4B74-81D7-5E2114AB267E"
    private val CHAR_UUIDS = mapOf("CHAR1" to "BBBBBBBB-62E1-45CC-9EF8-6747C01AD55A",
            "CHAR2" to "CCCCCCCC-A596-4B5D-92BE-4D3A1892F989",
            "CHAR3" to "DDDDDDDD-9B1E-428F-96EC-BD708B34EF87")

    private lateinit var sbxContext: Context
    private var gattServer: BluetoothGattServer? = null
    private var connectedDevice:BluetoothDevice? = null
    private var characteristic:BluetoothGattCharacteristic? = null
    private var advertiser: BluetoothLeAdvertiser? = null

    fun init(context: Context) {

        sbxContext = context

    }

    fun send() {

        Log.d("print" , "onConnectionStateChange1")


        var count = 0
        Timer().schedule(1000,100){

            val testList = listOf<Int>(
                    count,
                    Random().nextInt(100),
                    Random().nextInt(3),
                    Random().nextInt(255),
                    Random().nextInt(255),
                    Random().nextInt(255),
                    Random().nextInt(255),
                    Random().nextInt(255),
                    Random().nextInt(255),
                    Random().nextInt(255),
                    Random().nextInt(255),
                    Random().nextInt(255)

            )
            val test: List<Byte> = testList.map(Int::toByte)
            val sendDate = test.toByteArray()
            characteristic!!.setValue(sendDate)
            val notifyOk = gattServer!!.notifyCharacteristicChanged(connectedDevice, characteristic, false)
            if (!notifyOk) {

                // 再接続処理
                Log.d("print","再接続")

            }

            count = count + 1


        }



    }

    private fun setUuid() {

        //serviceUUIDを設定
        val service = BluetoothGattService(
                UUID.fromString(SERVICE_UUID),
                BluetoothGattService.SERVICE_TYPE_PRIMARY)

        //characteristicUUIDを設定
        characteristic = BluetoothGattCharacteristic(
                UUID.fromString(CHAR_UUIDS["CAHR1"]),
                BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ)

        //characteristicUUIDをserviceUUIDにのせる
        service.addCharacteristic(characteristic)

        //serviceUUIDをサーバーにのせる
        gattServer!!.addService(service)
    }

    fun stop(){

        gattServer!!.close()
        //advertiser!!.stopAdvertising(null)


    }

    fun start() {

        Log.d("print","advertise start")

        val manager = sbxContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = manager.adapter
        adapter.setName("YOUR_LOCAL_NAME")
        advertiser = adapter.bluetoothLeAdvertiser
        gattServer = manager.openGattServer(sbxContext,mGattServerCallback)

        // 設定
        val settingBuilder = AdvertiseSettings.Builder()
        settingBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
        settingBuilder.setConnectable(true)
        settingBuilder.setTimeout(100000)
        settingBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_ULTRA_LOW)
        val settings = settingBuilder.build()

        // アドバタイジングデータ
        val dataBuilder = AdvertiseData.Builder()
        dataBuilder.addServiceUuid(ParcelUuid(UUID.fromString(SERVICE_UUID)))
        val advertiseData = dataBuilder.build()

        setUuid()

        //アドバタイズを開始
        advertiser!!.startAdvertising(settings, advertiseData, object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                super.onStartSuccess(settingsInEffect)
                Log.d("print","advertise ok")
            }

            override fun onStartFailure(errorCode: Int) {
                super.onStartFailure(errorCode)
                Log.d("print","advertise ng")
            }
        })


    }

    private val mGattServerCallback = object : BluetoothGattServerCallback() {

        override fun onCharacteristicReadRequest(device:BluetoothDevice, requestId:Int, offset:Int, characteristic:BluetoothGattCharacteristic ) {

            Log.d("print" , "onCharacteristicReadRequest")

            //セントラルに任意の文字を返信する
            if (UUID.fromString(CHAR_UUIDS["CHAR1"]).equals(characteristic.uuid)) {
                val response = "your message."
                val value = response.toByteArray()
                gattServer!!.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)

//                connectedDevice = device
//                connectedRequestId = requestId
//                connectedOffset = offset
            }

        }

        override fun onConnectionStateChange (device:BluetoothDevice, status: Int, newState: Int) {

            Log.d("print" , "onConnectionStateChange")

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectedDevice = device


            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                connectedDevice = null
            }
        }

        override fun onNotificationSent(device: BluetoothDevice, status: Int) {

            Log.d("print" , "onNotificationSent")

        }

    }


}