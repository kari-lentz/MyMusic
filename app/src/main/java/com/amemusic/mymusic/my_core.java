package com.amemusic.mymusic;


import android.view.View;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by klentz on 11/6/15.
 */
public class my_core {
    static public int BUFFER_SIZE = 2 * 1024;

    static private String months_ [] = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sept", "Oct", "Nov", "Dec"};

    static public String get_month_short(Date dts){
        Calendar cal = Calendar.getInstance();
        cal.setTime(dts);
        return months_[cal.get(Calendar.MONTH)];
    }

    static public float px_to_dp(View v, float px){
         float dpi = v.getContext().getResources().getDisplayMetrics().densityDpi;
          return px / dpi * 160.0f;
    }

    interface file_callback_i{
        void call(RandomAccessFile stream) throws IOException;
    }

    static void with_open_file(File file, file_callback_i callback, String mode) throws IOException{

        mode = (mode == null) ? "r" : mode;

        RandomAccessFile stream = new RandomAccessFile(file, mode);
        try {
            callback.call(stream);
        }
        finally{
            stream.close();
        }
    }

    static class merge_request_t{

        private File file_;
        private long seek_;

        merge_request_t(File file, long seek){
            file_ = file;
            seek_ = seek;
        }

        File file(){
            return file_;
        }

        long seek(){
            return seek_;
        }
    }

    static void merge_files(File dest_file, merge_request_t... merge_requests) throws IOException{

        final int BUFFER_SIZE = 65536;
        final byte[] buffer = new byte[BUFFER_SIZE];
        final merge_request_t [] merge_request_inner = merge_requests;

        with_open_file(

                dest_file,

                new file_callback_i() {
                    @Override
                    public void call(RandomAccessFile out_stream) throws IOException{

                        final RandomAccessFile out_stream_inner = out_stream;

                        for(int idx = 0; idx < merge_request_inner.length; ++idx){
                            final merge_request_t r = merge_request_inner[idx];

                            with_open_file(
                                    r.file(),

                                    new file_callback_i() {
                                        @Override
                                        public void call(RandomAccessFile in_stream) throws IOException{

                                            in_stream.seek(r.seek());

                                            for(int ret_bytes = in_stream.read(buffer, 0, BUFFER_SIZE); ret_bytes != -1; ret_bytes = in_stream.read(buffer, 0, BUFFER_SIZE)) {
                                                out_stream_inner.write(buffer, 0, ret_bytes);
                                            }
                                        }
                                    },
                                    "r"
                            );
                        }
                    }
                },
                "rw"
        );
    }
}
