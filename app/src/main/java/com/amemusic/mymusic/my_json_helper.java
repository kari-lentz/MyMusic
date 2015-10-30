package com.amemusic.mymusic;

import android.util.JsonReader;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by klentz on 10/30/15.
 */
public class my_json_helper {

    private JsonReader inner_;
    private String default_str_;
    private int default_int_;

    public my_json_helper(JsonReader reader) throws IOException{
        inner_ = reader;
        default_str_ = "";
        default_int_ = 0;
    }

    public my_json_helper default_str(String value) {
        default_str_ = value;
        return this;
    }

    public my_json_helper default_int(int value) {
        default_int_ = value;
        return this;
    }

    public String try_string() throws IOException{
        try {
            return inner_.nextString();
        } catch (IllegalStateException e) {
            inner_.skipValue();
            return default_str_;
        }
    }

    public int try_int() throws IOException{
        try {
            return inner_.nextInt();
        } catch (IllegalStateException e) {
            inner_.skipValue();
            return default_int_;
        }
    }

    public Date try_date(String date_format) throws IOException {

        SimpleDateFormat formatter = new SimpleDateFormat(date_format);
        Date ret;
        try {
            ret = formatter.parse(inner_.nextString());
        } catch (ParseException e) {
            inner_.skipValue();
            ret = new Date();
        }

        return ret;
    }

}
