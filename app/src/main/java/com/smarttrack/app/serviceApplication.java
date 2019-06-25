package com.smarttrack.app;

import android.app.Application;

public class serviceApplication extends Application {
    public BluetoothLeService mBluetoothLeService;

    @Override
    public void onCreate() {
        super.onCreate();
        mBluetoothLeService = new BluetoothLeService();
    }
}
