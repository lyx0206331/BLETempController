package com.adrian.bletempcontroller.fragment;


import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.adrian.bletempcontroller.R;
import com.adrian.bletempcontroller.activities.HomeActivity;

/**
 * 实时测量
 */
public class RealTimeFragment extends BaseFragment {

    private static final String TAG = "RealTimeFragment";

    private static final int INTERVAL_RT = 1;

    private ImageView mBgIV;
    private TextView mRealtimeTempTV;
    private TextView mUsrNameTV;

    private float[] cents = new float[4];
    private float cent = 37.8f;
    private int index;

    private String temp;

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    refreshData();
                    break;
                case 1:
                    break;
                default:
                    break;
            }
        }
    };

    public RealTimeFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View mLayout = inflater.inflate(R.layout.fragment_real_time, container, false);
        mBgIV = (ImageView) mLayout.findViewById(R.id.iv_bg);
        mRealtimeTempTV = (TextView) mLayout.findViewById(R.id.tv_realtime_temp);
        mUsrNameTV = (TextView) mLayout.findViewById(R.id.tv_rt_name);
        mUsrNameTV.setText(((HomeActivity) getActivity()).getUserName());
        ((HomeActivity) getActivity()).setType(INTERVAL_RT);
//        mHandler.sendEmptyMessage(0);
        return mLayout;
    }

    @Override
    protected void lazyLoad() {

    }

    @Override
    protected void onVisible() {
        super.onVisible();
        Log.e(TAG, "onVisible");
        ((HomeActivity) getActivity()).setType(INTERVAL_RT);
        mHandler.sendEmptyMessage(0);
    }

    @Override
    protected void onInvisible() {
        super.onInvisible();
        mHandler.removeMessages(0);
    }

    private void refreshData() {
//        if (index == 3 && !TextUtils.isEmpty(temp)) {
//            mRealtimeTempTV.setText(temp);
//        } else {
        if (cents != null && cents.length == 4) {
            mRealtimeTempTV.setText(getString(R.string.centigrade, cents[index]));
        }
//        }
        index++;
        index %= 4;
        mHandler.sendEmptyMessageDelayed(0, 250);
    }

    private void startRotate() {
        Animation anim = AnimationUtils.loadAnimation(getContext(), R.anim.clockwise_rotation);
        mBgIV.startAnimation(anim);
    }

    public void setUsrName(String name) {
        if (TextUtils.isEmpty(name)) {
            return;
        }
        mUsrNameTV.setText(name);
    }

    public void setTemp(String temp) {
        this.temp = temp;
//        mRealtimeTempTV.setText(temp);
    }

    public void setCents(float[] values) {
        cents = values;
        if (!mHandler.hasMessages(0)) {
            mHandler.sendEmptyMessage(0);
        }
    }

    public void setCent(float value) {
        cent = value;
        for (int i = 0; i < 4; i++) {
            float r = (float) Math.random();
            cents[i] = value + r;
        }
    }
}
