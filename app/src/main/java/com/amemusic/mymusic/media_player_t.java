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
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

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

    String creds_ = "";

    TextView tv_title_artist_ = null;
    ProgressBar progress_play_ = null;
    TextView tv_play_position_ = null;
    TextView tv_play_duration_ = null;
    Button btn_play_ = null;
    Button btn_stop_ = null;
    String tag_ = "media_player_t";

    public media_player_t(Context context, AttributeSet attrs){
        super(context, attrs);

        player_ = new MediaPlayer();
        player_.setAudioStreamType(AudioManager.STREAM_MUSIC);

        ht_errors_.put(MediaPlayer.MEDIA_ERROR_UNKNOWN, "Unknown");
        ht_errors_.put(MediaPlayer.MEDIA_ERROR_SERVER_DIED, "Server Died");
        ht_errors_.put(MediaPlayer.MEDIA_ERROR_IO, "IO");
        ht_errors_.put(MediaPlayer.MEDIA_ERROR_MALFORMED, "Malformed");
        ht_errors_.put(MediaPlayer.MEDIA_ERROR_UNSUPPORTED, "Unsupported");
        ht_errors_.put(MediaPlayer.MEDIA_ERROR_TIMED_OUT, "Timed Out");
        ht_errors_.put(-2147483648, "System");

        this.setVisibility(isInEditMode() ? View.VISIBLE : View.INVISIBLE);
    }

    media_player_t authorization(String user_id, String password){

        // encrypt Authdata
        byte[] toEncrypt = (String.format("%s:%s", user_id, password)).getBytes();
        creds_ = Base64.encodeToString(toEncrypt, Base64.DEFAULT);

        return this;
    }

    media_player_t error_notify(error_notify_i error_notify){
        error_notify_ = error_notify;
        return this;
    }

    public void reset(){

        player_.stop();
        player_.reset();

        tv_title_artist_ = (TextView) findViewById(R.id.txt_title_artist);
        progress_play_ = (ProgressBar) findViewById(R.id.progress_play);
        tv_play_position_ = (TextView) findViewById(R.id.txt_play_position);
        tv_play_duration_ = (TextView) findViewById(R.id.txt_play_duration);
        btn_play_ = (Button) findViewById(R.id.MY_PLAY);
        btn_stop_ = (Button) findViewById(R.id.MY_STOP);

        progress_play_.setProgress(0);
        progress_play_.setSecondaryProgress(0);

        tv_play_duration_ .setText("00:00");
        tv_play_position_.setText("00:00");

        this.setVisibility(INVISIBLE);
    }

    media_player_t init()
    {
        reset();

        player_.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                progress_play_.setProgress(percent);
            }
        });

        player_.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                try {
                    mp.start();
                } catch (Exception e) {
                    Log.e(tag_, e.toString());
                }

                tv_play_duration_.setText(format_ms(player_.getDuration()));
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
                if (error_notify_ != null) {
                    error_notify_.media_error_notify(String.format("Media play error %s", descr));
                }

                return false;
            }
        });

        player_.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                progress_play_.setSecondaryProgress(100);
            }
        });

        return this;
    }

    void update_control_states(){
        btn_play_.setEnabled(!player_.isPlaying());
        btn_stop_.setEnabled(player_.isPlaying());
    }

    static String format_ms(int ms){
        int s = ms / 1000;
        int m = s / 60;
        return String.format("%02d:%02d", m, s - m * 60);
    }

    public void play(media_t media) throws Exception{

        String url = String.format("%s&creds=%s", media.get_play_link(), creds_);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            player_.setDataSource(url);
        }
        else
        {
            throw new Exception("Currently must have Ice Cream Sandwich or higher to enjoy media play");
        }

        Log.i(tag_, String.format("Begin playing %s", media.get_file_name()));


        this.setVisibility(View.VISIBLE);
        String edit = media.get_edit().isEmpty() ? "" : String.format(" (%s)", media.get_edit());
        tv_title_artist_.setText(String.format("%s - %s%s", media.get_title(), media.get_artist(), edit));
        update_control_states();

        player_.prepareAsync();
    }

    public void release(){

        if(player_ != null) {
            player_.release();
            player_ = null;
        }
    }
}
