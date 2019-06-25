package com.smarttrack.app;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class DeviceMainActivity extends AppCompatActivity implements ServiceConnection {
    private static final String TAG = "DeviceMainActivity";
    Button toLightViewActivity, toMotorViewActivity;
    Intent gattServiceIntent;
    private BluetoothLeService mBluetoothLeService;
    private String mDeviceAddress;
    private String mDeviceName;
    private Switch laserSwitch;
    private int saveValue = 0;
    BluetoothGattService rightService;
    private boolean mConnected = false, clicked = true;
    public static int state = 0;
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String EXTRAS_DEVICE_COLOR = "DEVICE_COLOR";
    public static final String EXTRAS_DEVICE_INTENSITY = "DEVICE_INTENSITY";
    List<BluetoothGattCharacteristic> chars = new ArrayList<>();
    List<BluetoothGattService> services = new ArrayList<>();

    private TextView deviceID;
    private AlertDialog dialog;
    private EditText editDeviceID;
    private int color, intensity;

    ProgressBar progressBar;
    Button buttonProgress;
    Handler handler;
    Runnable runnable;
    Timer timer;

    private DatabaseHelper myDb;

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device. This can be a
    // result of read or notification operations.
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
                Log.d("STATE: ", String.valueOf(mBluetoothLeService.getSupportedGattServices()));

                Log.d(TAG, "onCreate: STARTED THE READING PROCESS HERE");

                if (mBluetoothLeService == null) {
                    final boolean result = mBluetoothLeService.connect(mDeviceAddress);
                    Log.d(TAG, "Connect request result=" + result);
                }
                Log.d(TAG, "onResume: Get SERVICES and CHARACTERISTICS");

                services = mBluetoothLeService.getSupportedGattServices();

                rightService = null;
                for (int i = 0; i < services.size(); i++) {
                    if (services.get(i).getUuid().toString().equals(BLE_UUID.SERVICE_INFORMATION)) {
                        rightService = services.get(i);
                        Log.e("service found : ", rightService.getUuid().toString());
                    }
                }

                //Adding the characteristics
                chars.add(rightService.getCharacteristic(UUID.fromString(BLE_UUID.CHAR_LIGHT_INTENSITY)));
                chars.add(rightService.getCharacteristic(UUID.fromString(BLE_UUID.CHAR_LIGHT_COLOR)));
                chars.add(rightService.getCharacteristic(UUID.fromString(BLE_UUID.LASER)));
                for (int i = 0; i < chars.size(); i++) {
                    Log.e("characters found : ", chars.get(i).getUuid().toString());
                }

                Log.d(TAG, "onResume: Start REQUEST characteristics");
                mBluetoothLeService.setCharacteristicList(chars);
                mBluetoothLeService.requestCharacteristics();
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                Log.d(TAG, "onReceive: Data received, called");
                if (intent.getStringExtra(BluetoothLeService.LASER)!=null){
                    int laser = Integer.parseInt(intent.getStringExtra(BluetoothLeService.LASER));
                    Log.d(TAG, "onReceive: " + laser);
                    if(laser == 0)
                        laserSwitch.setChecked(false);
                    else
                        laserSwitch.setChecked(true);
                }
                else if (intent.getStringExtra(BluetoothLeService.COLOR)!=null){
                    color = Integer.parseInt(intent.getStringExtra(BluetoothLeService.COLOR));
                    Log.d(TAG, "onReceive: " + color);
                }
                else if (intent.getStringExtra(BluetoothLeService.INTENSITY)!=null){
                    intensity = Integer.parseInt(intent.getStringExtra(BluetoothLeService.INTENSITY));
                    Log.d(TAG, "onReceive: " + intensity);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        myDb = new DatabaseHelper(this);
        final Intent intent = getIntent();
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        //Get Device Secondary Name
        returnLatestDeviceName();
        deviceID = findViewById(R.id.device_id);
        deviceID.setText(mDeviceName);

        dialog = new AlertDialog.Builder(this).create();
        editDeviceID = new EditText(this);
        dialog.setTitle("Edit device name");
        dialog.setView(editDeviceID);

        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deviceID.setText(editDeviceID.getText());
                //Update Device Secondary Name
                myDb.updateDevice(mDeviceAddress, editDeviceID.getText().toString());
            }
        });

        deviceID.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editDeviceID.setText(deviceID.getText());
                dialog.show();
            }
        });

        gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, this, BIND_AUTO_CREATE);

        final Button saveButton, saveButton1, saveButton2, saveButton3,saveButton4,saveButton5,loadButton;
        laserSwitch = findViewById(R.id.laser_switch);

        laserSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean state;
                if (laserSwitch.isChecked()) {
                    //((serviceApplication)DeviceMainActivity.this.getApplicationContext()).mBluetoothLeService.writeCharacteristic_1byte(BLE_UUID.SERVICE_INFORMATION,BLE_UUID.LASER, 1);
                    state = mBluetoothLeService.writeCharacteristic_1byte(BLE_UUID.SERVICE_INFORMATION,BLE_UUID.LASER, 1);
                } else {
                    //((serviceApplication)DeviceMainActivity.this.getApplicationContext()).mBluetoothLeService.writeCharacteristic_1byte(BLE_UUID.SERVICE_INFORMATION,BLE_UUID.LASER, 0);
                    state = mBluetoothLeService.writeCharacteristic_1byte(BLE_UUID.SERVICE_INFORMATION,BLE_UUID.LASER, 0);
                }
                Log.d("Laser Write", String.valueOf(state));

            }
        });

        toLightViewActivity = findViewById(R.id.light_button);
        toLightViewActivity.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                returnLatestDeviceName();

                final Intent intent = new Intent(DeviceMainActivity.this, LightViewActivity.class);
                intent.putExtra(DeviceMainActivity.EXTRAS_DEVICE_NAME, mDeviceName);
                intent.putExtra(DeviceMainActivity.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
                intent.putExtra(DeviceMainActivity.EXTRAS_DEVICE_COLOR, String.valueOf(color));
                intent.putExtra(DeviceMainActivity.EXTRAS_DEVICE_INTENSITY, String.valueOf(intensity));

                startActivity(intent);
            }
        });

        toMotorViewActivity = findViewById(R.id.motor_button);
        toMotorViewActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                returnLatestDeviceName();

                final Intent intent = new Intent(DeviceMainActivity.this, MotorViewActivity.class);
                intent.putExtra(DeviceMainActivity.EXTRAS_DEVICE_NAME, mDeviceName);
                intent.putExtra(DeviceMainActivity.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
                startActivity(intent);
            }
        });

        saveButton1 = findViewById(R.id.save_button_1);
        saveButton2 = findViewById(R.id.save_button_2);
        saveButton3 = findViewById(R.id.save_button_3);
        saveButton4 = findViewById(R.id.save_button_4);
        saveButton5 = findViewById(R.id.save_button_5);

        saveButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveValue = 1;
                saveButton1.setBackgroundResource(R.drawable.button_border_clicked);
                saveButton2.setBackgroundResource(R.drawable.button_border);
                saveButton3.setBackgroundResource(R.drawable.button_border);
                saveButton4.setBackgroundResource(R.drawable.button_border);
                saveButton5.setBackgroundResource(R.drawable.button_border);
            }
        });

        saveButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveValue = 2;
                saveButton1.setBackgroundResource(R.drawable.button_border);
                saveButton2.setBackgroundResource(R.drawable.button_border_clicked);
                saveButton3.setBackgroundResource(R.drawable.button_border);
                saveButton4.setBackgroundResource(R.drawable.button_border);
                saveButton5.setBackgroundResource(R.drawable.button_border);
            }
        });
        saveButton3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveValue = 3;
                saveButton1.setBackgroundResource(R.drawable.button_border);
                saveButton2.setBackgroundResource(R.drawable.button_border);
                saveButton3.setBackgroundResource(R.drawable.button_border_clicked);
                saveButton4.setBackgroundResource(R.drawable.button_border);
                saveButton5.setBackgroundResource(R.drawable.button_border);
            }
        });


        saveButton4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveValue = 4;
                saveButton1.setBackgroundResource(R.drawable.button_border);
                saveButton2.setBackgroundResource(R.drawable.button_border);
                saveButton3.setBackgroundResource(R.drawable.button_border);
                saveButton4.setBackgroundResource(R.drawable.button_border_clicked);
                saveButton5.setBackgroundResource(R.drawable.button_border);

            }
        });
        saveButton5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveValue = 5;
                saveButton1.setBackgroundResource(R.drawable.button_border);
                saveButton2.setBackgroundResource(R.drawable.button_border);
                saveButton3.setBackgroundResource(R.drawable.button_border);
                saveButton4.setBackgroundResource(R.drawable.button_border);
                saveButton5.setBackgroundResource(R.drawable.button_border_clicked);
            }
        });

        saveButton = findViewById(R.id.save_button);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean state = mBluetoothLeService.writeCharacteristic_1byte(BLE_UUID.SERVICE_INFORMATION,BLE_UUID.SAVE_MEMORY, saveValue);
                //((serviceApplication)DeviceMainActivity.this.getApplicationContext()).mBluetoothLeService.writeCharacteristic_1byte(BLE_UUID.SERVICE_INFORMATION,BLE_UUID.SAVE_MEMORY, saveValue);
                Log.d("WRITE STATE", String.valueOf(state)+ " with the following saveValue = " + saveValue);

            }
        });

        loadButton = findViewById(R.id.load_button);
        loadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean state = mBluetoothLeService.writeCharacteristic_1byte(BLE_UUID.SERVICE_INFORMATION, BLE_UUID.LOAD_MEMORY, saveValue);
                //((serviceApplication)DeviceMainActivity.this.getApplicationContext()).mBluetoothLeService.writeCharacteristic_1byte(BLE_UUID.SERVICE_INFORMATION,BLE_UUID.SAVE_MEMORY, saveValue);
                Log.d("WRITE STATE", String.valueOf(state) + " with the following loadValue = " + saveValue);

                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                chars.add(rightService.getCharacteristic(UUID.fromString(BLE_UUID.LASER)));
                chars.add(rightService.getCharacteristic(UUID.fromString(BLE_UUID.CHAR_LIGHT_COLOR)));
                chars.add(rightService.getCharacteristic(UUID.fromString(BLE_UUID.CHAR_LIGHT_INTENSITY)));

                mBluetoothLeService.setCharacteristicList(chars);


                mBluetoothLeService.requestCharacteristics();
            }
        });
    }

    public void perform_factory_reset(View view){
        TextView fReset = findViewById(R.id.factory_reset);
        saveValue = 254;
        fReset.setHighlightColor(getResources().getColor(R.color.text_type2));
        mBluetoothLeService.writeCharacteristic_1byte(BLE_UUID.SERVICE_INFORMATION,BLE_UUID.SAVE_MEMORY, saveValue);

        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.INVISIBLE);
                timer.cancel();
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                //screen.setVisibility(View.GONE);
            }
        };

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                handler.post(runnable);
            }
        }, 20000, 1000);


        laserSwitch.setChecked(false);
        color = 0;
        intensity = 0;
    }

    private void returnLatestDeviceName(){
        if (mDeviceAddress == null) {
            Log.d(TAG, "returnLatestDeviceName: Not connected to any device");
            return;
        }

        Cursor res = myDb.readDevice(mDeviceAddress);
        if(res.getCount() == 0){
            return;
        }

        while(res.moveToNext()) {
            mDeviceName = res.getString(res.getColumnIndex("EDITNAME"));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: called");
        returnLatestDeviceName();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, this, BIND_AUTO_CREATE);



    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
        Log.d("Device Main Activity", "onPause: Paused");
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(this);
        mBluetoothLeService.disconnect();
        mBluetoothLeService.close();

        Log.d("Device Main Activity", "onDestroy()");
}
    @Override
    public void onBackPressed() {
        finish();
    }
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
            Log.d(TAG, "Unable to initialize Bluetooth");
            finish();
        }
        // Automatically connects to the device upon successful start-up initialization.
        //((serviceApplication)DeviceMainActivity.this.getApplicationContext()).mBluetoothLeService.connect(mDeviceAddress);
        mBluetoothLeService.connect(mDeviceAddress);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mBluetoothLeService.disconnect();
    }

    public void callUnbindService(){unbindService(this);}


}

//Log.d(TAG, "onCreate: STARTED THE READING PROCESS HERE");
//
//        if (mBluetoothLeService == null) {
//        final boolean result = ((serviceApplication)this.getApplicationContext()).mBluetoothLeService.connect(mDeviceAddress);
//        Log.d(TAG, "Connect request result=" + result);
//        } else {
//        Log.d(TAG, "onResume: Get SERVICES and CHARACTERISTICS");
//        services = mBluetoothLeService.getSupportedGattServices();
//
//        BluetoothGattService rightService = null;
//        for (int i = 0; i < services.size(); i++) {
//        if (services.get(i).getUuid().toString().equals(BLE_UUID.SERVICE_INFORMATION)) {
//        rightService = services.get(i);
//        Log.e("service found : ", rightService.getUuid().toString());
//        }
//        }
//        //Adding the characteristics
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
//        Log.d(TAG, "onResume: Start REQUEST characteristics");
//        mBluetoothLeService.setCharacteristicList(chars);
//        mBluetoothLeService.requestCharacteristics();
//        }
