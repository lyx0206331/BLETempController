package com.adrian.bletempcontroller.activities;

import android.support.annotation.IdRes;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;

import com.adrian.bletempcontroller.R;

public class HomeActivity extends BaseActivity implements RadioGroup.OnCheckedChangeListener {

    private RadioGroup mMeasureRG;
    private View mLine;
    private RelativeLayout mAccMeasureRL;
    private RelativeLayout mNowMeasureRL;
    private int lineWidth;
    private int curPos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void initVariables() {

    }

    @Override
    protected void initViews() {
        setContentView(R.layout.activity_home);
        setTitle(R.string.home);

        mMeasureRG = (RadioGroup) findViewById(R.id.rg_measure);
        mAccMeasureRL = (RelativeLayout) findViewById(R.id.rl_acc);
        mNowMeasureRL = (RelativeLayout) findViewById(R.id.rl_now);
        mLine = findViewById(R.id.line);
        DisplayMetrics dm = getDisplayMetrics();
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mLine.getLayoutParams();
        lineWidth = dm.widthPixels / 2;
        lp.width = lineWidth;
        mLine.setLayoutParams(lp);

        mMeasureRG.setOnCheckedChangeListener(this);
    }

    @Override
    protected void loadData() {

    }

    @Override
    public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
        switch (checkedId) {
            case R.id.rb_acc:
                moveLine(0);
                mAccMeasureRL.setVisibility(View.VISIBLE);
                mNowMeasureRL.setVisibility(View.GONE);
                break;
            case R.id.rb_now:
                moveLine(1);
                mAccMeasureRL.setVisibility(View.GONE);
                mNowMeasureRL.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }

    private void moveLine(int des) {
        TranslateAnimation anim = new TranslateAnimation(curPos * lineWidth, des * lineWidth, 0, 0);
        anim.setDuration(200);
        anim.setFillAfter(true);
//        mLine.setAnimation(anim);
        mLine.startAnimation(anim);
        curPos = des;
    }

    private DisplayMetrics getDisplayMetrics() {
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        return dm;
    }

    /**
     * 摄氏度转华氏度
     *
     * @param centigrade 摄氏度
     * @return
     */
    private float c2f(float centigrade) {
        return centigrade * 33.8f;
    }
}
