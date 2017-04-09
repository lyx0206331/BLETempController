package com.adrian.bletempcontroller.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.adrian.bletempcontroller.R;

public class LoginActivity extends BaseActivity implements View.OnClickListener {

    private Button mExpBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void initVariables() {

    }

    @Override
    protected void initViews() {
        setContentView(R.layout.activity_login);
        setTitle(R.string.login);
        mExpBtn = (Button) findViewById(R.id.btn_exp);

        mExpBtn.setOnClickListener(this);
    }

    @Override
    protected void loadData() {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_exp:
                startActivity(HomeActivity.class);
                finish();
                break;
        }
    }
}
