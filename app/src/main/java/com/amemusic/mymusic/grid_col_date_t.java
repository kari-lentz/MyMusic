package com.amemusic.mymusic;

import android.content.Context;

import java.text.SimpleDateFormat;

/**
 * Created by klentz on 10/31/15.
 */

public class grid_col_date_t extends grid_col_t{

    private String default_format_ = "MM/dd/yyyy";

    grid_col_date_t(Context context, String key, String header, int width){
        super(context, key, header, width, types_t.DATE);
    }

    grid_col_date_t default_format(String value){
        default_format_ = value;
        return this;
    }

    @Override
    public String string(media_t media){
        return new SimpleDateFormat(default_format_).format(media.get_data(key_));
    }
}
