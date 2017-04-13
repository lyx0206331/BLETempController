package com.adrian.bletempcontroller.activities;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.IdRes;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.animation.TranslateAnimation;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.SimpleExpandableListAdapter;
import android.widget.Toast;

import com.adrian.bletempcontroller.R;
import com.adrian.bletempcontroller.fragment.AccFragment;
import com.adrian.bletempcontroller.fragment.RealTimeFragment;
import com.adrian.bletempcontroller.utils.Constants;
import com.adrian.bletempcontroller.utils.TempUtil;
import com.clj.fastble.BleManager;
import com.clj.fastble.conn.BleCharacterCallback;
import com.clj.fastble.conn.BleGattCallback;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.scan.ListScanCallback;
import com.clj.fastble.utils.HexUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.adrian.bletempcontroller.utils.Constants.CHAR_UUID;
import static com.adrian.bletempcontroller.utils.Constants.SERVICE_UUID;

public class Home2Activity extends BaseFragmentActivity implements RadioGroup.OnCheckedChangeListener {

    private static final String TAG = "HomeActivity";

    private static final int FRAGMENT_COUNT = 2;

    private Fragment[] fragments = new Fragment[FRAGMENT_COUNT];

    private RadioGroup mMeasureRG;
    private View mLine;
    private int lineWidth;
    private int curPos;

    private int intervalTime = 1000;
    private int type = 0;

    private BleManager bleManager;
    private String userName = "宝宝";
    private BluetoothDevice curDev;

    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    private BluetoothLeService mBluetoothLeService;

    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            Log.e(TAG, "connected!" + (curDev == null));
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(curDev.getAddress());
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

//        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
//        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        scanLeDevice(true);
    }

    @Override
    protected void initVariables() {
//        initBle();
    }

    private void initBle() {
        bleManager = new BleManager(this);
        bleManager.enableBluetooth();
    }

    @Override
    protected void initViews() {
        setContentView(R.layout.activity_home);
        setTitle(R.string.home);
        fragments[0] = new AccFragment();
        fragments[1] = new RealTimeFragment();
        switchFragment(fragments[0], R.id.fragment_container);

        mMeasureRG = (RadioGroup) findViewById(R.id.rg_measure);
        mLine = findViewById(R.id.line);
        DisplayMetrics dm = TempUtil.getDisplayMetrics(this);
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mLine.getLayoutParams();
        lineWidth = dm.widthPixels / FRAGMENT_COUNT;
        lp.width = lineWidth;
        mLine.setLayoutParams(lp);

        mMeasureRG.setOnCheckedChangeListener(this);
        mMeasureRG.check(R.id.rb_acc);
    }

    @Override
    protected void loadData() {
//        mHandler.sendEmptyMessage(0);
    }

    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(curDev.getAddress());
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();

            Log.e(TAG, "service uuid:" + gattService.getUuid() + " type:" + gattService.getType());
            if (gattService.getUuid().equals(Constants.SERVICE_UUID)) {

                // Loops through available Characteristics.
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    String tempUUID = gattCharacteristic.getUuid().toString();
                    Log.e(TAG, "uuid:" + tempUUID);
                    if (tempUUID.equals(Constants.CHAR_UUID)) {
                        Log.e(TAG, "value:" + gattCharacteristic.getValue());
                    }
                }
                return;
            }
        }
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TempUtil.showToast(resourceId);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        fragments[0] = null;
        fragments[1] = null;
        fragments = null;
//        bleManager.closeBluetoothGatt();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
        switch (checkedId) {
            case R.id.rb_acc:
                moveLine(0);
                switchFragment(fragments[0], R.id.fragment_container);
                break;
            case R.id.rb_now:
                moveLine(1);
                switchFragment(fragments[1], R.id.fragment_container);
                break;
            default:
                break;
        }
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getUserName() {
        return userName;
    }

    /**
     * 移动顶部tab底线
     *
     * @param des
     */
    private void moveLine(int des) {
        TranslateAnimation anim = new TranslateAnimation(curPos * lineWidth, des * lineWidth, 0, 0);
        anim.setDuration(200);
        anim.setFillAfter(true);
//        mLine.setAnimation(anim);
        mLine.startAnimation(anim);
        curPos = des;
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            Log.e(TAG, "start scan device!");
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            Log.e(TAG, "stop scan device!");
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    if (!TextUtils.isEmpty(device.getName()) && device.getName().equals("zwg's pet")) {
                        curDev = device;
                        Log.e(TAG, "device info:" + device.getName() + "/" + device.getAddress());
                        scanLeDevice(false);
                        Intent gattServiceIntent = new Intent(Home2Activity.this, BluetoothLeService.class);
                        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
//                        mBluetoothLeService.connect(device.getAddress());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                TempUtil.showToast("device:" + device.getName() + "/" + device.getAddress());
                            }
                        });
                    }
                }
            };

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0:
                    scanLeDevice(true);
                    break;
                case 1:
                    boolean suc = startRead2(SERVICE_UUID, CHAR_UUID);
//                    if (suc) {
                    sendEmptyMessageDelayed(1, intervalTime);
//                    } else {
//                        removeMessages(1);
//                    }
                    break;
            }
        }
    };

    private void displayData(String data) {
        if (data != null) {
//            mDataField.setText(data);
            Log.e(TAG, "data:" + data);
        }
    }

    /**
     * 获取-.01~.01的偏移量
     *
     * @return
     */
    private float getOffset() {
        float[] offsets = {-.01f, 0, .01f};
        int index = (int) (Math.random() * 3);
        return offsets[index];
    }

    private int index = 0;
    private int[] colors = {0x80ff0000/*, 0x8000ff00*/, 0x800000ff};
    private BleCharacterCallback characterCallback = new BleCharacterCallback() {
        @Override
        public void onSuccess(final BluetoothGattCharacteristic characteristic) {
            Log.e(TAG, "interval:" + intervalTime);
            Log.e(TAG, "read success: " + '\n' + String.valueOf(HexUtil.encodeHex(characteristic.getValue())));
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String all = String.valueOf(HexUtil.encodeHex(characteristic.getValue()));
                    float[] values = new float[4];
                    values[3] = Integer.parseInt(all.substring(0, 4), 16) * .01f + getOffset();
                    values[2] = Integer.parseInt(all.substring(4, 8), 16) * .01f + getOffset();
                    values[1] = Integer.parseInt(all.substring(8, 12), 16) * .01f + getOffset();
                    values[0] = Integer.parseInt(all.substring(12, 16), 16) * .01f;
//                                String value = new String(characteristic.getValue());
                    if (type == 0) {
                        index %= 2;
                        ((AccFragment) fragments[0]).setCent(values[0], colors[index]);
//                                    ((AccFragment) fragments[0]).setTemp(value);
                        index++;
                    } else {
                        ((RealTimeFragment) fragments[1]).setCents(values);
//                                    ((RealTimeFragment) fragments[1]).setTemp(value);
                    }
                }
            });
        }

        @Override
        public void onFailure(BleException exception) {
            bleManager.handleException(exception);
            Log.e(TAG, "read failed:" + exception.getCode() + " " + exception.getDescription());
//            if (bleManager.isConnected()) {
//                mHandler.sendEmptyMessage(1);
//            } else {
//                if (!bleManager.isInScanning()) {
////                    bleManager.closeBluetoothGatt();
////                    initBle();
//                    mHandler.sendEmptyMessageDelayed(0, 1000);
//                }
//            }
        }
    };

    private boolean startRead2(String serviceUUID, final String characterUUID) {
        Log.i(TAG, "startRead");
        boolean suc = bleManager.readDevice(
                serviceUUID,
                characterUUID,
                characterCallback);

        Log.e(TAG, "read success:" + suc);
        if (!suc) {
//            bleManager.stopListenConnectCallback();
//            bleManager.closeBluetoothGatt();
//            bleManager=null;
//            initBle();
//            if (!bleManager.isInScanning()) {
//                mHandler.sendEmptyMessage(0);
//            }
        }
        return suc;
    }
}
