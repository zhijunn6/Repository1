package com.smarttrack.app;

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
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LightViewActivity extends AppCompatActivity implements ServiceConnection {
    private static final String TAG = "TAG";
    private SeekBar lightIntensity, lightColorTempSlider;
    public BluetoothLeService mBluetoothLeService;
    private String mDeviceAddress, mDeviceName, mColor, mIntensity;
    private TextView lightSeekbarValue;
    private Button zero_Light, fifty_Light, hundred_Light;
    private TextView deviceID;
    private ImageView lightBulb;
    private boolean mConnected = false;
    Intent gattServiceIntent;
    int colorValue,intensity;

    List<BluetoothGattCharacteristic> chars = new ArrayList<>();
    List<BluetoothGattService> services = new ArrayList<>();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String EXTRAS_DEVICE_COLOR = "DEVICE_COLOR";
    public static final String EXTRAS_DEVICE_INTENSITY = "DEVICE_INTENSITY";

    // Code to manage Service lifecycle.
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
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                Log.d(TAG, "onReceive: services # " + mBluetoothLeService.getSupportedGattServices());

                Log.d(TAG, "onCreate: STARTED THE READING PROCESS HERE");

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
                chars.add(rightService.getCharacteristic(UUID.fromString(BLE_UUID.CHAR_LIGHT_INTENSITY)));
                chars.add(rightService.getCharacteristic(UUID.fromString(BLE_UUID.CHAR_LIGHT_COLOR)));

                for (int i = 0; i < chars.size(); i++) {
                    Log.e("characters found : ", chars.get(i).getUuid().toString());
                }

                Log.d(TAG, "onResume: Start REQUEST characteristics");
                mBluetoothLeService.setCharacteristicList(chars);
                mBluetoothLeService.requestCharacteristics();

            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                Log.d(TAG, "onReceive: Received data from device, called");
                if (intent.getStringExtra(BluetoothLeService.COLOR)!=null){
                    Log.d(TAG, "onReceive: " + intent.getStringExtra(BluetoothLeService.COLOR));
                    colorValue = Integer.valueOf(intent.getStringExtra(BluetoothLeService.COLOR));
                    lightColorTempSlider.setProgress(colorValue-2700);
                }

                else if (intent.getStringExtra(BluetoothLeService.INTENSITY)!=null){
                    Log.d(TAG, "onReceive: " + intent.getStringExtra(BluetoothLeService.INTENSITY));
                    intensity = Integer.parseInt(intent.getStringExtra(BluetoothLeService.INTENSITY));
                    lightIntensity.setProgress(intensity);
                    lightSeekbarValue.setText(String.valueOf(intensity));
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
    public void onServiceConnected(ComponentName name, IBinder service) {
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
    public void onServiceDisconnected(ComponentName name) {
        mBluetoothLeService.disconnect();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_light_view);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        mColor = intent.getStringExtra(EXTRAS_DEVICE_COLOR);
        mIntensity = intent.getStringExtra(EXTRAS_DEVICE_INTENSITY);


        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, this, BIND_AUTO_CREATE);

        deviceID = findViewById(R.id.device_id);
        deviceID.setText(mDeviceName);
        lightIntensity = (SeekBar) findViewById(R.id.verticalSlider);
        lightColorTempSlider = (SeekBar) findViewById(R.id.lightColorTempSlider);
        lightSeekbarValue = (TextView) findViewById(R.id.light_seekBar_value);
        zero_Light = (Button) findViewById(R.id.zeroLightBtn);
        fifty_Light = (Button) findViewById(R.id.fiftyLightBtn);
        hundred_Light = (Button) findViewById(R.id.hundredLightBtn);
        lightBulb = (ImageView) findViewById(R.id.lightBulb);

        lightIntensity.setProgress(Integer.parseInt(mIntensity));
        lightSeekbarValue.setText(mIntensity);
        lightColorTempSlider.setProgress(Integer.parseInt(mColor)-2700);


        lightIntensity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                lightSeekbarValue.setText(String.valueOf(seekBar.getProgress()));

                mBluetoothLeService.writeCharacteristic_1byte(BLE_UUID.SERVICE_INFORMATION, BLE_UUID.CHAR_LIGHT_INTENSITY, lightIntensity.getProgress());

                if(seekBar.getProgress() == 0){
                    lightBulb.setImageResource(R.drawable.light_bulb_off);
                    zero_Light.setBackgroundResource(R.drawable.button_border_clicked);
                }
                else{
                    lightBulb.setImageResource(R.drawable.light_bulb_on);
                    zero_Light.setBackgroundResource(R.drawable.button_border);
                }
                if(seekBar.getProgress() == 50){
                    fifty_Light.setBackgroundResource(R.drawable.button_border_clicked);
                }
                else{
                    fifty_Light.setBackgroundResource(R.drawable.button_border);
                }
                if(seekBar.getProgress() == 100){
                    hundred_Light.setBackgroundResource(R.drawable.button_border_clicked);
                }
                else{
                    hundred_Light.setBackgroundResource(R.drawable.button_border);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        zero_Light.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lightIntensity.setProgress(0);
                mBluetoothLeService.writeCharacteristic_1byte(BLE_UUID.SERVICE_INFORMATION, BLE_UUID.CHAR_LIGHT_INTENSITY, lightIntensity.getProgress());
                lightSeekbarValue.setText(String.valueOf(lightIntensity.getProgress()));
                lightBulb.setImageResource(R.drawable.light_bulb_off);
            }
        });

        fifty_Light.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lightIntensity.setProgress(50);
                mBluetoothLeService.writeCharacteristic_1byte(BLE_UUID.SERVICE_INFORMATION, BLE_UUID.CHAR_LIGHT_INTENSITY, lightIntensity.getProgress());
                lightSeekbarValue.setText(String.valueOf(lightIntensity.getProgress()));
                lightBulb.setImageResource(R.drawable.light_bulb_on);
            }
        });

        hundred_Light.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lightIntensity.setProgress(100);
                mBluetoothLeService.writeCharacteristic_1byte(BLE_UUID.SERVICE_INFORMATION, BLE_UUID.CHAR_LIGHT_INTENSITY, lightIntensity.getProgress());
                lightSeekbarValue.setText(String.valueOf(lightIntensity.getProgress()));
                lightBulb.setImageResource(R.drawable.light_bulb_on);
            }
        });

        lightColorTempSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = progress +2700;
                mBluetoothLeService.writeCharacteristic_2byte(BLE_UUID.SERVICE_INFORMATION,BLE_UUID.CHAR_LIGHT_COLOR,value);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, this, BIND_AUTO_CREATE);
        Log.d("light view activity","resumed");
    }

    @Override
    protected void onPause() {
        super.onPause();
        services = mBluetoothLeService.getSupportedGattServices();
        BluetoothGattService rightService = null;
        for (int i = 0; i < services.size(); i++) {
            if (services.get(i).getUuid().toString().equals(BLE_UUID.SERVICE_INFORMATION)) {
                rightService = services.get(i);
                Log.e("service found : ", rightService.getUuid().toString());
            }
        }

        if(rightService!=null) {
            chars.add(rightService.getCharacteristic(UUID.fromString(BLE_UUID.CHAR_LIGHT_INTENSITY)));
            chars.add(rightService.getCharacteristic(UUID.fromString(BLE_UUID.CHAR_LIGHT_COLOR)));
        }
        for (int i = 0; i < chars.size(); i++) {
            Log.e("characters found : ", chars.get(i).getUuid().toString());
        }

        Log.d(TAG, "onResume: Start REQUEST characteristics");
        mBluetoothLeService.setCharacteristicList(chars);
        mBluetoothLeService.requestCharacteristics();


        unregisterReceiver(mGattUpdateReceiver);
        Log.d("light view activity","paused");
    }
    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(this);

        Log.d("light view activity","destroyed");
    }
}
