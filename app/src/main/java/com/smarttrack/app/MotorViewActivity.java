package com.smarttrack.app;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MotorViewActivity extends AppCompatActivity implements ServiceConnection {
    private static final String TAG = "TAG";

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private String mDeviceAddress;
    private String mDeviceName;
    public BluetoothLeService mBluetoothLeService;
    private boolean mConnected = false;
    private BluetoothGatt mBluetoothGatt;

    private SeekBar seekBar1, seekBar2;
    private Button resetBtn;
    private TextView deviceID;

    List<BluetoothGattCharacteristic> chars = new ArrayList<>();
    List<BluetoothGattService> services = new ArrayList<>();
    List<Integer> data = new ArrayList<>();
    int dir1,dir2,speed1,speed2;

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder service) {
        mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
        if (!mBluetoothLeService.initialize()) {
            Log.e(TAG, "Unable to initialize Bluetooth");
            finish();
        }
        // Automatically connects to the device upon successful start-up initialization.
        mBluetoothLeService.connect(mDeviceAddress);
        Log.e("Reconnecting"," = bluetooth reconnect");
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        mBluetoothLeService.disconnect();
        //mBluetoothLeService.close();
    }


    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    // or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                //updateConnectionState(R.string.connected);
                //invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                //updateConnectionState(R.string.disconnected);
                //invalidateOptionsMenu();
                //clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                //getIndividualServices(mBluetoothLeService.getSupportedGattServices());
                Log.d(TAG, "onReceive: services # " + mBluetoothLeService.getSupportedGattServices());

                Log.d(TAG, "onCreate: STARTED THE READING PROCESS HERE");

                // NULL...
                if (mBluetoothLeService == null) {
                    final boolean result = mBluetoothLeService.connect(mDeviceAddress);
                    Log.d(TAG, "Connect request result=" + result);
                }
                Log.d(TAG, "onResume: Get SERVICES and CHARACTERISTICS");

                services = mBluetoothLeService.getSupportedGattServices();

                BluetoothGattService rightService = null;
                for (int i = 0; i < services.size(); i++) {
                    if (services.get(i).getUuid().toString().equals(BLE_UUID.SERVICE_INFORMATION)) {
                        rightService = services.get(i);
                        Log.e("service found : ", rightService.getUuid().toString());
                    }
                }

                //Adding the characteristics
                chars.add(rightService.getCharacteristic(UUID.fromString(BLE_UUID.CHAR_MOTOR1_SPEED)));
                chars.add(rightService.getCharacteristic(UUID.fromString(BLE_UUID.CHAR_MOTOR2_SPEED)));
                chars.add(rightService.getCharacteristic(UUID.fromString(BLE_UUID.CHAR_MOTOR1_DIR)));
                chars.add(rightService.getCharacteristic(UUID.fromString(BLE_UUID.CHAR_MOTOR2_DIR)));


                for (int i = 0; i < chars.size(); i++) {
                    Log.e("characters found : ", chars.get(i).getUuid().toString());
                }

                Log.d(TAG, "onResume: Start REQUEST characteristics");
                mBluetoothLeService.setCharacteristicList(chars);
                mBluetoothLeService.requestCharacteristics();

            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                Log.d(TAG, "onReceive: Received data from device");
                if (intent.getStringExtra(BluetoothLeService.DIR1)!=null){
                    dir1 = Integer.parseInt(intent.getStringExtra(BluetoothLeService.DIR1));
                    data.add(dir1);
                    Log.d(TAG, "onReceive: " + dir1);
                } else if (intent.getStringExtra(BluetoothLeService.DIR2)!=null){
                    dir2 = Integer.parseInt(intent.getStringExtra(BluetoothLeService.DIR2));
                    data.add(dir2);
                    Log.d(TAG, "onReceive: " + dir2);
                } else if (intent.getStringExtra(BluetoothLeService.SPEED1)!=null){
                    speed1 = Integer.parseInt(intent.getStringExtra(BluetoothLeService.SPEED1));
                    Log.d(TAG, "onReceive: " + data.get(0));
                    Log.d(TAG, "onReceive: " + speed1);
                    if(dir1==1){
                        int value = 100-speed1;
                        seekBar1.setProgress(value);
                    }
                    else{
                        seekBar1.setProgress(speed1+100);
                    }
                } else if (intent.getStringExtra(BluetoothLeService.SPEED2)!=null){
                    speed2 = Integer.parseInt(intent.getStringExtra(BluetoothLeService.SPEED2));
                    Log.d(TAG, "onReceive: " + data.get(1));
                    Log.d(TAG, "onReceive: " + speed2);
                    if(dir2==1){
                        int value = 100-speed2;
                        seekBar2.setProgress(value);
                    }else{
                        seekBar2.setProgress(speed2+100);
                    }
                }
            }
        }
    };
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_motor_view);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, this, BIND_AUTO_CREATE);

        //UI
        deviceID = findViewById(R.id.device_id);
        deviceID.setText(mDeviceName);

        seekBar1 = findViewById(R.id.seekBar_1);
        seekBar2 = findViewById(R.id.seekBar_2);
        resetBtn = findViewById(R.id.reset_button);


        resetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mBluetoothLeService.getWriteDetails(BLE_UUID.SERVICE_INFORMATION,BLE_UUID.CHAR_MOTOR1_SPEED,0);
//                try {
//                    Thread.sleep(200);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
                mBluetoothLeService.getWriteDetails(BLE_UUID.SERVICE_INFORMATION,BLE_UUID.CHAR_MOTOR2_SPEED,0);
                seekBar1.setProgress(10);
                seekBar2.setProgress(10);
            }
        });
        seekBar1.setMax(20);
        seekBar1.setProgress(10);
        seekBar2.setMax(20);
        seekBar2.setProgress(10);
        seekBar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                boolean state;
                int speed=0, direction=0;
                int value = (seekBar1.getProgress()-10)*10;

                if(value<0){
                    speed = value*(-1);
                    direction = 1;
                }
                else if(value>=0){
                    direction =0;
                    speed = value;
                }

                mBluetoothLeService.getWriteDetails(BLE_UUID.SERVICE_INFORMATION,BLE_UUID.CHAR_MOTOR1_DIR,direction);
                //Log.d("direction", String.valueOf(state));
//                try {
//                    Thread.sleep(100);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
                mBluetoothLeService.getWriteDetails(BLE_UUID.SERVICE_INFORMATION,BLE_UUID.CHAR_MOTOR1_SPEED,speed);
                //Log.d("speed", String.valueOf(state));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        seekBar2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                boolean state;
                int speed=0, direction=0;
                int value = (seekBar2.getProgress()-10)*10;

                if(value<0){
                    speed = value*(-1);
                    direction = 1;
                }
                else if(value>=0){
                    direction =0;
                    speed = value;
                }

                mBluetoothLeService.getWriteDetails(BLE_UUID.SERVICE_INFORMATION,BLE_UUID.CHAR_MOTOR2_DIR,direction);
//                Log.d("direction", String.valueOf(state));
//                try {
//                    Thread.sleep(100);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
                mBluetoothLeService.getWriteDetails(BLE_UUID.SERVICE_INFORMATION,BLE_UUID.CHAR_MOTOR2_SPEED,speed);
                //Log.d("speed", String.valueOf(state));

            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


    }
    @Override
    protected void onResume() {

        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, this, BIND_AUTO_CREATE);
        Log.d("motor view activity","resumed");
    }
    public void onBackPressed() {
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);

        seekBar1.setProgress(10);
        seekBar2.setProgress(10);

        mBluetoothLeService.getWriteDetails(BLE_UUID.SERVICE_INFORMATION,BLE_UUID.CHAR_MOTOR1_SPEED,0);
        mBluetoothLeService.getWriteDetails(BLE_UUID.SERVICE_INFORMATION,BLE_UUID.CHAR_MOTOR2_SPEED,0);

        Log.d("motor view activity","paused");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(this);
        Log.d("motor view activity","destroyed");
    }


}
