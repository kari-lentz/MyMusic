package com.amemusic.mymusic;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.AsyncTask;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;

/**
 * Created by klentz on 11/22/15.
 */
public class media_player_t extends LinearLayout implements ring_buffer_t.factory_i<media_frame_t>{

    interface error_notify_i{
        void media_error_notify(String error);
    }

    class exec_cancelled extends Exception{
    }

    static String format_ms(int ms){
        int s = ms / 1000;
        int m = s / 60;
        return String.format("%02d:%02d", m, s - m * 60);
    }

    class play_progress_t{
        int played_;
        int duration_;

        play_progress_t(int played, int duration){
            played_ = played;
            duration_ = duration;
        }

        void call(){
            if(duration_ > 0){
                progress_play_.setSecondaryProgress(played_ * 100 / duration_);
                tv_play_position_.setText(format_ms(played_));
            }
        }
    }

    class begin_play_progress_t extends play_progress_t{
        begin_play_progress_t(int duration){
            super(0, duration);
        }

        @Override
        void call(){
            super.call();
            tv_play_duration_.setText(format_ms(duration_));
        }
    }

    class play_task_t extends AsyncTask<Void, play_progress_t, Void> implements ring_buffer_t.reader_i<media_frame_t> {

        String tag_ = "media_player_t.play_task_t";

        MediaFormat format_;
        ring_buffer_t<media_frame_t> buffer_;
        int seek_;

        MediaCodec codec_ = null;
        ByteBuffer [] input_buffers_;
        AudioTrack audio_track_ = null;

        boolean threshold_p_ = false;

        Exception e_;
        boolean done_p_ = false;
        final long TIME_OUT_ = 10000;

        play_task_t(MediaFormat format, ring_buffer_t<media_frame_t> buffer, int seek) {
            format_ = format;
            buffer_ = buffer;
            seek_ = seek;
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

        @Override
        protected void onProgressUpdate(play_progress_t ... progress){
            if(progress.length > 0) {
                progress[0].call();
            }
        }

        public int read(media_frame_t [] buffer, int offset, int length) throws IOException, eof_t {

            int ret = 0;

            media_frame_t frame = buffer[offset];

            /*
            if(!threshold_p_)
            {
                if (buffer_.get_samples_available() < buffer_.get_frames_per_period()) {
                    threshold_p_ = true;
                    return 0;
                }
            }*/

            int input_buffer_idx = codec_.dequeueInputBuffer(TIME_OUT_);
            if(input_buffer_idx >= 0) {
                frame.copy_buffer(input_buffers_[input_buffer_idx]);

                codec_.queueInputBuffer(input_buffer_idx,
                        0, //offset
                        frame.get_size(),
                        0,
                        frame.is_eof() ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);

                if(frame.is_eof()){
                    Log.d(tag_,"SAW MUSIC EOF");
                }

                //Log.d(tag_, String.format("Queued idx:%d samples:%d:eof:%b", input_buffer_idx, frame.get_size(), frame.is_eof()));

                ++ret;
            }

             return ret;
        }

        void call() throws IOException, InterruptedException, exec_cancelled, eof_t{

            String mime = format_.getString(MediaFormat.KEY_MIME);
            Log.d(tag_, String.format("started play for mime: %s", mime));

            int duration_bytes = 4 * Long.valueOf(format_.getLong(MediaFormat.KEY_DURATION) * 441 / 10000).intValue();
            int duration_ms = duration_bytes / 4 * 10 / 441;
            publishProgress(new begin_play_progress_t(duration_ms));

            // the actual decoder
            codec_ = MediaCodec.createDecoderByType(mime);
            codec_.configure(format_, null /* surface */, null /* crypto */, 0 /* flags */);
            codec_.start();
            input_buffers_ = codec_.getInputBuffers();
            ByteBuffer [] output_buffers = codec_.getOutputBuffers();

            // get the sample rate to configure AudioTrack
            int sample_rate = format_.getInteger(MediaFormat.KEY_SAMPLE_RATE);

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

            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            byte [] chunk = new byte[2048];
            threshold_p_ = false;

            int init_play_ms = seek_ * 1000;
            int play_ms = init_play_ms;
            int played_bytes = seek_ * 44100 * 4;

            Log.d(tag_, String.format("STARTING PLAY:%d/%d -> %d/%d", play_ms, duration_ms, played_bytes, duration_bytes));

            boolean done_p = false;
            do {

                buffer_.read(this);

                int res = codec_.dequeueOutputBuffer(info, TIME_OUT_);
                if (res >= 0) {

                    int output_buffer_idx = res;

                    ByteBuffer buf = output_buffers[output_buffer_idx];

                    if(info.size > chunk.length) {
                        chunk = new byte[info.size];
                    }
                    buf.get(chunk);
                    buf.clear();
                    if(info.size> 0) {
                        int remaining = (duration_bytes - played_bytes);
                        int size;

                        if(remaining > info.size){
                            size = info.size;
                        }
                        else {
                            done_p = true;
                            Log.d(tag_, String.format("SAW END:%d bytes", remaining));
                            size = remaining;
                        }

                        if(size > 0){
                            audio_track_.write(chunk, 0, size);
                            played_bytes += size;
                        }
                    }

                    play_ms = init_play_ms + Long.valueOf(audio_track_.getPlaybackHeadPosition()).intValue() * 10 / 441;

                    codec_.releaseOutputBuffer(output_buffer_idx, false);
                }
                else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
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

                publishProgress(new play_progress_t(play_ms, duration_ms));

                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d(tag_, "saw output EOS.");
                    break;
                }
            } while(!done_p);

            Log.d(tag_, "stopping...");
        }

        @Override
        protected Void doInBackground(Void... values) {

            e_ = null;

            try {

                try {
                    call();
                    done_p_ = true;
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
                e_.printStackTrace();
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

    class comm_progress_t{
        int buffered_;
        int duration_;

        comm_progress_t(int buffered, int duration){
            buffered_ = buffered;
            duration_ = duration;
        }

        void call(){
            if(duration_ > 0){
                progress_play_.setProgress(buffered_ * 100 / duration_);
            }
        }
    }

    class begin_comm_progress_t extends comm_progress_t{
        begin_comm_progress_t(int duration){
            super(0, duration);
        }

        @Override
        void call(){
            super.call();
            tv_play_duration_.setText(format_ms(duration_));
        }
    }

    class comm_task_t extends AsyncTask<Void, comm_progress_t, Void> implements mp3_iter_t.reader_i, ring_buffer_t.writer_i<media_frame_t>{

        String url_;

        HttpURLConnection connection_ = null;
        BufferedInputStream in_stream_ = null;
        mp3_iter_t mp3_iter_ = null;
        boolean eof_p_ = false;
        int downloaded_bytes_ = 0;
        int duration_ms_ = 0;
        int content_length_ = 0;
        int bit_rate_ = 0;

        String tag_ = "media_player_t.comm_task_t";

        ring_buffer_t<media_frame_t> buffer_;
        int seek_;

        private ring_buffer_t.writer_i<media_frame_t> writer_;
        play_task_t play_task_ = null;

        String creds_ = null;
        Exception e_ = null;

        boolean done_p_ = false;

        comm_task_t (String url, ring_buffer_t<media_frame_t> buffer, int seek){

            url_ = url;
            buffer_ = buffer;
            seek_ = seek;

            mp3_iter_ = new mp3_iter_t(this);
            writer_ = this;
        }

        public comm_task_t authorization(String user_id, String password){

            if(user_id != null && password != null) {
                // encrypt Authdata
                byte[] toEncrypt = (String.format("%s:%s", user_id, password)).getBytes();
                creds_ = Base64.encodeToString(toEncrypt, Base64.DEFAULT);
            }

            return this;
        }

        public int read(byte dest[], int offset, int length) throws IOException{
            int ret = 0;
            int remaining = length;
            int total_bytes = 0;
            do{
                ret = in_stream_.read(dest, offset + total_bytes, remaining);
                if(ret < 0){
                    return -1;
                }
                total_bytes += ret;
                remaining -= ret;
            }while(remaining > 0);

            return total_bytes;
        }

        public void wait_for_play(){

            if(!isCancelled()) {
                try {
                    play_task_.get();
                    play_task_ = null;
                } catch (Exception e) {
                    Log.e(tag_, String.format("waiting for player, caught:%s", e));
                }
            }
        }

        public void release(){

            if(in_stream_ != null){
                in_stream_ = null;
            }

            if(connection_ != null){
                connection_.disconnect();
                connection_ = null;
            }

            if(play_task_ != null){
                if(!play_task_.is_done()) {
                    play_task_.cancel(true);
                }

                play_task_ = null;
            }
        }

        @Override
        protected void onProgressUpdate(comm_progress_t ... progress){
            if(progress.length > 0) {
                progress[0].call();
            }
        }

        long get_presentation_ts(int num_bytes){
            return Double.valueOf(((double) num_bytes * 8) / ((double) bit_rate_) * 1000000.0).longValue();
        }

        int get_bytes(int secs){
            return Double.valueOf(((double) secs) * ((double) bit_rate_) / 8.0).intValue();
        }

        private play_task_t run_play() throws Exception{

            eof_p_ = false;

            String url_seek = String.format("%s&start-at=%d", url_, seek_);
            String url_real = creds_ != null ? String.format("%s&creds=%s", url_seek, creds_) : url_seek;
            Log.i(tag_, String.format("Connecting %s", url_real));

            URL url = new URL(url_real);
            connection_ = (HttpURLConnection) url.openConnection();

            connection_.setRequestMethod("GET");
            connection_.connect();

            int code = connection_.getResponseCode();

            if (!(code == 200 || (code >= 300 && code <= 304))) {
                throw new http_exception_t(code, connection_.getResponseMessage());
            }

            in_stream_ = new BufferedInputStream(connection_.getInputStream());

            mp3_iter_ = new mp3_iter_t(this);

            if(mp3_iter_.next() == null){
                throw new parse_exception_t("no mp3 frames");
            }

            bit_rate_ = mp3_iter_.get_bit_rate();
            int sample_rate = mp3_iter_.get_sample_rate();
            int num_channels = mp3_iter_.get_num_channels();
            content_length_ = connection_.getContentLength();
            long duration = seek_ * 1000000 + get_presentation_ts(content_length_);
            duration_ms_ = Long.valueOf(duration / 1000).intValue();
            int max_buffer_size = Double.valueOf(((double) bit_rate_) / ((double)sample_rate) * 144.0 + 0.5).intValue();
            Log.d(tag_, String.format("mp3 format specs:%d:%d:%d:%s", sample_rate, bit_rate_, num_channels, max_buffer_size));
            MediaFormat format = MediaFormat.createAudioFormat("audio/mpeg", sample_rate, num_channels);
            format.setLong(MediaFormat.KEY_DURATION, duration);

            downloaded_bytes_ = get_bytes(seek_);

            //Log.d(tag_, String.format("content-length:%d %s", content_length_, mp3_iter_.dump()));

            play_task_ = new play_task_t(format, buffer_, seek_);
            play_task_.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

            return play_task_;
        }

        public int write(media_frame_t [] buffer, int offset, int length) throws IOException, eof_t{

            int ret = 0;

            for(int idx = 0; !eof_p_ && idx < length; ++idx) {

                media_frame_t frame = buffer[offset + idx];
                if(frame == null){
                    buffer[offset + idx] = new media_frame_t();
                    frame = buffer[offset + idx];
                }

                frame.fill(mp3_iter_);

                //Log.d(tag_, mp3_iter_.dump());

                downloaded_bytes_ += frame.get_size();
                ++ret;

                try {
                    if(mp3_iter_.next() == null){
                        frame.set_eof();
                        eof_p_ = true;
                    }
                }
                catch(Exception e){
                    e_ = e;
                    frame.set_eof();
                    eof_p_ = true;
                }
            }

            //Log.d(tag_, String.format("buffered %d samples", ret));
            return ret;
        }

        public void call() throws Exception{

            done_p_ = false;
            Log.d(tag_, String.format("commenced buffering"));

            run_play();
            publishProgress(new begin_comm_progress_t(duration_ms_));

            do{
                buffer_.write(this);

                if(isCancelled()) {
                    Log.d(tag_, "caught user cancel");
                    throw new exec_cancelled();
                }

                publishProgress(new comm_progress_t(Long.valueOf(get_presentation_ts(downloaded_bytes_) / 1000).intValue(), duration_ms_));
            }while(!eof_p_);

            wait_for_play();
         }

        @Override
        protected Void doInBackground(Void... values) {

            e_ = null;

            try {

                try {
                    call();
                }
                finally {
                    release();
                }
            }
            catch(exec_cancelled e){
            }
            catch (Exception e) {
                e_ = e;
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            done_p_ = true;

            if (e_ != null) {
                Log.e(tag_, String.format("caught:%s", e_.toString()));
                if(error_notify_ != null) {
                    error_notify_.media_error_notify(e_.toString());
                }
                e_.printStackTrace();
            }
            else {
                Log.d(tag_, "Done Buffering!");
            }

            comm_task_ = null;
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

    comm_task_t comm_task_ = null;
    String user_id_ = null;
    String password_ = null;
    media_t last_media_ = null;

    String tag_ = "media_player_t";

    public media_player_t(Context context, AttributeSet attrs){
        super(context, attrs);

        this.setVisibility(isInEditMode() ? View.VISIBLE : View.INVISIBLE);
    }

    media_player_t error_notify(error_notify_i error_notify){
        error_notify_ = error_notify;
        return this;
    }

    void reset()
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

        progress_play_.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        release();
                        int seek = Float.valueOf(100 * event.getX() / v.getWidth()).intValue();
                        Log.d(tag_, String.format("TRY SEEK:%f/%d -> seek %d", event.getX(), v.getWidth(), seek));
                        try {
                            play(last_media_, seek);
                        } catch (Exception e) {
                            Log.d(tag_, String.format("seeking media, caught %s", e.toString()));
                        }
                        break;
                }

                return true;
            }
        });

        tv_play_duration_ .setText("00:00");
        tv_play_position_.setText("00:00");

        this.setVisibility(INVISIBLE);
    }

    media_player_t init()
    {
        reset();
        return this;
    }

    media_player_t authorization(String user_id, String password){

        user_id_ = user_id;
        password_ = password;

        return this;
    }

    void update_control_states(){
    }

    @Override
    public media_frame_t[] new_inst(int size){
        return new media_frame_t[size];
    }

    public void play(media_t media, int seek_percent) throws Exception{

        if(comm_task_ != null){
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
            int seek = media.get_run() * seek_percent / 100;
            comm_task_ = new comm_task_t(media.get_play_link(), new ring_buffer_t<media_frame_t>(this, 64,16), seek).authorization(user_id_, password_);
            comm_task_.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        else
        {
            throw new Exception("Currently must have Kitkat or higher to enjoy media play");
        }

        last_media_ = media;
    }

    public void release(){

        if(comm_task_ != null){

            if(!comm_task_.is_done()) {
                comm_task_.cancel(true);
            }

            comm_task_ = null;
        }

        reset();
    }
}
