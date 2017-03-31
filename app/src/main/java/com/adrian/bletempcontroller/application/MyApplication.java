package com.adrian.bletempcontroller.application;

import android.app.Application;

import com.clj.fastble.BleManager;

/**
 * Created by adrian on 17-3-31.
 */

public class MyApplication extends Application {

    private static MyApplication instance;

    private BleManager bleManager;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        bleManager = new BleManager(this);
        bleManager.enableBluetooth();
    }

    public static MyApplication getInstance() {
        return instance;
    }

    public void closeBluetoothGatt() {
        bleManager.closeBluetoothGatt();
    }
}
