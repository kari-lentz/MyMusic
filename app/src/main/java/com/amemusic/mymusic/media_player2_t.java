package com.amemusic.mymusic;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.AsyncTask;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by klentz on 11/22/15.
 */
public class media_player2_t extends LinearLayout {


    interface error_notify_i{
        void media_error_notify(String error);
    }

    class exec_cancelled extends Exception{
    }

    class play_task_t extends AsyncTask<Void, Void, Void> {

        String tag_ = "media_player_t.decoder_t";
        String url_;

        MediaCodec codec_ = null;
        AudioTrack audio_track_ = null;
        Exception e_;
        boolean done_p_ = false;

        play_task_t(String url) {
            url_ = url;
        }

        private void clean_up(Boolean release)
        {
            if(codec_ != null){
                if(release){
                    codec_.stop();
                    codec_.release();
                }
            }
            if(audio_track_ != null){
                audio_track_.flush();
                audio_track_.release();
            }
        }

        void main_loop() throws IOException, exec_cancelled{

            MediaExtractor extractor = new MediaExtractor();

            extractor.setDataSource(url_);

            MediaFormat format = extractor.getTrackFormat(0);
            String mime = format.getString(MediaFormat.KEY_MIME);

            // the actual decoder
            codec_ = MediaCodec.createDecoderByType(mime);
            codec_.configure(format, null /* surface */, null /* crypto */, 0 /* flags */);
            codec_.start();
            ByteBuffer [] input_buffers = codec_.getInputBuffers();
            ByteBuffer [] output_buffers = codec_.getOutputBuffers();

            // get the sample rate to configure AudioTrack
            int sample_rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);

            audio_track_ = new AudioTrack(AudioManager.STREAM_MUSIC,
                    sample_rate,
                    AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    AudioTrack.getMinBufferSize (
                            sample_rate,
                            AudioFormat.CHANNEL_OUT_STEREO,
                            AudioFormat.ENCODING_PCM_16BIT
                    ),
                    AudioTrack.MODE_STREAM);

            // start playing, we will feed you later
            audio_track_.play();
            extractor.selectTrack(0);

            // start decoding
            final long TIME_OUT = 10000;
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            boolean saw_input_eos_p = false;
            boolean saw_output_eos_p = false;
            int output_counter = 0;
            int output_counter_limit = 50;

            while (!saw_output_eos_p && output_counter < output_counter_limit) {
                Log.i(tag_, "loop ");
                output_counter++;

                if (!saw_input_eos_p) {
                    int input_idx = codec_.dequeueInputBuffer(TIME_OUT);
                    Log.d(tag_, " bufIndexCheck " + input_idx);
                    if (input_idx>= 0) {
                        ByteBuffer buf = input_buffers[input_idx];
                        int ret = extractor.readSampleData(buf, 0 /* offset */);

                        long presentation_ts = 0;

                        if (ret < 0) {
                            Log.d(tag_, "saw input EOS.");
                            saw_input_eos_p = true;
                            ret = 0;
                        }
                        else {
                            presentation_ts = extractor.getSampleTime();
                        }
                        // can throw illegal state exception (???)
                        codec_.queueInputBuffer(
                                input_idx,
                                0 /* offset */,
                                ret,
                                presentation_ts,
                                saw_input_eos_p ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);

                        if (!saw_input_eos_p) {
                            extractor.advance();
                        }
                    }
                    else {
                        Log.e(tag_, "inputBufIndex " + input_idx);
                    }
                }

                int res = codec_.dequeueOutputBuffer(info, TIME_OUT);
                if (res >= 0) {

                    Log.d(tag_, "got frame, size " + info.size + "/" + info.presentationTimeUs);
                    if (info.size > 0) {
                        output_counter = 0;
                    }

                    int output_idx = res;
                    ByteBuffer buf = output_buffers[output_idx];

                    final byte[] chunk = new byte[info.size];
                    buf.get(chunk);
                    buf.clear();
                    if(chunk.length > 0){
                        audio_track_.write(chunk, 0, chunk.length);
                    }

                    codec_.releaseOutputBuffer(output_idx, false /* render */);

                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        Log.d(tag_, "saw output EOS.");
                        saw_output_eos_p = true;
                    }
                } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    output_buffers = codec_.getOutputBuffers();
                    Log.d(tag_, "output buffers have changed.");
                } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat oformat = codec_.getOutputFormat();
                    Log.d(tag_, "output format has changed to " + oformat);
                }
                else {
                    Log.d(tag_, "dequeueOutputBuffer returned " + res);
                }

                if(isCancelled()){
                    Log.d(tag_, "caught user cancel");
                    throw new exec_cancelled();
                }
            }

            Log.d(tag_, "stopping...");
            done_p_ = true;
        }

        @Override
        protected Void doInBackground(Void... values) {

            e_ = null;

            try {

                try {
                    main_loop();
                }
                finally {
                    clean_up(true);
                }
            }
            catch(exec_cancelled e){
                e_ = e;
            }
            catch (Exception e) {
                e_ = e;
            }

            return null;
        }

        void handle_cancel(){
        }

        @Override
        protected void onCancelled(Void v) {
            Log.i(tag_, "handling cancel post-api 11 style");
            handle_cancel();
        }

        @Override
        protected void onCancelled() {
            Log.i(tag_, "handling cancel post-api 3 style");
            handle_cancel();
        }

        @Override
        protected void onPostExecute(Void v) {
            if (e_ != null) {
                Log.e(tag_, String.format("caught:%s", e_.toString()));
                if(error_notify_ != null) {
                    error_notify_.media_error_notify(e_.toString());
                }
            }
        }

        public boolean is_done(){
            return done_p_;
        }
    }

    error_notify_i error_notify_;

    TextView tv_title_artist_ = null;
    ProgressBar progress_play_ = null;
    TextView tv_play_position_ = null;
    TextView tv_play_duration_ = null;
    Button btn_play_ = null;
    Button btn_stop_ = null;

    play_task_t play_task_ = null;
    String creds_ = null;

    String tag_ = "media_player_t";

    public media_player2_t(Context context, AttributeSet attrs){
        super(context, attrs);

        this.setVisibility(isInEditMode() ? View.VISIBLE : View.INVISIBLE);
    }

    media_player2_t error_notify(error_notify_i error_notify){
        error_notify_ = error_notify;
        return this;
    }

    media_player2_t init()
    {
        tv_title_artist_ = (TextView) findViewById(R.id.txt_title_artist);
        progress_play_ = (ProgressBar) findViewById(R.id.progress_play);
        tv_play_position_ = (TextView) findViewById(R.id.txt_play_position);
        tv_play_duration_ = (TextView) findViewById(R.id.txt_play_duration);
        btn_play_ = (Button) findViewById(R.id.MY_PLAY);
        btn_stop_ = (Button) findViewById(R.id.MY_STOP);

        tv_title_artist_.setText("");

        progress_play_.setProgress(0);
        progress_play_.setSecondaryProgress(0);

        tv_play_duration_ .setText("00:00");
        tv_play_position_.setText("00:00");

        this.setVisibility(INVISIBLE);

        return this;
    }

    media_player2_t authorization(String user_id, String password){

        // encrypt Authdata
        byte[] toEncrypt = (String.format("%s:%s", user_id, password)).getBytes();
        creds_ = Base64.encodeToString(toEncrypt, Base64.DEFAULT);

        return this;
    }
    void update_control_states(){
    }

    static String format_ms(int ms){
        int s = ms / 1000;
        int m = s / 60;
        return String.format("%02d:%02d", m, s - m * 60);
    }

    public void play(media_t media) throws Exception{

        if(play_task_ != null){
            if(error_notify_ != null) {
                error_notify_.media_error_notify("Already playing media ... please stop first");
            }
            return;
        }

        this.setVisibility(View.VISIBLE);
        String edit = media.get_edit().isEmpty() ? "" : String.format(" (%s)", media.get_edit());
        tv_title_artist_.setText(String.format("%s - %s%s", media.get_title(), media.get_artist(), edit));
        update_control_states();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            String url = String.format("%s&creds=%s", media.get_play_link(), creds_);
            play_task_ = new play_task_t(url);
            play_task_.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        else
        {
            throw new Exception("Currently must have Kitkat or higher to enjoy media play");
        }
    }

    public void release(){
        if(play_task_ != null){
            if(!play_task_.is_done()) {
                play_task_.cancel(true);
            }
            play_task_ = null;
        }
    }
}
