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

    private BleManager bleManager;
    private String userName = "宝宝";

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

    public void setIntervalTime(int intervalTime) {
        this.intervalTime = intervalTime;
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
        showProgress("正在连接设备...");
        bleManager.connectDevice(device, true, new BleGattCallback() {
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
                        hideProgress();
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
    private int[] colors = {0x80ff0000/*, 0x8000ff00*/, 0x800000ff};

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
                                if (intervalTime == 1000) {
                                    index %= 2;
                                    ((AccFragment) fragments[0]).setCent(value, colors[index]);
                                    index++;
                                } else {
                                    ((RealTimeFragment) fragments[1]).setCent(value);
                                }
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
