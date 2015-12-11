package com.amemusic.mymusic;

import android.media.MediaExtractor;

import java.nio.ByteBuffer;

/**
 * Created by klentz on 12/10/15.
 */
public class media_frame_t {

    long presentation_ts_;
    ByteBuffer buffer_;
    boolean eos_p_;

    media_frame_t(int max_buffer_size){
        presentation_ts_ = 0;
        buffer_ = ByteBuffer.allocate(max_buffer_size);
         eos_p_ = false;
    }

    void fill(MediaExtractor extractor) throws eof_t{
        eos_p_ = false;
        buffer_.clear();
        int ret = extractor.readSampleData(buffer_, 0 /* offset */);

        if(ret < 0) {
            buffer_.limit(0);
            eos_p_ = true;
        }
        else {
            buffer_.limit(ret);
        }

        presentation_ts_ = extractor.getSampleTime();

        if(extractor.advance()){
            eos_p_ = false;
        }
    }

    void copy_buffer(ByteBuffer dest_buffer){
        dest_buffer.put(buffer_.array(), 0, buffer_.limit());
    }

    int get_num_samples(){
        return buffer_.limit();
    }

    long get_presentation_ts(){
        return presentation_ts_;
    }

    boolean is_eos(){
        return eos_p_;
    }
}
