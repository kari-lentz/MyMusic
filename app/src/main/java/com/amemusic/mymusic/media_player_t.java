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

import java.io.IOException;
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

        MediaCodec codec_ = null;
        ByteBuffer [] input_buffers_;
        AudioTrack audio_track_ = null;

        Exception e_;
        boolean done_p_ = false;
        final long TIME_OUT_ = 10000;

        play_task_t(MediaFormat format, ring_buffer_t<media_frame_t> buffer) {
            format_ = format;
            buffer_ = buffer;
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
            int input_buffer_idx = codec_.dequeueInputBuffer(TIME_OUT_);
            if(input_buffer_idx >= 0) {
                frame.copy_buffer(input_buffers_[input_buffer_idx]);

                codec_.queueInputBuffer(input_buffer_idx,
                        0 /* offset */,
                        frame.get_num_samples(),
                        frame.get_presentation_ts(),
                        frame.is_eos() ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);

                //Log.d(tag_, String.format("Queued idx: %d samples:%d", input_buffer_idx, frame.get_num_samples()));

                ++ret;
            }

             return ret;
        }

        void call() throws IOException, InterruptedException, exec_cancelled, eof_t{

            String mime = format_.getString(MediaFormat.KEY_MIME);
            Log.d(tag_, String.format("started play for mime: %s", mime));

            int duration_ms = Long.valueOf(format_.getLong(MediaFormat.KEY_DURATION) / 1000).intValue();
            publishProgress(new begin_play_progress_t(duration_ms));

            // the actual decoder
            codec_ = MediaCodec.createDecoderByType(mime);
            codec_.configure(format_, null /* surface */, null /* crypto */, 0 /* flags */);
            codec_.start();
            input_buffers_ = codec_.getInputBuffers();
            ByteBuffer [] output_buffers = codec_.getOutputBuffers();

            int play_ms = 0;

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

            for(;;) {

                buffer_.read(this);

                int res = codec_.dequeueOutputBuffer(info, TIME_OUT_);
                if (res >= 0) {

                    int output_buffer_idx = res;
                    ByteBuffer buf = output_buffers[output_buffer_idx];

                    final byte[] chunk = new byte[info.size];
                    buf.get(chunk);
                    buf.clear();
                    if (chunk.length > 0) {
                        audio_track_.write(chunk, 0, chunk.length);
                    }

                    play_ms = Long.valueOf(audio_track_.getPlaybackHeadPosition()).intValue() * 10 / 441;

                    codec_.releaseOutputBuffer(output_buffer_idx, false /* render */);
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
            }

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

    class comm_task_t extends AsyncTask<Void, comm_progress_t, Void> implements ring_buffer_t.writer_i<media_frame_t>{

        String url_;
        MediaExtractor extractor_ = null;
        int duration_ms_ = 0;
        int max_buffer_size_;

        String tag_ = "media_player_t.comm_task_t";

        ring_buffer_t<media_frame_t> buffer_;
        private ring_buffer_t.writer_i<media_frame_t> writer_;

        String creds_ = null;
        Exception e_ = null;

        media_frame_t last_frame_ = null;
        boolean done_p_ = false;

        comm_task_t (String url, ring_buffer_t<media_frame_t> buffer){

            url_ = url;
            extractor_ = new MediaExtractor();

            buffer_ = buffer;
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

        public play_task_t play() throws Exception{

            String url_real = creds_ != null ? String.format("%s&creds=%s", url_, creds_) : url_;
            Log.i(tag_, String.format("Connecting %s", url_real));
            extractor_.setDataSource(url_real);

            MediaFormat format = extractor_.getTrackFormat(0);

            if(format == null){
                throw new Exception(String.format("No track format for:%s", url_real));
            }

            String mime =  format.getString(MediaFormat.KEY_MIME);

            if(mime.equals("audio/mpeg")){
                max_buffer_size_ = 1045;
            }
            else {
                throw new Exception(String.format("unknown mime: %s", mime));
            }

            duration_ms_ = Long.valueOf(format.getLong(MediaFormat.KEY_DURATION) / 1000).intValue();

            //select this track to feed into buffer
            extractor_.selectTrack(0);

            Log.d(tag_, String.format("selected track of duration %dms", duration_ms_));

            play_task_t ret = new play_task_t(format, buffer_);
            ret.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

            return ret;
        }

        @Override
        protected void onProgressUpdate(comm_progress_t ... progress){
            if(progress.length > 0) {
                progress[0].call();
            }
        }

        public int write(media_frame_t [] buffer, int offset, int length) throws IOException, eof_t{

            int ret = 0;

            for(int idx = 0; idx < length; ++idx) {

                media_frame_t frame = buffer[offset + idx];
                if(frame == null){
                    buffer[offset + idx] = new media_frame_t(max_buffer_size_);
                    frame = buffer[offset + idx];
                }

                frame.fill(extractor_);
                last_frame_ = frame;

                ++ret;

                if(frame.is_eos()) {
                    break;
                }
            }

            //Log.d(tag_, String.format("buffered %d samples", ret));

            return ret;
        }

        public void call() throws http_exception_t, IOException, exec_cancelled, InterruptedException, eof_t{

            done_p_ = false;
            Log.d(tag_, String.format("commenced buffering"));

            publishProgress(new begin_comm_progress_t(duration_ms_));

            do{
                buffer_.write(this);

                if(done_p_){
                    break;
                }

                if(isCancelled()) {
                    Log.d(tag_, "caught user cancel");
                    throw new exec_cancelled();
                }

                long presentation_ts = extractor_.getSampleTime();
                int buffered_ms = (Long.valueOf(presentation_ts).intValue() / 1000);
                //Log.d(tag_, String.format("Buffered=%d / Duration=%d", buffered_ms, duration_ms_));
                publishProgress(new comm_progress_t(buffered_ms, duration_ms_));
            }while((last_frame_ != null && !last_frame_.is_eos()));

            Log.d(tag_, "Done!");
        }

        private void clean_up(){
            extractor_.release();
        }

        @Override
        protected Void doInBackground(Void... values) {

            e_ = null;

            try {

                try {
                    call();
                }
                finally {
                    clean_up();
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
                e_.printStackTrace();
                Log.e(tag_, String.format("caught:%s", e_.toString()));
                if(error_notify_ != null) {
                    error_notify_.media_error_notify(e_.toString());
                }
            }

            comm_task_ = null;
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
    play_task_t play_task_ = null;
    String user_id_ = null;
    String password_ = null;

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

    public void play(media_t media) throws Exception{

        if(comm_task_ != null || play_task_ != null){
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
            comm_task_ = new comm_task_t(media.get_play_link(), new ring_buffer_t<media_frame_t>(this, 64,16)).authorization(user_id_, password_);
            play_task_ = comm_task_.play();
            comm_task_.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        else
        {
            throw new Exception("Currently must have Kitkat or higher to enjoy media play");
        }
    }

    public void release(){

        if(comm_task_ != null){
            if(!comm_task_.is_done()) {
                comm_task_.cancel(true);
            }

            comm_task_ = null;
        }

        if(play_task_ != null){
            if(!play_task_.is_done()) {
                play_task_.cancel(true);
            }

            play_task_ = null;
        }

        reset();
    }
}
