package com.amemusic.mymusic;

import android.content.Context;
import android.graphics.Typeface;

import java.util.Calendar;
import java.util.Hashtable;

/**
 * Created by klentz on 11/12/15.
 */
public class grid_col_download_t extends grid_col_t {

    auth_block_t auth_block_;
    Hashtable<media_t.states_t, String> states_ = new Hashtable<media_t.states_t, String>();
    Calendar cal_ = Calendar.getInstance();
    String months_ [] = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sept", "Oct", "Nov", "Dec"};

    grid_col_download_t(Context context, auth_block_t auth_block, int width){
        super(context, "DOWNLOAD", "Download", width, grid_col_t.types_t.STRING);
        auth_block_ = auth_block;
        states_.put(media_t.states_t.PENDING, "");
        states_.put(media_t.states_t.DOWNLOADING, "Downloading");
        states_.put(media_t.states_t.DOWNLOADED, "Downloaded");
        states_.put(media_t.states_t.MAX_DOWNLOADS, "Max Allowed");
        states_.put(media_t.states_t.PAYMENT_MISSING, "Payment Missing");
    }

    @Override
    public String string(media_t media){
        String ret;

        media_t.states_t state = media.get_download();
        if(state == media_t.states_t.PAYMENT_MISSING){
            cal_.setTime(media.get_process_date());
            ret = String.format("%s. Payment Needed", months_[cal_.get(Calendar.MONTH)]);
        }
        else {
            ret = states_.get(state);
        }

        return ret != null ? ret : "Unknown";
    }

    @Override
    public int get_typeface(media_t media){
        return media.get_download() == media_t.states_t.PAYMENT_MISSING ? Typeface.ITALIC : Typeface.NORMAL;
    }
}
