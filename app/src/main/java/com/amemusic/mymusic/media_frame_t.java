package com.amemusic.mymusic;

import android.media.MediaExtractor;
import android.util.Log;

import java.nio.ByteBuffer;

/**
 * Created by klentz on 12/10/15.
 */
public class media_frame_t {

    boolean eof_p_ = false;
    ByteBuffer buffer_;

    media_frame_t(){
        buffer_ = null;
    }

    void fill(mp3_iter_t iter) throws eof_t{
        eof_p_ = false;

        int frame_size = iter.get_frame_size();

        if(buffer_ == null){
            buffer_ = ByteBuffer.allocate(frame_size);
        }

        if(buffer_.capacity() < frame_size){
            buffer_ = ByteBuffer.allocate(frame_size);
        }
        buffer_.position(0);
        buffer_.limit(frame_size);
        buffer_.put(iter.get_frame_bytes(), 0, frame_size);
    }

    void copy_buffer(ByteBuffer dest_buffer){
        dest_buffer.position(0);
        buffer_.position(0);
        dest_buffer.put(buffer_);
        dest_buffer.position(0);
        buffer_.position(0);
    }

    boolean is_eof(){
        return eof_p_;
    }

    void set_eof(){
        eof_p_ = true;
    }

    int get_size(){
        return buffer_.limit();
    }

    void dump(){
        buffer_.position(0);
        byte [] temp = new byte[get_size()];
        buffer_.get(temp, 0, get_size());
        mp3_iter_t iter = new mp3_iter_t(temp);
        Log.d("media-frame", String.format("FRAME-DUMP:%s", iter.dump()));
    }
}
