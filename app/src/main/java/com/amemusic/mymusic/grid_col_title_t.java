package com.amemusic.mymusic;

import android.content.Context;
import android.support.v4.content.ContextCompat;

import com.amemusic.mymusic.R;

/**
 * Created by klentz on 11/9/15.
 */
public class grid_col_title_t extends grid_col_t{

    int content_warning_color_;

    grid_col_title_t(Context context, int width){
        super(context, "TITLE", "Title", width, grid_col_t.types_t.STRING);
        content_warning_color_ = ContextCompat.getColor(context, R.color.GRID_CONTENT_WARNING_TEXT_COLOR);
    }

    @Override
    public String string(media_t media){
        String warning = media.get_warning();
        String content_warning_str = warning.length() > 0 ? String.format("%s - ", warning) : "";
        return String.format("%s%s", content_warning_str, super.string(media));
    }

    @Override
    public int get_text_color(media_t media){
        return media.get_warning().length() > 0 ? content_warning_color_: text_color_;
    }
}
