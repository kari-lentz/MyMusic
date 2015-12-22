package com.amemusic.mymusic;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by klentz on 10/30/15.
 */
public class my_json_helper {

    private JSONObject inner_;
    private String default_str_;
    private int default_int_;
    private double default_double_;

    public my_json_helper(JSONObject inner){
        inner_ = inner;
        default_str_ = "";
        default_int_ = 0;
        default_double_ = 0.0;
    }

    public my_json_helper default_str(String value) {
        default_str_ = value;
        return this;
    }

    public my_json_helper default_int(int value) {
        default_int_ = value;
        return this;
    }

    public my_json_helper default_double(double value) {
        default_double_ = value;
        return this;
    }

    public String try_string(String key){
        try {
            if(!inner_.isNull(key)) {
                return inner_.getString(key);
            }
            else {
                return default_str_;
            }
        }
        catch(JSONException e){
            return default_str_;
        }
    }

    public int try_int(String key){
        try {
            return inner_.getInt(key);
        }
        catch(JSONException e){
            return default_int_;
        }
    }

    public double try_double(String key){
        try {
            return inner_.getDouble(key);
        }
        catch(JSONException e){
            return default_double_;
        }
    }

    public Date try_date(String key, String date_format)  {

        SimpleDateFormat formatter = new SimpleDateFormat(date_format);
        Date ret;
        try {
            try {
                ret = formatter.parse(inner_.getString(key));
            }
            catch(ParseException e){
                ret = new Date();
            }
        }
        catch(JSONException e){
            ret = new Date();
        }

        return ret;
    }
}
