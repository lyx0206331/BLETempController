package com.adrian.bletempcontroller;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.graphics.Color;
import android.icu.text.DecimalFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.ColorInt;
import android.support.annotation.IdRes;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.clj.fastble.BleManager;
import com.clj.fastble.conn.BleGattCallback;
import com.clj.fastble.conn.BleCharacterCallback;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.scan.ListScanCallback;
import com.clj.fastble.utils.HexUtil;

import java.util.Random;


/**
 * Created by 陈利健 on 2016/9/20.
 * 可作为工具测试
 */
public class OperateActivity extends AppCompatActivity implements View.OnClickListener, RadioGroup.OnCheckedChangeListener {

//    temId:SERVICE_UUID==0000fff0-0000-1000-8000-00805f9b34fb
//    CHAR_UUID==0000fff6-0000-1000-8000-00805f9b34fb

    private static final String SERVICE_UUID = "0000fff0-0000-1000-8000-00805f9b34fb";
    private static final String CHAR_UUID = "0000fff6-0000-1000-8000-00805f9b34fb";

    private static final String TAG = OperateActivity.class.getSimpleName();

    private LinearLayout layout_item_connect;
    private LinearLayout layout_device_list;
    private EditText et_device_name;

    private LinearLayout layout_item_state;
    private TextView txt_device_name;
    private LinearLayout layout_character_list;
    private LinearLayout layout_temperature;
    private TextView mDevNameTV;
    private TextView mTempTV;
    private Button mDisConnBtn;
    private RadioGroup mInvervalRG;

    private BleManager bleManager;
    private ProgressDialog progressDialog;

    private int intervalTime = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_operate);

        findViewById(R.id.btn_scan).setOnClickListener(this);
        findViewById(R.id.btn_connect).setOnClickListener(this);
        findViewById(R.id.btn_disconnect).setOnClickListener(this);

        layout_item_connect = (LinearLayout) findViewById(R.id.layout_item_connect);
        layout_device_list = (LinearLayout) findViewById(R.id.layout_device_list);
        et_device_name = (EditText) findViewById(R.id.et_device_name);

        layout_item_state = (LinearLayout) findViewById(R.id.layout_item_state);
        txt_device_name = (TextView) findViewById(R.id.txt_device_name);
        layout_character_list = (LinearLayout) findViewById(R.id.layout_character_list);
        layout_temperature = (LinearLayout) findViewById(R.id.layout_temperature);
        mInvervalRG = (RadioGroup) findViewById(R.id.rg_interval);
        mDevNameTV = (TextView) findViewById(R.id.tv_dev_name);
        mTempTV = (TextView) findViewById(R.id.tv_tem);
        mDisConnBtn = (Button) findViewById(R.id.btn_disconn);
        mDisConnBtn.setOnClickListener(this);
        mInvervalRG.setOnCheckedChangeListener(this);
        mInvervalRG.check(R.id.rb_one);

        bleManager = new BleManager(this);
        bleManager.enableBluetooth();

        showDisConnectState();
        progressDialog = new ProgressDialog(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bleManager.closeBluetoothGatt();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_scan:
                scanDevice();
                break;

            case R.id.btn_connect:
                String deviceName = et_device_name.getText().toString().trim();
                if (!TextUtils.isEmpty(deviceName)) {
                    connectNameDevice(deviceName);
                }
                break;

            case R.id.btn_disconnect:
            case R.id.btn_disconn:
                showDisConnectState();
                break;
        }
    }

    /**
     * 搜索周围蓝牙设备
     */
    private void scanDevice() {
        if (bleManager.isInScanning())
            return;

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
                        showDeviceList(devices);
                    }
                });
            }

        });
    }

    /**
     * 显示蓝牙设备列表
     */
    private void showDeviceList(final BluetoothDevice[] devices) {
        layout_device_list.removeAllViews();

        for (int i = 0; devices != null && i < devices.length; i++) {
            View itemView = LayoutInflater.from(this).inflate(R.layout.layout_list_item_device, null);

            RelativeLayout layout_item_device = (RelativeLayout) itemView.findViewById(R.id.layout_list_item_device);
            TextView txt_item_name = (TextView) itemView.findViewById(R.id.txt_item_name);

            final BluetoothDevice device = devices[i];
            txt_item_name.setText(device.getName());
            layout_item_device.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    connectSpecialDevice(device);
                }
            });

            layout_device_list.addView(itemView);
        }
    }

    /**
     * 连接设备
     */
    private void connectSpecialDevice(final BluetoothDevice device) {
        progressDialog.show();
        bleManager.connectDevice(device, true, new BleGattCallback() {
            @Override
            public void onNotFoundDevice() {
                progressDialog.dismiss();
                Toast.makeText(OperateActivity.this, "onNotFoundDevice", Toast.LENGTH_LONG).show();
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
                        Log.d(TAG, "Mac Addr : " + device.getAddress());
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
                        showDisConnectState();
                    }
                });
                bleManager.handleException(exception);
            }
        });
    }

    /**
     * 直连某一蓝牙设备
     */
    private void connectNameDevice(final String deviceName) {
        progressDialog.show();
        bleManager.scanNameAndConnect(deviceName, 5000, false, new BleGattCallback() {
            @Override
            public void onNotFoundDevice() {
                progressDialog.dismiss();
                Toast.makeText(OperateActivity.this, "onNotFoundDevice", Toast.LENGTH_LONG).show();
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
                        showConnectState(deviceName, gatt);
                    }
                });
            }

            @Override
            public void onConnectFailure(BleException exception) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                        showDisConnectState();
                    }
                });
                bleManager.handleException(exception);
            }
        });
    }

    /**
     * 显示未连接状态
     */
    private void showDisConnectState() {
        bleManager.closeBluetoothGatt();

        layout_item_connect.setVisibility(View.VISIBLE);
        layout_item_state.setVisibility(View.GONE);
        layout_temperature.setVisibility(View.GONE);

        layout_device_list.removeAllViews();
    }

    private void showConnectState2(String deviceName, BluetoothGatt gatt) {
        bleManager.getBluetoothState();
        layout_item_connect.setVisibility(View.GONE);
        layout_temperature.setVisibility(View.VISIBLE);
        mDevNameTV.setText(deviceName);

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
                                mHandler.sendEmptyMessageDelayed(0, intervalTime);
                            } else {
                                mHandler.sendEmptyMessageDelayed(0, 4000);
                            }
                        }

                    }
                }

            }
        }
    }

    Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            startRead2(SERVICE_UUID, CHAR_UUID);
//            stopListen(CHAR_UUID);
            sendEmptyMessageDelayed(0, intervalTime);
        }
    };

    /**
     * 显示连接状态
     */
    private void showConnectState(String deviceName, BluetoothGatt gatt) {
        bleManager.getBluetoothState();

        layout_item_connect.setVisibility(View.GONE);
        layout_item_state.setVisibility(View.VISIBLE);
        txt_device_name.setText(deviceName);

        layout_character_list.removeAllViews();
        if (gatt != null) {
            for (final BluetoothGattService service : gatt.getServices()) {
                View serviceView = LayoutInflater.from(this).inflate(R.layout.layout_list_item_service, null);
                TextView txt_service = (TextView) serviceView.findViewById(R.id.txt_service);
                txt_service.setText(service.getUuid().toString());

                layout_character_list.addView(serviceView);

                for (final BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                    View characterView = LayoutInflater.from(this).inflate(R.layout.layout_list_item_character, null);
                    characterView.setTag(characteristic.getUuid().toString());
                    TextView txt_character = (TextView) characterView.findViewById(R.id.txt_character);
                    final Button btn_properties = (Button) characterView.findViewById(R.id.btn_properties);
                    TextView txt_value = (TextView) characterView.findViewById(R.id.txt_value);

                    txt_character.setText(characteristic.getUuid().toString());
                    if(characteristic.getValue() != null) {
                        txt_value.setText(String.valueOf(HexUtil.encodeHex(characteristic.getValue())));
                    }
                    switch (characteristic.getProperties()) {
                        case 2:
                            btn_properties.setText(String.valueOf("read"));
                            btn_properties.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    if (btn_properties.getText().toString().equals("read")) {
                                        startRead(service.getUuid().toString(), characteristic.getUuid().toString());
                                    } else if (btn_properties.getText().toString().equals("stopListen")) {
                                        stopListen(characteristic.getUuid().toString());
                                        btn_properties.setText(String.valueOf("read"));
                                    }
                                }
                            });
                            break;

                        case 8:
                            btn_properties.setText(String.valueOf("write"));
                            btn_properties.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    if (btn_properties.getText().toString().equals("write")) {
                                        EditDialog dialog = new EditDialog(OperateActivity.this);
                                        dialog.setOnDialogClickListener(new EditDialog.OnDialogClickListener() {
                                            @Override
                                            public void onEditOkClick(String writeData) {
                                                startWrite(service.getUuid().toString(), characteristic.getUuid().toString(), writeData);
                                            }

                                            @Override
                                            public void onEditErrorClick() {
                                                Log.e(TAG, "write error");
                                            }
                                        });
                                        dialog.show();
                                    } else if (btn_properties.getText().toString().equals("stopListen")) {
                                        stopListen(characteristic.getUuid().toString());
                                        btn_properties.setText(String.valueOf("write"));
                                    }
                                }
                            });
                            break;

                        case 16:
                            btn_properties.setText(String.valueOf("notify"));
                            btn_properties.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    if (btn_properties.getText().toString().equals("notify")) {
                                        startNotify(service.getUuid().toString(), characteristic.getUuid().toString());
                                    } else if (btn_properties.getText().toString().equals("stopListen")) {
                                        stopListen(characteristic.getUuid().toString());
                                        bleManager.stopNotify(service.getUuid().toString(), characteristic.getUuid().toString());
                                        btn_properties.setText(String.valueOf("notify"));
                                    }
                                }
                            });
                            break;

                        case 32:
                            btn_properties.setText(String.valueOf("indicate"));
                            btn_properties.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    if (btn_properties.getText().toString().equals("indicate")) {
                                        startIndicate(service.getUuid().toString(), characteristic.getUuid().toString());
                                    } else if (btn_properties.getText().toString().equals("stopListen")) {
                                        stopListen(characteristic.getUuid().toString());
                                        bleManager.stopIndicate(service.getUuid().toString(), characteristic.getUuid().toString());
                                        btn_properties.setText(String.valueOf("indicate"));
                                    }
                                }
                            });
                            break;
                        default:
                            btn_properties.setText(String.valueOf("read"));
                            btn_properties.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    String temSerId = service.getUuid().toString();
                                    String temCharId = characteristic.getUuid().toString();
                                    Log.e(TAG, "temId:SERVICE_UUID==" + temSerId + "\nCHAR_UUID==" + temCharId);
                                    if (btn_properties.getText().toString().equals("read")) {
                                        startRead(temSerId, temCharId);
                                    } else if (btn_properties.getText().toString().equals("stopListen")) {
                                        stopListen(temCharId);
                                        btn_properties.setText(String.valueOf("read"));
                                    }
                                }
                            });
                            break;
                    }
                    layout_character_list.addView(characterView);

                }
            }
        }
    }

    private void startNotify(String serviceUUID, final String characterUUID) {
        Log.i(TAG, "startNotify");
        boolean suc = bleManager.notify(
                serviceUUID,
                characterUUID,
                new BleCharacterCallback() {
                    @Override
                    public void onSuccess(final BluetoothGattCharacteristic characteristic) {
                        Log.d(TAG, "notify success： " + '\n' + String.valueOf(HexUtil.encodeHex(characteristic.getValue())));

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                View characterView = layout_character_list.findViewWithTag(characterUUID);
                                if (characterView != null) {
                                    TextView txt_value = (TextView) characterView.findViewById(R.id.txt_value);
                                    if (txt_value != null) {
                                        txt_value.setText(String.valueOf(HexUtil.encodeHex(characteristic.getValue())));
                                    }
                                }
                            }
                        });
                    }

                    @Override
                    public void onFailure(BleException exception) {
                        bleManager.handleException(exception);
                    }
                });

        if (suc) {
            View characterView = layout_character_list.findViewWithTag(characterUUID);
            if (characterView != null) {
                Button btn_properties = (Button) characterView.findViewById(R.id.btn_properties);
                btn_properties.setText(String.valueOf("stopListen"));
            }
        }
    }

    private void startIndicate(String serviceUUID, final String characterUUID) {
        Log.i(TAG, "startIndicate");
        boolean suc = bleManager.indicate(
                serviceUUID,
                characterUUID,
                new BleCharacterCallback() {
                    @Override
                    public void onSuccess(final BluetoothGattCharacteristic characteristic) {
                        Log.d(TAG, "indicate success： " + '\n' + String.valueOf(HexUtil.encodeHex(characteristic.getValue())));
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                View characterView = layout_character_list.findViewWithTag(characterUUID);
                                if (characterView != null) {
                                    TextView txt_value = (TextView) characterView.findViewById(R.id.txt_value);
                                    if (txt_value != null) {
                                        txt_value.setText(String.valueOf(HexUtil.encodeHex(characteristic.getValue())));
                                    }
                                }
                            }
                        });
                    }

                    @Override
                    public void onFailure(BleException exception) {
                        bleManager.handleException(exception);
                    }
                });

        if (suc) {
            View characterView = layout_character_list.findViewWithTag(characterUUID);
            if (characterView != null) {
                Button btn_properties = (Button) characterView.findViewById(R.id.btn_properties);
                btn_properties.setText(String.valueOf("stopListen"));
            }
        }
    }

    private void startWrite(String serviceUUID, final String characterUUID, String writeData) {
        Log.i(TAG, "startWrite");
        boolean suc = bleManager.writeDevice(
                serviceUUID,
                characterUUID,
                HexUtil.hexStringToBytes(writeData),
                new BleCharacterCallback() {
                    @Override
                    public void onSuccess(final BluetoothGattCharacteristic characteristic) {
                        Log.d(TAG, "write success: " + '\n' + String.valueOf(HexUtil.encodeHex(characteristic.getValue())));
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                View characterView = layout_character_list.findViewWithTag(characterUUID);
                                if (characterView != null) {
                                    TextView txt_value = (TextView) characterView.findViewById(R.id.txt_value);
                                    if (txt_value != null) {
                                        txt_value.setText(String.valueOf(HexUtil.encodeHex(characteristic.getValue())));
                                    }
                                }
                            }
                        });
                    }

                    @Override
                    public void onFailure(BleException exception) {
                        bleManager.handleException(exception);
                    }
                });

        if (suc) {
            View characterView = layout_character_list.findViewWithTag(characterUUID);
            if (characterView != null) {
                Button btn_properties = (Button) characterView.findViewById(R.id.btn_properties);
                btn_properties.setText(String.valueOf("stopListen"));
            }
        }
    }

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
                        Log.d(TAG, "read success: " + '\n' + String.valueOf(HexUtil.encodeHex(characteristic.getValue())));
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                float value = Integer.parseInt(String.valueOf(HexUtil.encodeHex(characteristic.getValue())).substring(0, 4), 16) * 1.0f / 10;
                                mTempTV.setText(getString(R.string.centigrade, value));
                                index %= 3;
                                mTempTV.setTextColor(colors[index]);
                                index++;
                            }
                        });
                    }

                    @Override
                    public void onFailure(BleException exception) {
                        bleManager.handleException(exception);
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

    private void startRead(String serviceUUID, final String characterUUID) {
        Log.i(TAG, "startRead");
        boolean suc = bleManager.readDevice(
                serviceUUID,
                characterUUID,
                new BleCharacterCallback() {
                    @Override
                    public void onSuccess(final BluetoothGattCharacteristic characteristic) {
                        Log.d(TAG, "read success: " + '\n' + String.valueOf(HexUtil.encodeHex(characteristic.getValue())));
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                View characterView = layout_character_list.findViewWithTag(characterUUID);
                                if (characterView != null) {
                                    TextView txt_value = (TextView) characterView.findViewById(R.id.txt_value);
                                    if (txt_value != null) {
                                        txt_value.setText(String.valueOf(HexUtil.encodeHex(characteristic.getValue())));
                                    }
                                }
                            }
                        });
                    }

                    @Override
                    public void onFailure(BleException exception) {
                        bleManager.handleException(exception);
                    }
                });

        if (suc) {
            View characterView = layout_character_list.findViewWithTag(characterUUID);
            if (characterView != null) {
                Button btn_properties = (Button) characterView.findViewById(R.id.btn_properties);
                btn_properties.setText(String.valueOf("stopListen"));
            }
        }
    }

    private void stopListen(String characterUUID) {
        Log.i(TAG, "stopListen");
        bleManager.stopListenCharacterCallback(characterUUID);
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
}
