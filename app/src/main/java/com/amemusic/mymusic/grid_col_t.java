package com.amemusic.mymusic;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;

import com.amemusic.mymusic.R;

import java.text.SimpleDateFormat;

/**
 * Created by klentz on 10/30/15.
 */
public class grid_col_t {

    public enum types_t{STRING, INT, FLOAT, DATE, NULL};

    protected String key_;
    private String header_;
    private int width_;
    types_t type_;
    protected int text_color_;
    protected int typeface_ = Typeface.NORMAL;

    grid_col_t(Context context, String key, String header, int width, types_t type)
    {
        key_ = key;
        header_ = header;
        width_ = width;
        type_ = type;

        text_color_ = ContextCompat.getColor(context, R.color.GRID_TEXT_COLOR);
    }

    public String get_key(){
        return key_;
    }

    public String get_header(){
        return header_;
    }

    public int get_width(){
        return width_;
    }

    public void set_width(int width){
        width_ = width;
    }

    public types_t get_type(){
        return type_;
    }

    public String string(media_t media){
        return media.get_data(key_).toString();
    }

    public int get_text_color(media_t media){
        return text_color_;
    }

    public int get_typeface(media_t media) {return typeface_;}
}

