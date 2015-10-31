package com.amemusic.mymusic;

import java.text.SimpleDateFormat;

/**
 * Created by klentz on 10/31/15.
 */

public class grid_col_date_t extends grid_col_t{

    grid_col_date_t(String key, String header, int width){
        super(key, header, width, types_t.DATE);
    }

    @Override
    public String string(Object value){
        return new SimpleDateFormat("MM/dd/yyyy").format(value);
    }
}
