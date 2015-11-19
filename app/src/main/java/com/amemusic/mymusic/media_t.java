package com.amemusic.mymusic;

import java.util.Date;
import java.util.Hashtable;

/**
 * Created by klentz on 10/29/15.
 */
public class media_t extends Object{

    enum states_t {PENDING, QUEUED, DOWNLOADING, DOWNLOADED, MAX_DOWNLOADS, NOT_ACTIVATED, PAYMENT_MISSING}

    private auth_block_t auth_block_;

    private int music_id_;
    private String disc_;
    private Date impact_dts_;
    private String title_;
    private String artist_;
    private String edit_;
    private String warning_;
    private Date process_dts_;
    private int credits_used_;
    private Date downloaded_dts_ = null;
    private int total_credits_;
    private String media_type_;
    private states_t queue_state_ = states_t.PENDING;

    private Hashtable<String, Object> data_;

    private Hashtable<String, String> exts_;

    static private String codec_ = "mp3";
    static final public String THD_URL ="http://tophitsdirect.com";
    static final public String MAIN_URL =THD_URL + "/1.0.12.0";
    static final public String DOWNLOAD_URL = THD_URL + "/download-engine";
    static final public String RUN_URL = THD_URL + "/run";

    private String unc_fix(String value){

        final String replc[][] =  {{"\\",""}, {"/", ""}, {"\"", "'"}, {"?", "[Q]"}, {"$", "S"}, {":", "[colon]"}, {"*", "-"}};
        String ret = value;

        for(int idx = 0; idx < replc.length; ++idx) {
            ret = ret.replace(replc[idx][0], replc[idx][1]);
        }

        return ret;
    }

    public media_t(auth_block_t auth_block){
        auth_block_ = auth_block;

        data_ = new Hashtable<String, Object>();
        exts_ = new Hashtable<String, String>();

        exts_.put("mp3", "mp3");
        exts_.put("alac", "m4p");
        exts_.put("video", "mp4");
    }

    public int get_music_id(){return music_id_;}

    public void set_music_id(int value){music_id_ = value;}

    public String get_media_type(){return media_type_;}
    public void set_media_type(String value){media_type_ = value;}

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

    public Date get_process_date(){return process_dts_;}

    public void set_process_date(Date process_dts){process_dts_ = process_dts;}

    public void set_credits_used(int value){
         credits_used_ = value;}

    public void set_total_credits(int value){total_credits_ = value;}

    public states_t get_download(){
        states_t ret;

        if(auth_block_.is_payment_missing(this)){
            ret = states_t.PAYMENT_MISSING;
        }
        else if(queue_state_ != states_t.PENDING){
            ret = queue_state_;
        }
        else if(credits_used_ >= total_credits_)
        {
            ret = states_t.MAX_DOWNLOADS;
        }
        else if( credits_used_ == 0 )
        {
            ret = states_t.PENDING;
        }
        else
        {
            ret = states_t.DOWNLOADED;
        }

        return ret;
    }

    class can_t {

        private boolean can_p_;
        private String reason_;

        can_t(){
            can_p_ = true;
        }

        can_t(String reason){
            can_p_ = false;
            reason_ = reason;
        }

        boolean can(){
            return can_p_;
        }

        String reason(){
            return reason_;
        }
    }

    public can_t can_download(){
        can_t ret;

        switch(get_download()){
            case QUEUED:
                ret = new can_t("Media already queued");
                break;
            case DOWNLOADING:
                ret = new can_t("Media already downloading");
                break;
            case MAX_DOWNLOADS:
                ret = new can_t("Media already downloaded maximum allowable of 2 times");
                break;
            case PAYMENT_MISSING:
                ret = new can_t(String.format("%s. Payment needed to download", process_dts_));
                break;
            default:
                ret = new can_t();
                break;
        }

        return ret;
    }

    public can_t can_cancel() {
        return (queue_state_ != states_t.PENDING) ? new can_t() : new can_t("Media never selected for download");
    }

    public void flag_queued(){
        queue_state_  = states_t.QUEUED;
    }

    public void flag_downloading(){
         queue_state_  = states_t.DOWNLOADING;
    }

    public void flag_downloaded(int credits_used, Date downloaded_dts){
        queue_state_ = states_t.PENDING;
        credits_used_ = credits_used;
        downloaded_dts_ = downloaded_dts;
    }

    public void flag_cancelled(){
        queue_state_ = states_t.PENDING;
    }

    public String get_file_name(){
        String edit_str = edit_.length() > 0 ? String.format(" (%s)", unc_fix(edit_)): "";
        String warning_str = warning_.length() > 0 ? " (Warning Content)" : "";
        return String.format("%s - %s%s%s.%s", unc_fix(title_), unc_fix(artist_), edit_str, warning_str, exts_.get(codec_));
    }

    public String get_working_file_name(){
        return String.format("%s.wrk", get_file_name());
    }


    public String get_tag_file_name(){
        return String.format("%s.tag", get_file_name());
    }

    static public String get_codec(){
        return codec_;
    }

    static public void set_codec(String value){
        codec_ = value;
    }

    public Object get_data(String key){
        return data_.get(key);
    }

    public void put_data(String key, Object value) {
        data_.put(key,  value);
    }
}
