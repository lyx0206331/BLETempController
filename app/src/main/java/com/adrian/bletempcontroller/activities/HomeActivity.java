package com.adrian.bletempcontroller.activities;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.Bundle;
import android.os.Handler;
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
import android.widget.Toast;

import com.adrian.bletempcontroller.R;
import com.adrian.bletempcontroller.fragment.AccFragment;
import com.adrian.bletempcontroller.fragment.RealTimeFragment;
import com.adrian.bletempcontroller.utils.TempUtil;
import com.clj.fastble.BleManager;
import com.clj.fastble.conn.BleCharacterCallback;
import com.clj.fastble.conn.BleGattCallback;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.scan.ListScanCallback;
import com.clj.fastble.utils.HexUtil;

import static com.adrian.bletempcontroller.utils.Constants.CHAR_UUID;
import static com.adrian.bletempcontroller.utils.Constants.SERVICE_UUID;

public class HomeActivity extends BaseFragmentActivity implements RadioGroup.OnCheckedChangeListener {

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void initVariables() {
        initBle();
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
        mHandler.sendEmptyMessage(0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        fragments[0] = null;
        fragments[1] = null;
        fragments = null;
        bleManager.closeBluetoothGatt();
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

    /**
     * 搜索周围蓝牙设备
     */
    private void scanDevice() {
        if (bleManager.isInScanning())
            return;

        showProgress("正在搜索蓝牙设备...");

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
                        HomeActivity.this.hideProgress();
//                        showDeviceList(devices);
                        for (BluetoothDevice dev :
                                devices) {
                            Log.e(TAG, "dev name:" + dev.getName() + " / " + dev.getAddress());
                            if (!TextUtils.isEmpty(dev.getName()) && dev.getName().equals("zwg's pet")) {
                                curDev = dev;
                                bleManager.stopListenConnectCallback();
                                connectSpecialDevice();
                                return;
                            }
                        }
                    }
                });
            }

        });
    }

    BleGattCallback bleGattCallback = new BleGattCallback() {
        @Override
        public void onNotFoundDevice() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    hideProgress();
                    Toast.makeText(HomeActivity.this, "onNotFoundDevice", Toast.LENGTH_LONG).show();
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
                    hideProgress();
                    Log.e(TAG, "Mac Addr : " + curDev.getAddress());
                    showConnectState2(curDev.getName(), gatt);
                }
            });
        }

        @Override
        public void onConnectFailure(BleException exception) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    hideProgress();
//                        showDisConnectState();
//                    mHandler.sendEmptyMessageDelayed(0, intervalTime);
                    connectSpecialDevice();
                }
            });
            bleManager.handleException(exception);

            Log.e(TAG, "connect failed:" + exception.getCode() + " " + exception.getDescription());
        }
    };

    /**
     * 连接设备
     */
    private void connectSpecialDevice() {
        showProgress("正在连接设备...");
        bleManager.connectDevice(curDev, true, bleGattCallback);
    }

    private void showConnectState2(String deviceName, BluetoothGatt gatt) {
        bleManager.getBluetoothState();
//        String name = "宝宝";
//        ((AccFragment)fragments[0]).setUsrName(name);
//        ((RealTimeFragment)fragments[1]).setUsrName(name);

        if (gatt != null) {
            for (final BluetoothGattService service : gatt.getServices()) {
                boolean isSer = service.getUuid().toString().equals(SERVICE_UUID);
                if (isSer) {
                    for (final BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                        boolean isChar = characteristic.getUuid().toString().equals(CHAR_UUID);
//                        Log.e(TAG, "cur uuid : " + characteristic.getUuid());
                        if (isChar) {
                            if (characteristic.getValue() != null) {
                                ((AccFragment) fragments[0]).setTemp(String.valueOf(HexUtil.encodeHex(characteristic.getValue())));
                                mHandler.sendEmptyMessageDelayed(1, intervalTime);
                            } else {
                                mHandler.sendEmptyMessageDelayed(1, intervalTime);
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
                    boolean suc = startRead2(SERVICE_UUID, CHAR_UUID);
//            stopListen(CHAR_UUID);
                    if (!suc) {
//                        bleManager.stopListenCharacterCallback(CHAR_UUID);
                        connectSpecialDevice();
                    } else {
                        sendEmptyMessageDelayed(1, intervalTime);
                    }
                    break;
            }
        }
    };

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
                    values[0] = Integer.parseInt(all.substring(12, 16), 16) * .01f + getOffset();
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
