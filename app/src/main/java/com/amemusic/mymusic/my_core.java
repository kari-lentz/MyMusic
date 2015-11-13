package com.amemusic.mymusic;


import android.view.View;

/**
 * Created by klentz on 11/6/15.
 */
public class my_core {
    static public int BUFFER_SIZE = 65536;

    public static float px_to_dp(View v, float px){
         float dpi = v.getContext().getResources().getDisplayMetrics().densityDpi;
          return px / dpi * 160.0f;
    }

}
