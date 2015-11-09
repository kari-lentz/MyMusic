package com.amemusic.mymusic;

import java.util.Date;
import java.util.Hashtable;

/**
 * Created by klentz on 10/29/15.
 */
public class media_t extends Object{

    private int music_id_;
    private int download_;
    private String disc_;
    private Date impact_dts_;
    private String title_;
    private String artist_;
    private String edit_;
    private String warning_;

    private Hashtable<String, Object> data_;

    private Hashtable<String, String> exts_;

    static private String codec_ = "alac";

    private String unc_fix(String value){

        final String replc[][] =  {{"\\",""}, {"/", ""}, {"\"", "'"}, {"?", "[Q]"}, {"$", "S"}, {":", "[colon]"}, {"*", "-"}};
        String ret = value;

        for(int idx = 0; idx < replc.length; ++idx) {
            ret = ret.replace(replc[idx][0], replc[idx][1]);
        }

        return ret;
    }

    public media_t(){
        download_ = 0;
        data_ = new Hashtable<String, Object>();
        exts_ = new Hashtable<String, String>();

        exts_.put("mp3", "mp3");
        exts_.put("alac", "m4p");
        exts_.put("video", "mp4");
    }

    public int get_music_id(){
        return music_id_;
    }

    public void set_music_id(int value){
        music_id_ = value;
    }

    public int get_download(){
        return download_;
    }

    public void set_download(int value){
        download_ = value;
    }

    public String get_disc(){
        return disc_;
    }

    public void set_disc(String value){
        disc_ = value;
    }

    public Date get_impact_dts(){
        return impact_dts_;
    }

    public void set_impact_dts(Date value){
        impact_dts_ = value;
    }

    public String get_title(){
        return title_;
    }

    public void set_title(String value){
        title_ = value;
    }

    public String get_artist(){
        return artist_;
    }

    public void set_artist(String value){
        artist_ = value;
    }

    public String get_edit(){
        return edit_;
    }

    public void set_edit(String value){
        edit_ = value;
    }

    public String get_warning(){
        return warning_;
    }

    public void set_warning(String value){
        warning_ = value;
    }

    public String get_file_name(){
        String edit_str = edit_.length() > 0 ? String.format(" (%s)", unc_fix(edit_)): "";
        String warning_str = warning_.length() > 0 ? " (Warning Content)" : "";
        return String.format("%s - %s%s%s.%s", unc_fix(title_), unc_fix(artist_), edit_str, warning_str, exts_.get(codec_));
    }

    public Object get_data(String key){
        return data_.get(key);
    }

    public void put_data(String key, Object value) {
        data_.put(key,  value);
    }
}
