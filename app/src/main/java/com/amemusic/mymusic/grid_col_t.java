package com.amemusic.mymusic;

/**
 * Created by klentz on 10/30/15.
 */
public class grid_col_t {

    public enum types_t{STRING, INT, FLOAT, DATE, NULL};

    private String key_;
    private String header_;
    private int width_;
    types_t type_;

    grid_col_t(String key, String header, int width, types_t type)
    {
        key_ = key;
        header_ = header;
        width_ = width;
        type_ = type;
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
}
