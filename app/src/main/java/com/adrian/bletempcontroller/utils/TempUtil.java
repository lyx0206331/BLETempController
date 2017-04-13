package com.adrian.bletempcontroller.utils;

import android.app.Activity;
import android.content.Context;
import android.location.LocationManager;
import android.util.DisplayMetrics;
import android.widget.Toast;

import com.adrian.bletempcontroller.application.MyApplication;

/**
 * Created by adrian on 17-4-10.
 */

public class TempUtil {

    public static DisplayMetrics getDisplayMetrics(Activity context) {
        DisplayMetrics dm = new DisplayMetrics();
        context.getWindowManager().getDefaultDisplay().getMetrics(dm);
        return dm;
    }

    /**
     * 摄氏度转华氏度
     *
     * @param centigrade 摄氏度
     * @return
     */
    public static float c2f(float centigrade) {
        return centigrade * 33.8f;
    }

    public static void showToast(String msg) {
        Toast.makeText(MyApplication.getInstance(), msg, Toast.LENGTH_SHORT).show();
    }

    public static void showToast(int resourceId) {
        Toast.makeText(MyApplication.getInstance(), resourceId, Toast.LENGTH_SHORT).show();
    }

    /**
     * Location service if enable
     *
     * @param context
     * @return location is enable if return true, otherwise disable.
     */
    public static boolean isLocationEnable(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean networkProvider = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        boolean gpsProvider = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (networkProvider || gpsProvider) return true;
        return false;
    }

    /**
     * 随机获取正负1
     *
     * @param offset
     * @return
     */
    public static float getRandom(float offset) {
        int a = (int) (Math.random() * 2 + 1);
        int aa = (int) (Math.pow(-1, a));
        return offset * aa;
    }
}
