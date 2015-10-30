package com.amemusic.mymusic;

import java.util.Date;

/**
 * Created by klentz on 10/29/15.
 */
public class media_t extends Object{

    private int music_id_;
    private int download_;
    private Date impact_dts_;
    private String title_;
    private String artist_;
    private String edit_;

    public media_t(){
        download_ = 0;
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
}
