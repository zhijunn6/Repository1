package com.smarttrack.app;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

public class BluetoothLeService extends Service {
    private final static String TAG = "BluetoothLeService";

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    public boolean ready = false;
    public final static String ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";
    public final static String SPEED1 = "com.example.bluetooth.le.SPEED1";
    public final static String SPEED2 = "com.example.bluetooth.le.SPEED2";
    public final static String DIR1 = "com.example.bluetooth.le.DIR1";
    public final static String DIR2 = "com.example.bluetooth.le.DIR2";
    public final static String COLOR = "com.example.bluetooth.le.COLOR";
    public final static String INTENSITY = "com.example.bluetooth.le.INTENSITY";
    public final static String LASER = "com.example.bluetooth.le.LASER";
    public final static String LOAD = "com.example.bluetooth.le.LOAD";
    public final static String SAVE = "com.example.bluetooth.le.SAVE";
    final BluetoothDevice device = null;

    private Queue<String> writeQueueService = new LinkedList();
    private Queue<String> writeQueueCharac = new LinkedList();
    private Queue<Integer> writeQueueData = new LinkedList();
    private boolean isWriting = false;


    public int laser, load_memory, save_memory, light_intensity,light_color, dir2,dir1, speed1,speed2;
    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    List<BluetoothGattCharacteristic> chars = new ArrayList<>();
    public final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" + mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            // If discoverServices return True
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "onServicesDiscovered: IN!");
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }


        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
        {
            Log.d(TAG, "Reading Characteristic test");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Reading Characteristic: Start READ with broadcastUpdate");
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                chars.remove(chars.get(chars.size() - 1));
                if (chars.size() > 0) {
                    Log.d(TAG, "onCharacteristicRead: Recursive function REQUEST");
                    requestCharacteristics();
                }
            }else
                Log.e(TAG, "onCharacteristicRead: Failed READ");
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            isWriting = false;
            writeNextValueFromQueue();
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    // THIS IS FOR SERVICE
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
        Log.d(TAG, "broadcastUpdate: Passing intent of service, success");
    }

    // THIS IS FOR CHARACTERISTICS
    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        final String str = characteristic.getUuid().toString();
        Log.d(TAG, "broadcastUpdate: Entered broadcastUpdate");

        if (str.equals(BLE_UUID.LASER)) {
            String laser = String.valueOf(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0));
            Log.e("LASER STATUS ", String.valueOf(laser));
            intent.putExtra(LASER, laser);
        }
        else if (str.equals(BLE_UUID.CHAR_LIGHT_INTENSITY)) {
            String intensity = String.valueOf(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0));
            Log.e("INTENSITY STATUS ", String.valueOf(intensity));
            intent.putExtra(INTENSITY, intensity);
        }
        else if (str.equals(BLE_UUID.CHAR_LIGHT_COLOR)) {
            String color = String.valueOf(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, 0));
            Log.e("COLOR STATUS ", String.valueOf(color));
            intent.putExtra(COLOR, color);
        }
        else if (str.equals(BLE_UUID.CHAR_MOTOR2_SPEED)) {
            String speed2 = String.valueOf(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0));
            Log.e("MOTOR2 SPEED STATUS ", String.valueOf(speed2));
            intent.putExtra(SPEED2, speed2);
        }
        else if (str.equals(BLE_UUID.CHAR_MOTOR2_DIR)) {
            String dir2 = String.valueOf(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0));
            Log.e("MOTOR2 DIR STATUS ", String.valueOf(dir2));
            intent.putExtra(DIR2, dir2);
        }
        else if (str.equals(BLE_UUID.CHAR_MOTOR1_SPEED)) {
            String speed1 = String.valueOf(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0));
            Log.e("MOTOR1 SPEED STATUS ", String.valueOf(speed1));
            intent.putExtra(SPEED1, speed1);
        }
        else if (str.equals(BLE_UUID.CHAR_MOTOR1_DIR)) {
            String dir1 = String.valueOf(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0));
            Log.e("LOAD MEMORY STATUS ", String.valueOf(dir1));
            intent.putExtra(DIR1, dir1);
        }
        else if (str.equals(BLE_UUID.LOAD_MEMORY)) {
            String load = String.valueOf(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0));
            Log.e("LOAD MEMORY STATUS ", String.valueOf(load));
            intent.putExtra(LOAD, load);
        }
        else if (str.equals(BLE_UUID.SAVE_MEMORY)) {
            String save = String.valueOf(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0));
            Log.e("SAVE MEMORY STATUS ", String.valueOf(save));
            intent.putExtra(SAVE, save);
        }

        sendBroadcast(intent);
        Log.d(TAG, "broadcastUpdate: Intent sent");
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: Service started");

        return super.onStartCommand(intent, flags, startId);
    }

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            Log.d(TAG, "getService: Called");
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            disconnect();
        }

        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        } else{
            Log.e(TAG, "BluetoothAdapter obtained.");
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w("Connect()", "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                Log.d(TAG, "connect: TRUE");
                return true;
            } else {
                Log.d(TAG, "connect: FALSE");
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        if(mBluetoothGatt==null){
            mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
            Log.d(TAG, "Trying to create a new connection.");
        }

        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        Log.d(TAG, "connect: Passed here! mGattCallback: " + mGattCallback);
        return true;
    }



    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w("disconnect() ", "BluetoothAdapter not initialized");
            return;
        }
        Log.d("DISCONNECTED", "from bluetooth");
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }

        mBluetoothGatt.close();
        mBluetoothGatt = null;

    }

    public void requestCharacteristics() {
        Log.d(TAG, "requestCharacteristics: Start READ characteristics");
        mBluetoothGatt.readCharacteristic(chars.get(chars.size()-1));
    }


    public void setCharacteristicList(List<BluetoothGattCharacteristic> chars){
        Log.d(TAG, "setCharacteristicList: CHARACTERISTICS successfully passed over");
         this.chars = chars;
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w("SET CHARACTERISTIC", "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        for (BluetoothGattDescriptor descriptor:characteristic.getDescriptors()){
            Log.d(TAG, "BluetoothGattDescriptor: "+descriptor.getUuid().toString());
        }
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */

    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;
        Log.d(TAG, "getSupportedGattServices: Detected services received");
        //Get the service you want
        return mBluetoothGatt.getServices();
    }

    //-------------------- Write in queue --------------------
    public void getWriteDetails(String services, String characteristics, int value){
        //Queue all write details from activity
//        Log.d(TAG, "getWriteDetails: Details received");
//        Log.d(TAG, "getWriteDetails: Service: " + services);
//        Log.d(TAG, "getWriteDetails: Characteristic: " + characteristics);
//        Log.d(TAG, "getWriteDetails: Data: " + value);
        writeQueueService.add(services);
        writeQueueCharac.add(characteristics);
        writeQueueData.add(value);

        writeNextValueFromQueue();
    }

    private void writeNextValueFromQueue() {

        if(isWriting){
            return;
        }

        if(writeQueueCharac.size() == 0 || writeQueueService.size() == 0 || writeQueueData.size() == 0){
            return;
        }
        isWriting = true;

        writeCharacteristic_1byte(writeQueueService.poll(), writeQueueCharac.poll(), writeQueueData.poll());
    }

    public boolean writeCharacteristic_1byte(String services, String characteristics, int temp){

        Log.d(TAG, "writeCharacteristic_1byte: Details received");
        Log.d(TAG, "writeCharacteristic_1byte: Service: " + services);
        Log.d(TAG, "writeCharacteristic_1byte: Characteristic: " + characteristics);
        Log.d(TAG, "writeCharacteristic_1byte: Data: " + temp);

        //check mBluetoothGatt is available
        if (mBluetoothGatt == null) {
            Log.d(TAG, "lost connection");
            return false;
        }
        BluetoothGattService Service = mBluetoothGatt.getService(UUID.fromString(services));
        if (Service == null) {
            Log.d(TAG, "service not found!");
            return false;
        }
        BluetoothGattCharacteristic charac = Service.getCharacteristic(UUID.fromString(characteristics));
        if (charac == null) {
            Log.d(TAG, "char not found!");
            return false;
        }

        Log.d(TAG, "writeCharacteristic_1byte: 1byte value" + temp);
        byte[] value = new byte[1];
        value[0] = (byte) temp;
        Log.d("OUTPUT", String.valueOf((byte) temp));
        charac.setValue(value);

        boolean status = mBluetoothGatt.writeCharacteristic(charac);
        return status;
    }

    public boolean writeCharacteristic_2byte(String services, String characteristics, int temp){
        Log.d(TAG, "writeCharacteristic_2byte: Temp: " + temp);

        //check mBluetoothGatt is available
        if (mBluetoothGatt == null) {
            Log.e(TAG, "lost connection");
            return false;
        }
        BluetoothGattService Service = mBluetoothGatt.getService(UUID.fromString(services));
        if (Service == null) {
            Log.e(TAG, "service not found!");
            return false;
        }
        BluetoothGattCharacteristic charac = Service.getCharacteristic(UUID.fromString(characteristics));
        if (charac == null) {
            Log.e(TAG, "char not found!");
            return false;
        }

        byte[] value = new byte[2];
        value[1] = (byte) ((temp >> 8) & 0xFF);
        value[0] = (byte) temp;

        Log.d("OUTPUT", String.valueOf((byte) temp));
        charac.setValue(value);
        boolean status = mBluetoothGatt.writeCharacteristic(charac);
        return status;
    }
}


//                if (characteristic.getUuid().toString().equals(BLE_UUID.LASER)) {
//                        int laser = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
//                        Log.e("LASER STATUS ", String.valueOf(laser));
//                        } else if (characteristic.getUuid().toString().equals(BLE_UUID.CHAR_LIGHT_INTENSITY)) {
//                        int intensity = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);
//                        Log.e("INTENSITY STATUS ", String.valueOf(intensity));
//                        } else if (characteristic.getUuid().toString().equals(BLE_UUID.CHAR_LIGHT_COLOR)) {
//                        int color = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
//                        Log.e("COLOR STATUS ", String.valueOf(color));
//                        } else if (characteristic.getUuid().toString().equals(BLE_UUID.CHAR_MOTOR2_SPEED)) {
//                        int speed2 = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
//                        Log.e("MOTOR2 SPEED STATUS ", String.valueOf(speed2));
//                        } else if (characteristic.getUuid().toString().equals(BLE_UUID.CHAR_MOTOR2_DIR)) {
//                        int dir2 = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
//                        Log.e("MOTOR2 DIR STATUS ", String.valueOf(dir2));
//                        } else if (characteristic.getUuid().toString().equals(BLE_UUID.CHAR_MOTOR1_SPEED)) {
//                        int speed1 = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
//                        Log.e("MOTOR1 SPEED STATUS ", String.valueOf(speed1));
//                        } else if (characteristic.getUuid().toString().equals(BLE_UUID.CHAR_MOTOR1_DIR)) {
//                        int dir1 = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
//                        Log.e("LOAD MEMORY STATUS ", String.valueOf(dir1));
//                        } else if (characteristic.getUuid().toString().equals(BLE_UUID.LOAD_MEMORY)) {
//                        int load = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
//                        Log.e("LOAD MEMORY STATUS ", String.valueOf(load));
//                        } else if (characteristic.getUuid().toString().equals(BLE_UUID.SAVE_MEMORY)) {
//                        int save = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
//                        Log.e("SAVE MEMORY STATUS ", String.valueOf(save));
//                        }

//    //Getting Services
//    List<BluetoothGattService> services = gatt.getServices();
//    BluetoothGattService rightService = null;
//                for (int i = 0; i < services.size(); i++) {
//        if (services.get(i).getUuid().toString().equals(BLE_UUID.SERVICE_INFORMATION)) {
//        rightService = services.get(i);
//        Log.e("service found : ", rightService.getUuid().toString());
//        }
//        }
//        //Adding the characteristics
//        chars.add(rightService.getCharacteristic(UUID.fromString(BLE_UUID.LASER)));
//        chars.add(rightService.getCharacteristic(UUID.fromString(BLE_UUID.LOAD_MEMORY)));
//        chars.add(rightService.getCharacteristic(UUID.fromString(BLE_UUID.SAVE_MEMORY)));
//        chars.add(rightService.getCharacteristic(UUID.fromString(BLE_UUID.CHAR_LIGHT_INTENSITY)));
//        chars.add(rightService.getCharacteristic(UUID.fromString(BLE_UUID.CHAR_LIGHT_COLOR)));
//        chars.add(rightService.getCharacteristic(UUID.fromString(BLE_UUID.CHAR_MOTOR1_SPEED)));
//        chars.add(rightService.getCharacteristic(UUID.fromString(BLE_UUID.CHAR_MOTOR2_SPEED)));
//        chars.add(rightService.getCharacteristic(UUID.fromString(BLE_UUID.CHAR_MOTOR2_DIR)));
//        chars.add(rightService.getCharacteristic(UUID.fromString(BLE_UUID.CHAR_MOTOR1_DIR)));
//
//        for (int i = 0; i < chars.size(); i++) {
//        Log.e("characters found : ", chars.get(i).getUuid().toString());
//        }
//
//        requestCharacteristics(gatt);
//

//  if(DeviceMainActivity.state == 0){
//          if ( characteristic.getUuid().toString().equals(BLE_UUID.LASER)) {
//          laser = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
//          Log.e("LASER = ", String.valueOf(laser));
//          }else if ( characteristic.getUuid().toString().equals(BLE_UUID.LOAD_MEMORY)) {
//          load_memory = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
//          Log.e("LOAD MEMORY = ", String.valueOf(load_memory));
//          }else if(( characteristic.getUuid().toString().equals(BLE_UUID.SAVE_MEMORY))){
//          save_memory = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
//          Log.e("SAVE MEMORY", String.valueOf(save_memory));
//          }
//          }
//          else if (DeviceMainActivity.state==1){
//          if ( characteristic.getUuid().toString().equals(BLE_UUID.CHAR_LIGHT_COLOR)) {
//          light_color = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, 0);
//          Log.e("COLOR LIGHT = ", String.valueOf(light_color));
//          }else if ( characteristic.getUuid().toString().equals(BLE_UUID.CHAR_LIGHT_INTENSITY)) {
//          light_intensity = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
//          Log.e("INTENSITY LIGHT = ", String.valueOf(light_intensity));
//          ready = true;
//          }
//          }
//          else if (DeviceMainActivity.state==2){
//          if ( characteristic.getUuid().toString().equals(BLE_UUID.CHAR_MOTOR1_DIR)) {
//          dir1 = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
//          Log.e("DIRECTION 1 = ", String.valueOf(dir1));
//          ready = true;
//          }else if ( characteristic.getUuid().toString().equals(BLE_UUID.CHAR_MOTOR1_SPEED)) {
//          speed1 = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
//          Log.e("SPEED 1 = ", String.valueOf(speed1));
//          }else if(( characteristic.getUuid().toString().equals(BLE_UUID.CHAR_MOTOR2_DIR))){
//          dir2 = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
//          Log.e("DIRECTION 2 = ", String.valueOf(dir2));
//          }else if(( characteristic.getUuid().toString().equals(BLE_UUID.CHAR_MOTOR2_SPEED))){
//          speed2 = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
//          Log.e("SPEED 2 = ", String.valueOf(speed2));
//          }
//          }


//        final byte[] data = characteristic.getValue();
//        if (data != null && data.length > 0) {
//            final StringBuilder stringBuilder = new StringBuilder(data.length);
//            for(byte byteChar : data)
//                stringBuilder.append(String.format("%02X ", byteChar));
//            intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
//        }