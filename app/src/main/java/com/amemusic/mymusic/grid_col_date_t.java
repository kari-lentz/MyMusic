package com.amemusic.mymusic;

import android.content.Context;

import java.text.SimpleDateFormat;

/**
 * Created by klentz on 10/31/15.
 */

public class grid_col_date_t extends grid_col_t{

    grid_col_date_t(Context context, String key, String header, int width){
        super(context, key, header, width, types_t.DATE);
    }

    @Override
    public String string(media_t media){
        return new SimpleDateFormat("MM/dd/yyyy").format(media.get_data(key_));
    }
}
