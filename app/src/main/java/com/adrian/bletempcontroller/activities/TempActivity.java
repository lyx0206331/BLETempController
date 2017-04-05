package com.adrian.bletempcontroller.activities;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.IdRes;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
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

public class TempActivity extends AppCompatActivity implements RadioGroup.OnCheckedChangeListener {

    private static final String TAG = TempActivity.class.getSimpleName();

    private static final String SERVICE_UUID = "0000fff0-0000-1000-8000-00805f9b34fb";
    private static final String CHAR_UUID = "0000fff6-0000-1000-8000-00805f9b34fb";

    private TextView mDevNameTV;
    private TextView mTempTV;
    private RadioGroup mInvervalRG;
    private BleManager bleManager;
    private ProgressDialog progressDialog;

    private int intervalTime = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_temperature);

        mInvervalRG = (RadioGroup) findViewById(R.id.rg_interval);
        mDevNameTV = (TextView) findViewById(R.id.tv_dev_name);
        mTempTV = (TextView) findViewById(R.id.tv_tem);
        mInvervalRG.setOnCheckedChangeListener(this);
        mInvervalRG.check(R.id.rb_one);

        initBle();

        progressDialog = new ProgressDialog(this);
        progressDialog.setCanceledOnTouchOutside(false);
    }

    private void initBle() {
        bleManager = new BleManager(this);
        bleManager.enableBluetooth();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mHandler.sendEmptyMessage(0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        progressDialog.cancel();
        progressDialog = null;
        bleManager.closeBluetoothGatt();
    }

    @Override
    public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
        switch (checkedId) {
            case R.id.rb_one:
                intervalTime = 1000;
                break;
            case R.id.rb_five:
                intervalTime = 5000;
                break;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (bleManager.isInScanning()) {
            bleManager.stopListenConnectCallback();
            finish();
        }
    }

    /**
     * 搜索周围蓝牙设备
     */
    private void scanDevice() {
        if (bleManager.isInScanning())
            return;

        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
        }
        progressDialog.show();

        bleManager.scanDevice(new ListScanCallback(5000) {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                super.onLeScan(device, rssi, scanRecord);
                Log.i(TAG, "发现设备：" + device.getName());
            }

            @Override
            public void onDeviceFound(final BluetoothDevice[] devices) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
//                        showDeviceList(devices);
                        for (BluetoothDevice dev :
                                devices) {
                            Log.e(TAG, "dev name:" + dev.getName() + " / " + dev.getAddress());
                            if (!TextUtils.isEmpty(dev.getName()) && dev.getName().equals("zwg's pet")) {
                                bleManager.stopListenConnectCallback();
                                connectSpecialDevice(dev);
                                return;
                            }
                        }
                    }
                });
            }

        });
    }

    /**
     * 连接设备
     */
    private void connectSpecialDevice(final BluetoothDevice device) {
        progressDialog.show();
        bleManager.connectDevice(device, true, new BleGattCallback() {
            @Override
            public void onNotFoundDevice() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                        Toast.makeText(TempActivity.this, "onNotFoundDevice", Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onFoundDevice(BluetoothDevice device) {

            }

            @Override
            public void onConnectSuccess(BluetoothGatt gatt, int status) {
                gatt.discoverServices();
            }

            @Override
            public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                        Log.e(TAG, "Mac Addr : " + device.getAddress());
                        showConnectState2(device.getName(), gatt);
                    }
                });
            }

            @Override
            public void onConnectFailure(BleException exception) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
//                        showDisConnectState();
                        mHandler.sendEmptyMessageDelayed(0, 1000);
                    }
                });
                bleManager.handleException(exception);

                Log.e(TAG, "exception:" + exception.getCode() + " " + exception.getDescription());
            }
        });
    }

    private void showConnectState2(String deviceName, BluetoothGatt gatt) {
        bleManager.getBluetoothState();
        mDevNameTV.setText("宝宝");

        if (gatt != null) {
            for (final BluetoothGattService service : gatt.getServices()) {
                boolean isSer = service.getUuid().toString().equals(SERVICE_UUID);
                if (isSer) {
                    for (final BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                        boolean isChar = characteristic.getUuid().toString().equals(CHAR_UUID);
//                        Log.e(TAG, "cur uuid : " + characteristic.getUuid());
                        if (isChar) {
                            if (characteristic.getValue() != null) {
                                mTempTV.setText(String.valueOf(HexUtil.encodeHex(characteristic.getValue())));
                                mHandler.sendEmptyMessageDelayed(1, intervalTime);
                            } else {
                                mHandler.sendEmptyMessageDelayed(1, 4000);
                            }
                        }

                    }
                }

            }
        }
    }

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0:
                    scanDevice();
                    break;
                case 1:
                    startRead2(SERVICE_UUID, CHAR_UUID);
//            stopListen(CHAR_UUID);
                    sendEmptyMessageDelayed(1, intervalTime);
                    break;
            }
        }
    };

    private int index = 0;
    private int[] colors = {0x80ff0000, 0x8000ff00, 0x800000ff};

    private void startRead2(String serviceUUID, final String characterUUID) {
        Log.i(TAG, "startRead");
        boolean suc = bleManager.readDevice(
                serviceUUID,
                characterUUID,
                new BleCharacterCallback() {
                    @Override
                    public void onSuccess(final BluetoothGattCharacteristic characteristic) {
                        Log.e(TAG, "read success: " + '\n' + String.valueOf(HexUtil.encodeHex(characteristic.getValue())));
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                float value = Integer.parseInt(String.valueOf(HexUtil.encodeHex(characteristic.getValue())).substring(0, 4), 16) * 1.0f / 10;
                                mTempTV.setText(getString(R.string.temp_info, value));
                                index %= 3;
                                mTempTV.setTextColor(colors[index]);
                                index++;
                            }
                        });
                    }

                    @Override
                    public void onFailure(BleException exception) {
                        bleManager.handleException(exception);
                        Log.e(TAG, "exception:" + exception.getCode() + " " + exception.getDescription());
                        bleManager.closeBluetoothGatt();
                        initBle();
                        mHandler.sendEmptyMessageDelayed(0, 1000);
                    }
                });

        if (suc) {
//            View characterView = layout_character_list.findViewWithTag(characterUUID);
//            if (characterView != null) {
//                Button btn_properties = (Button) characterView.findViewById(R.id.btn_properties);
//                btn_properties.setText(String.valueOf("stopListen"));
//            }
        }
    }
}
