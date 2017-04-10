package com.adrian.bletempcontroller.utils;

import android.app.Activity;
import android.util.DisplayMetrics;

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
}
