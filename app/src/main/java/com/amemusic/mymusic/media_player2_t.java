package com.amemusic.mymusic;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;

/**
 * Created by klentz on 11/22/15.
 */
public class media_player2_t extends LinearLayout {


    interface error_notify_i{
        void media_error_notify(String error);
    }

    class decoder_t implements ring_buffer_t.reader_i{

        MediaCodec codec_ = null;
        ring_buffer_t comm_buffer_ = null;
        AudioTrack audio_track_;

        decoder_t(ring_buffer_t comm_buffer){
            comm_buffer_ = comm_buffer;
            audio_track_ = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, 65536, AudioTrack.MODE_STREAM);
        }

        public int read(byte[] buffer, int offset, int length) throws IOException{

            int num_bytes;

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

                int idx_input = codec_.dequeueInputBuffer(-1);
                ByteBuffer input_buffer = codec_.getInputBuffers()[idx_input];
                int remaining = input_buffer.remaining();
                num_bytes = remaining > length ? length : remaining;
                input_buffer.put(buffer, offset, num_bytes);
                codec_.queueInputBuffer(idx_input, 0, 0, 0, 0);
            }
            else {
                num_bytes = 0;
            }

            return num_bytes;
        }

        public void call() throws IOException, InterruptedException{
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

                codec_ = MediaCodec.createDecoderByType("audio/mpeg");
                MediaCodec.BufferInfo buffer_info = new MediaCodec.BufferInfo();

                codec_.start();
                try {
                    ByteBuffer[] output_buffers = codec_.getOutputBuffers();

                    for (;;) {

                        int idx_input = codec_.dequeueInputBuffer(-1);
                        if (idx_input >= 0) {
                            // fill inputBuffers[inputBufferId] with valid data
                            comm_buffer_.read(this);
                        }

                        int idx_output = codec_.dequeueOutputBuffer(buffer_info, -1);
                        if (idx_output >= 0) {
                            // outputBuffers[outputBufferId] is ready to be processed or rendered.
                            ByteBuffer output_buffer = output_buffers[idx_output];
                            audio_track_.write(output_buffer, 0, output_buffer.remaining(), AudioTrack.WRITE_BLOCKING);
                            codec_.releaseOutputBuffer(idx_output, false);
                        } else if (idx_output == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                            output_buffers = codec_.getOutputBuffers();
                        } else if (idx_output == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                            // Subsequent data will conform to new format.
                            MediaFormat format = codec_.getOutputFormat();
                        }
                    }
                }
                finally {
                    codec_.stop();
                    codec_.release();
                    codec_ = null;
                }
            }
        }
    }

    error_notify_i error_notify_;

    TextView tv_title_artist_ = null;
    ProgressBar progress_play_ = null;
    TextView tv_play_position_ = null;
    TextView tv_play_duration_ = null;
    Button btn_play_ = null;
    Button btn_stop_ = null;

    stream_getter_t stream_getter_ = null;
    decoder_t decoder_ = null;

    String tag_ = "media_player_t";

    public media_player2_t(Context context, AttributeSet attrs){
        super(context, attrs);

        ring_buffer_t comm_buffer = new ring_buffer_t(4, 65536);
        ring_buffer_t pcm_buffer = new ring_buffer_t(4, 65536);

        stream_getter_  = new stream_getter_t(comm_buffer);
        decoder_ = new decoder_t(comm_buffer);

        this.setVisibility(isInEditMode() ? View.VISIBLE : View.INVISIBLE);
    }

    media_player2_t authorization(String user_id, String password){

        stream_getter_.authorization(user_id, password);
        return this;
    }

    media_player2_t error_notify(error_notify_i error_notify){
        error_notify_ = error_notify;
        return this;
    }

    media_player2_t init(ring_buffer_t decode_buffer) throws IOException
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

    void update_control_states(){
    }

    static String format_ms(int ms){
        int s = ms / 1000;
        int m = s / 60;
        return String.format("%02d:%02d", m, s - m * 60);
    }

    public void play(media_t media) throws Exception{

        this.setVisibility(View.VISIBLE);
        String edit = media.get_edit().isEmpty() ? "" : String.format(" (%s)", media.get_edit());
        tv_title_artist_.setText(String.format("%s - %s%s", media.get_title(), media.get_artist(), edit));
        update_control_states();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            stream_getter_.call(new URL(media.get_play_link()));
            decoder_.call();
        }
        else
        {
            throw new Exception("Currently must have Kitkat or higher to enjoy media play");
        }
    }
}
