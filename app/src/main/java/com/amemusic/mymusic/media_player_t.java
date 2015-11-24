package com.amemusic.mymusic;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 * Created by klentz on 11/22/15.
 */
public class media_player_t extends LinearLayout {


    interface error_notify_i{
        void media_error_notify(String error);
    }

    private MediaPlayer player_ = null;
    error_notify_i error_notify_;
    final Hashtable<Integer, String> ht_errors_ = new Hashtable();

    Map<String, String> headers_ = new HashMap<>();

    String tag_ = "media_player_t";

    public media_player_t(Context context, AttributeSet attrs){
        super(context, attrs);

        ht_errors_.put(MediaPlayer.MEDIA_ERROR_UNKNOWN, "Unknown");
        ht_errors_.put(MediaPlayer.MEDIA_ERROR_SERVER_DIED, "Server Died");
        ht_errors_.put(MediaPlayer.MEDIA_ERROR_IO, "IO");
        ht_errors_.put(MediaPlayer.MEDIA_ERROR_MALFORMED, "Malformed");
        ht_errors_.put(MediaPlayer.MEDIA_ERROR_UNSUPPORTED, "Unsupported");
        ht_errors_.put(MediaPlayer.MEDIA_ERROR_TIMED_OUT, "Timed Out");
        ht_errors_.put(-2147483648, "System");

        this.setVisibility(View.INVISIBLE);
    }

    media_player_t authorization(String user_id, String password){

        // encrypt Authdata
        byte[] toEncrypt = (String.format("%s:%s", user_id, password)).getBytes();
        String encoded = Base64.encodeToString(toEncrypt, Base64.DEFAULT);

        // create header
        headers_.put("Authorization", "Basic " + encoded);

        return this;
    }

    media_player_t error_notify(error_notify_i error_notify){
        error_notify_ = error_notify;
        return this;
    }

    public void play(media_t media) throws Exception{
        Uri uri = Uri.parse(media.get_play_link());

        if(player_ == null) {
            player_ = new MediaPlayer();
            player_.setAudioStreamType(AudioManager.STREAM_MUSIC);
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            player_.setDataSource(getContext(), uri, headers_);
        }
        else
        {
            throw new Exception("Currently must have Ice Cream Sandwich or higher to enjoy media play");
        }

        final media_t my_media = media;

        player_.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                try {
                    Log.i(tag_, String.format("Begin playing %s", my_media.get_file_name()));
                    mp.start();
                } catch (Exception e) {
                    Log.e(tag_, e.toString());
                }
            }
        });

        player_.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {

                String category = ht_errors_.get(what);

                if (category == null) {
                    category = "Unknown Category";
                }

                String info = ht_errors_.get(extra);

                if (info == null) {
                    info = "Unknown origin";
                }

                String descr = String.format("category: %s info: %s", category, info);

                Log.e(tag_, String.format("%s", descr));
                error_notify_.media_error_notify(String.format("Media play error %s", descr));

                return false;
            }
        });

        player_.prepareAsync();
        this.setVisibility(View.VISIBLE);
    }

    public void release(){
        this.setVisibility(View.INVISIBLE);

        if(player_ != null) {
            player_.release();
            player_ = null;
        }
    }
}
