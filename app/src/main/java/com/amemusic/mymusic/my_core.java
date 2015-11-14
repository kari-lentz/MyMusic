package com.amemusic.mymusic;


import android.view.View;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by klentz on 11/6/15.
 */
public class my_core {
    static public int BUFFER_SIZE = 65536;

    static private String months_ [] = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sept", "Oct", "Nov", "Dec"};

    static public String get_month_short(Date dts){
        Calendar cal = Calendar.getInstance();
        cal.setTime(dts);
        return months_[cal.get(Calendar.MONTH)];
    }

    static public float px_to_dp(View v, float px){
         float dpi = v.getContext().getResources().getDisplayMetrics().densityDpi;
          return px / dpi * 160.0f;
    }

}
