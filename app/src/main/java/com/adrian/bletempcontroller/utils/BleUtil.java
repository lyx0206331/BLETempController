package com.adrian.bletempcontroller.utils;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.util.Log;

import com.clj.fastble.BleManager;
import com.clj.fastble.conn.BleCharacterCallback;
import com.clj.fastble.conn.BleGattCallback;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.scan.ListScanCallback;
import com.clj.fastble.utils.HexUtil;

/**
 * Created by adrian on 17-3-31.
 */

public class BleUtil {
    private static final String TAG = "BleUtil";

    private static final int SCAN_TIME = 5000;  //扫描时长
    private static final int LISTEN_INTERVAL = 1000;    //温度监听间隔

    private static BleUtil instance;

    private Context context;
    private BleManager bleManager;

    private IBleCallback callback;

    public BleUtil(Context context) {
        this.context = context;
        init();
    }

    private void init() {
        bleManager = new BleManager(context);
        bleManager.enableBluetooth();
    }

    public BleUtil getInstance(Context context) {
        if (instance == null) {
            instance = new BleUtil(context);
        }
        return instance;
    }

    public void closeBluetoothGatt() {
        bleManager.closeBluetoothGatt();
    }

    public void scanDevices() {
        if (bleManager.isInScanning()) {
            return;
        }

        bleManager.scanDevice(new ListScanCallback(5000) {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                super.onLeScan(device, rssi, scanRecord);
                Log.i(TAG, "发现设备：" + device.getName());
            }

            @Override
            public void onDeviceFound(final BluetoothDevice[] devices) {

            }

        });
    }

    /**
     * 连接设备
     */
    public void connectSpecialDevice(final BluetoothDevice device) {
//        progressDialog.show();
        bleManager.connectDevice(device, true, new BleGattCallback() {
            @Override
            public void onNotFoundDevice() {
//                progressDialog.dismiss();
//                Toast.makeText(OperateActivity.this, "onNotFoundDevice", Toast.LENGTH_LONG).show();
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
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        progressDialog.dismiss();
//                        Log.d(TAG, "Mac Addr : " + device.getAddress());
//                        showConnectState2(device.getName(), gatt);
//                    }
//                });
            }

            @Override
            public void onConnectFailure(BleException exception) {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        progressDialog.dismiss();
//                        showDisConnectState();
//                    }
//                });
                bleManager.handleException(exception);
            }
        });
    }

    /**
     * 直连某一蓝牙设备
     */
    public void connectNameDevice(final String deviceName) {
//        progressDialog.show();
        bleManager.scanNameAndConnect(deviceName, 5000, false, new BleGattCallback() {
            @Override
            public void onNotFoundDevice() {
//                progressDialog.dismiss();
//                Toast.makeText(OperateActivity.this, "onNotFoundDevice", Toast.LENGTH_LONG).show();
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
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        progressDialog.dismiss();
//                        showConnectState(deviceName, gatt);
//                    }
//                });
            }

            @Override
            public void onConnectFailure(BleException exception) {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        progressDialog.dismiss();
//                        showDisConnectState();
//                    }
//                });
                bleManager.handleException(exception);
            }
        });
    }

    public void startRead(String serviceUUID, final String characterUUID) {
        Log.i(TAG, "startRead");
        boolean suc = bleManager.readDevice(
                serviceUUID,
                characterUUID,
                new BleCharacterCallback() {
                    @Override
                    public void onSuccess(final BluetoothGattCharacteristic characteristic) {
                        Log.d(TAG, "read success: " + '\n' + String.valueOf(HexUtil.encodeHex(characteristic.getValue())));
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                View characterView = layout_character_list.findViewWithTag(characterUUID);
//                                if (characterView != null) {
//                                    TextView txt_value = (TextView) characterView.findViewById(R.id.txt_value);
//                                    if (txt_value != null) {
//                                        txt_value.setText(String.valueOf(HexUtil.encodeHex(characteristic.getValue())));
//                                    }
//                                }
//                            }
//                        });
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

    public void startWrite(String serviceUUID, final String characterUUID, String writeData) {
        Log.i(TAG, "startWrite");
        boolean suc = bleManager.writeDevice(
                serviceUUID,
                characterUUID,
                HexUtil.hexStringToBytes(writeData),
                new BleCharacterCallback() {
                    @Override
                    public void onSuccess(final BluetoothGattCharacteristic characteristic) {
                        Log.d(TAG, "write success: " + '\n' + String.valueOf(HexUtil.encodeHex(characteristic.getValue())));
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                View characterView = layout_character_list.findViewWithTag(characterUUID);
//                                if (characterView != null) {
//                                    TextView txt_value = (TextView) characterView.findViewById(R.id.txt_value);
//                                    if (txt_value != null) {
//                                        txt_value.setText(String.valueOf(HexUtil.encodeHex(characteristic.getValue())));
//                                    }
//                                }
//                            }
//                        });
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

    public void startNotify(String serviceUUID, final String characterUUID) {
        Log.i(TAG, "startNotify");
        boolean suc = bleManager.notify(
                serviceUUID,
                characterUUID,
                new BleCharacterCallback() {
                    @Override
                    public void onSuccess(final BluetoothGattCharacteristic characteristic) {
                        Log.d(TAG, "notify success： " + '\n' + String.valueOf(HexUtil.encodeHex(characteristic.getValue())));

//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                View characterView = layout_character_list.findViewWithTag(characterUUID);
//                                if (characterView != null) {
//                                    TextView txt_value = (TextView) characterView.findViewById(R.id.txt_value);
//                                    if (txt_value != null) {
//                                        txt_value.setText(String.valueOf(HexUtil.encodeHex(characteristic.getValue())));
//                                    }
//                                }
//                            }
//                        });
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

    public void startIndicate(String serviceUUID, final String characterUUID) {
        Log.i(TAG, "startIndicate");
        boolean suc = bleManager.indicate(
                serviceUUID,
                characterUUID,
                new BleCharacterCallback() {
                    @Override
                    public void onSuccess(final BluetoothGattCharacteristic characteristic) {
                        Log.d(TAG, "indicate success： " + '\n' + String.valueOf(HexUtil.encodeHex(characteristic.getValue())));
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                View characterView = layout_character_list.findViewWithTag(characterUUID);
//                                if (characterView != null) {
//                                    TextView txt_value = (TextView) characterView.findViewById(R.id.txt_value);
//                                    if (txt_value != null) {
//                                        txt_value.setText(String.valueOf(HexUtil.encodeHex(characteristic.getValue())));
//                                    }
//                                }
//                            }
//                        });
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

    public void stopListen(String characterUUID) {
        Log.i(TAG, "stopListen");
        bleManager.stopListenCharacterCallback(characterUUID);
    }

    public interface IBleCallback {
        void showProgressDialog(boolean show);

        void addScanDev(BluetoothDevice dev);

        void addScanDevs(BluetoothDevice[] devs);
    }
}
