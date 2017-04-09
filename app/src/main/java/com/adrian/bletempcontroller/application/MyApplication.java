package com.adrian.bletempcontroller.application;

import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.adrian.bletempcontroller.OperateActivity;
import com.adrian.bletempcontroller.R;
import com.clj.fastble.BleManager;
import com.clj.fastble.conn.BleCharacterCallback;
import com.clj.fastble.conn.BleGattCallback;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.scan.ListScanCallback;
import com.clj.fastble.utils.HexUtil;

/**
 * Created by adrian on 17-3-31.
 */

public class MyApplication extends Application {

    private static final String TAG = "MyApplication";

    private static final int SCAN_TIME = 5000;  //扫描时长
    private int listenInterval = 1000;    //温度监听间隔

    private static MyApplication instance;

    private BleManager bleManager;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

//        bleManager = new BleManager(this);
//        bleManager.enableBluetooth();
    }

    public static MyApplication getInstance() {
        return instance;
    }

    public BleManager getBleManager() {
        if (bleManager == null) {
            bleManager = new BleManager(this);
            bleManager.enableBluetooth();
        }
        return bleManager;
    }


}
