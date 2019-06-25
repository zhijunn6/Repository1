package com.smarttrack.app;

import android.Manifest;
import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.location.LocationManager;
import android.os.Handler;
import android.os.ParcelUuid;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class DeviceScanActivity extends ListActivity  {

    private static final String TAG = "HELL";

    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private Handler mHandler;

    private Button button_scan;
    private boolean mScanning;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_COARSE_LOCATION = 2;
    // Stops scanning after 5 seconds.
    private static final long SCAN_PERIOD = 10000;
    private AnimationDrawable drawable;
    String deviceName;

    private DatabaseHelper myDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_scan);
        mHandler = new Handler();

        myDb = new DatabaseHelper(this);

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();

            AlertDialog alertDialog = new AlertDialog.Builder(DeviceScanActivity.this).create();
            alertDialog.setTitle(String.valueOf(R.string.app_name));
            alertDialog.setMessage(String.valueOf(R.string.error_bluetooth_not_supported));
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    });
            alertDialog.show();
            return;
        }

        //UI
        button_scan = findViewById(R.id.scanBtn);

        drawable = new AnimationDrawable();
        drawable.addFrame(new ColorDrawable(getResources().getColor(R.color.gradient_end_2)), 400);
        drawable.addFrame(new ColorDrawable(getResources().getColor(R.color.gradient_end_3)), 400);
        drawable.setOneShot(false);

        button_scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!mScanning){
                    Log.d(TAG, "onClick: Starting scan");
                    mLeDeviceListAdapter.clear();
                    mLeDeviceListAdapter = new LeDeviceListAdapter();
                    setListAdapter(mLeDeviceListAdapter);
                    scanLeDevice(true);

                    mScanning = true;
                    button_scan.setText(getString(R.string.scanning));
                    button_scan.setBackground(drawable);
                    drawable.setVisible(true, true);
                    drawable.start();
//                    getListView().setVisibility(View.VISIBLE);
                } else{
                    Log.d(TAG, "onClick: Stopped scanning");
                    scanLeDevice(false);

                    mScanning = false;
                    button_scan.setText(R.string.scan_Btn);
                    button_scan.setBackgroundResource(R.drawable.refresh_button);
                    drawable.setVisible(false, true);
                }
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    button_scan.setText(R.string.scan_Btn);
                    drawable.stop();
                    mScanning = false;
                    button_scan.setBackgroundResource(R.drawable.refresh_button);

                    mBluetoothLeScanner.stopScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);

            List<ScanFilter> filter = new ArrayList<>();
            ScanFilter scanFilter = new ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid.fromString(BLE_UUID.SERVICE_INFORMATION))
                    .build();
            filter.add(scanFilter);
            ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
            mScanning = false;
            mBluetoothLeScanner.startScan(filter, settings, mLeScanCallback);
        } else {
            mScanning = true;
            mBluetoothLeScanner.stopScan(mLeScanCallback);
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
        if (device == null) return;
        myDb.insertDevice(device.getAddress(), device.getName(), device.getName());

        final Intent intent = new Intent(this, DeviceMainActivity.class);
        //intent.putExtra(DeviceMainActivity.EXTRAS_DEVICE_NAME, deviceName);
        intent.putExtra(DeviceMainActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());


        if (mScanning) {
            Log.d(TAG, "onListItemClick: Stopped scan to next activity");

            mBluetoothLeScanner.stopScan(mLeScanCallback);
            drawable.stop();
            mScanning = false;
            button_scan.setBackgroundResource(R.drawable.refresh_button);
        }
        startActivity(intent);
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause: Called");
        super.onPause();
        if(mBluetoothLeScanner!=null) {
            scanLeDevice(false);
            mLeDeviceListAdapter.clear();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(ContextCompat.checkSelfPermission(DeviceScanActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED){
        } else {
            requestLocationPermission();
        }

        onLocation();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            Log.w(TAG, "onResume: BT is enabled");
            mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

            // Initializes list view adapter.
            Log.d(TAG, "onResume: Initializing");
            mLeDeviceListAdapter = new LeDeviceListAdapter();
            setListAdapter(mLeDeviceListAdapter);
            scanLeDevice(true);
        }
    }

    private void onLocation() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if( !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.location_request_string) // Want to enable?
                    .setPositiveButton(R.string.yes_string, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialogInterface, int i) {
                            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        }
                    })
                    .setNegativeButton(R.string.no_string, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            finish();
                        }
                    })
                    .show();
        }
    }

    private void requestLocationPermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)){
            new AlertDialog.Builder(this)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.location_permission_string)
                    .setPositiveButton(R.string.yes_string, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            dialog.cancel();
                            ActivityCompat.requestPermissions(DeviceScanActivity.this, new String[] {Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_COARSE_LOCATION);
                        }
                    })
                    .setNegativeButton(R.string.no_string, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            finish();
                        }
                    }).create().show();
        } else{
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_COARSE_LOCATION);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private ArrayList<Integer> mLeRssi;
        private ArrayList<String> mLocalDeviceName;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<>();
            mLeRssi = new ArrayList<>();
            mLocalDeviceName = new ArrayList<>();
            mInflator = DeviceScanActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device, int rssi) {
            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device);
                mLeRssi.add(rssi);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            Log.d(TAG, "getView: " + i);
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceName = view.findViewById(R.id.device_name);
                viewHolder.rssiStrength = view.findViewById(R.id.rssi_strength);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            Cursor res = myDb.readDevice(mLeDevices.get(i).getAddress());
            if(res.getCount() == 0){
                deviceName = device.getName();
            }

            while(res.moveToNext()) {
                deviceName = res.getString(res.getColumnIndex("EDITNAME"));
            }

            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);

            int rssi = mLeRssi.get(i);

            if (rssi >= -30) {
                viewHolder.rssiStrength.setImageResource(R.drawable.ic_signal_wifi_4_bar_black_24dp);
            } else if (rssi >= -50) {
                viewHolder.rssiStrength.setImageResource(R.drawable.ic_signal_wifi_3_bar_black_24dp);
            } else if (rssi >= -75) {
                viewHolder.rssiStrength.setImageResource(R.drawable.ic_signal_wifi_2_bar_black_24dp);
            } else if (rssi >= -90) {
                viewHolder.rssiStrength.setImageResource(R.drawable.ic_signal_wifi_1_bar_black_24dp);
            }

            return view;
        }
    }

    static class ViewHolder {
        TextView deviceName;
        ImageView rssiStrength;
    }

    // Device scan callback.
    ScanCallback mLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            mLeDeviceListAdapter.addDevice(result.getDevice(), result.getRssi());
            Log.d(TAG, "onScanResult: " + mLeDeviceListAdapter.getCount());
            mLeDeviceListAdapter.notifyDataSetChanged();
        }

        public void onScanFailed(int errorCode) {
            switch (errorCode) {
                case SCAN_FAILED_ALREADY_STARTED:
                    Log.d(TAG, "Already started");
                    break;
                case SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                    Log.d(TAG, "Cannot be registered");
                    break;
                case SCAN_FAILED_FEATURE_UNSUPPORTED:
                    Log.d(TAG, "Power optimized scan not supported");
                    break;
                case SCAN_FAILED_INTERNAL_ERROR:
                    Log.d(TAG, "Internal error");
                    break;
            }
        }
    };

}
