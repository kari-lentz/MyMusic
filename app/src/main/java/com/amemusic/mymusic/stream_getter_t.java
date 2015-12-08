package com.amemusic.mymusic;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;

/**
 * Created by klentz on 12/4/15.
 */
public class stream_getter_t implements ring_buffer_t.writer_i{

    class exec_cancelled extends Exception{
    }

    interface progress_i {
        void do_progress_record(int progress, int total);
    }

    interface canceller_i {
        boolean is_cancelled();
    }

    private String user_id_ = null;
    private String password_ = null;
    private ring_buffer_t.writer_i writer_;
    progress_i progress_ = null;
    canceller_i canceller_ = null;
    private int total_ = 0;
    ring_buffer_t buffer_;
    BufferedInputStream in_stream_ = null;

    enum SPECIAL_CHARS{EOL(10);
        private int value;
        SPECIAL_CHARS(int value){
                this.value = value;
            }
    };

    public stream_getter_t (ring_buffer_t buffer){
        buffer_ = buffer;
        writer_ = this;
    }

    public stream_getter_t authorization(String user_id, String password){
        user_id_ = user_id;
        password_ = password;

        return this;
    }

    stream_getter_t  progress(progress_i progress){
        progress_ = progress;
        return this;
    }

    stream_getter_t  canceller(canceller_i canceller){
        canceller_ = canceller;
        return this;
    }

    public int write(byte [] buffer, int offset, int frames_per_period) throws IOException{
        return in_stream_.read(buffer, offset, frames_per_period);
    }

    public void call(URL url) throws http_exception_t, IOException, exec_cancelled, InterruptedException{

        total_ = 0;

        if(user_id_ != null && password_ != null) {
            Authenticator.setDefault(new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user_id_, password_.toCharArray());
                }
            });
        }

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            connection.setRequestMethod("GET");
            connection.connect();

            int code = connection.getResponseCode();

            if (!(code == 200 || (code >= 300 && code <= 304))) {
                throw new http_exception_t(code, connection.getResponseMessage());
            }

            in_stream_ = new BufferedInputStream(connection.getInputStream());

            int ret_bytes = -1;
            do {
                ret_bytes = buffer_.write(writer_);

                if(progress_ != null){
                    progress_.do_progress_record(0, total_);
                }

                if (canceller_ != null && canceller_.is_cancelled()) {
                    throw new exec_cancelled();
                }

            } while(ret_bytes == -1);
        }
        finally {
            connection.disconnect();
        }
    }
}
