package com.adrian.bletempcontroller.fragment;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.adrian.bletempcontroller.R;
import com.adrian.bletempcontroller.activities.HomeActivity;

/**
 * 精确测量
 */
public class AccFragment extends BaseFragment implements View.OnClickListener {

    private static final String TAG = "AccFragment";

    private static final int INTERVAL_ACC = 0;

    private TextView mAccTempTV;
    private TextView mUsrNameTV;
    private TextView mMaxTempTV;
    private Button mResetBtn;

    private float maxTemp = 0f;

    public AccFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View mLayout = inflater.inflate(R.layout.fragment_acc, container, false);
        mAccTempTV = (TextView) mLayout.findViewById(R.id.tv_acc_temp);
        mUsrNameTV = (TextView) mLayout.findViewById(R.id.tv_acc_name);
        mMaxTempTV = (TextView) mLayout.findViewById(R.id.tv_max);
        mResetBtn = (Button) mLayout.findViewById(R.id.btn_reset);
        mResetBtn.setOnClickListener(this);
//        ((HomeActivity)getActivity()).showProgress("loading...");
        mUsrNameTV.setText(((HomeActivity) getActivity()).getUserName());
        ((HomeActivity) getActivity()).setType(INTERVAL_ACC);
        return mLayout;
    }

    @Override
    protected void lazyLoad() {

    }

    @Override
    protected void onVisible() {
        super.onVisible();
        Log.e(TAG, "onVisible");
        ((HomeActivity) getActivity()).setType(INTERVAL_ACC);
    }

    @Override
    protected void onInvisible() {
        super.onInvisible();
    }

    public void setUsrName(String name) {
        mUsrNameTV.setText(name);
    }

    public void setTemp(String temp) {
        mAccTempTV.setText(temp);
    }

    public void setCent(float value) {
        getMaxTemp(value);
        mAccTempTV.setText(getString(R.string.centigrade, value));
    }

    private void getMaxTemp(float value) {
        maxTemp = Math.max(maxTemp, value);
        mMaxTempTV.setText(getString(R.string.centigrade, maxTemp));
    }

    public void setCent(float value, int color) {
        getMaxTemp(value);
        mAccTempTV.setText(getString(R.string.centigrade, value));
        mAccTempTV.setTextColor(color);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_reset:
                mMaxTempTV.setText("0.00℃");
                break;
        }
    }
}
