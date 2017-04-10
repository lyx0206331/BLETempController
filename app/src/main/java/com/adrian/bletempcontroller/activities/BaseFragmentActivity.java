package com.adrian.bletempcontroller.activities;

import android.support.v4.app.Fragment;

/**
 * Created by adrian on 16-7-17 00:36.
 */
public class BaseFragmentActivity extends BaseActivity {

    private Fragment fromFragment;

    protected void switchFragment(Fragment fragment, int layoutId) {
        if (null == fragment || fromFragment == fragment) {
            return;
        }
        if (!fragment.isAdded()) {
            if (null == fromFragment) {
                getSupportFragmentManager().beginTransaction().add(layoutId, fragment).commit();
            } else {
                getSupportFragmentManager().beginTransaction().hide(fromFragment).add(layoutId, fragment).commit();
            }
        } else {
            getSupportFragmentManager().beginTransaction().hide(fromFragment).show(fragment).commit();
        }
        fromFragment = fragment;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        fromFragment = null;
    }

    @Override
    protected void initVariables() {

    }

    @Override
    protected void initViews() {

    }

    @Override
    protected void loadData() {

    }
}
