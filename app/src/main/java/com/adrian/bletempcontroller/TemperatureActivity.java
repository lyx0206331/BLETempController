package com.adrian.bletempcontroller;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class TemperatureActivity extends AppCompatActivity {

    private TextView mTemTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_temperature);
        mTemTV = (TextView) findViewById(R.id.tv_tem);
    }
}
