package com.amemusic.mymusic;

/**
 * Created by klentz on 11/7/15.
 */
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.StringWriter;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;

public class file_getter {

    class exec_cancelled extends Exception{
    }

    interface progress_i {
        void do_progress_record(int progress, int total);
    }

    interface canceller_i {
        boolean is_cancelled();
    }

    private String user_id_;
    private String password_;
    progress_i progress_ = null;
    canceller_i canceller_ = null;
    private int total_ = 0;

    private StringWriter temp_writer_;

    enum SPECIAL_CHARS{EOL(10);
        private int value;
        SPECIAL_CHARS(int value){
            this.value = value;
        }
    };

    public file_getter(String codec, String user_id, String password){
        user_id_ = user_id;
        password_ = password;

        temp_writer_ = new StringWriter();
    }

    file_getter progress(progress_i progress){
        progress_ = progress;
        return this;
    }

    file_getter canceller(canceller_i canceller){
        canceller_ = canceller;
        return this;
    }

    private String verify_field(InputStream in_stream, String command) throws IOException, parse_exception_t{

        String line = null;

        for(int c = in_stream.read(); c != -1; c = in_stream.read()){
            if(c != SPECIAL_CHARS.EOL.value){
                temp_writer_.write(c);
            }
            else {
                line = temp_writer_.toString();
                temp_writer_.getBuffer().setLength(0);
                break;
            }
        }

        if(line == null){
            throw new parse_exception_t("premature end of line:");
        }

        String parts[] = line.split(":");

        if(parts.length == 1){
            return null;
        }

        if(parts.length!=2){
            throw new parse_exception_t("Invalid command string:" + line);
        }

        if(!parts[0].equals(command)){
            throw new parse_exception_t("Unexpected command string:" + parts[0] + ": expected:" + command);
        }

        return parts[1];
    }

    private void run_write(InputStream in_stream, RandomAccessFile out_stream, int size, int total_remaining) throws IOException, parse_exception_t
    {
        final int BUFFER_SIZE = 65536;
        byte[] buffer = new byte[BUFFER_SIZE];

        if(total_ == 0) {
            total_ = total_remaining;
        }

        verify_field(in_stream, "payload");

        int remaining = size;
        while(remaining > 0){
            int ret_bytes = in_stream.read(buffer, 0, remaining > BUFFER_SIZE ? BUFFER_SIZE : remaining);

            if(ret_bytes == -1){
                break;
            }

            out_stream.write(buffer, 0, ret_bytes);
            remaining -= ret_bytes;
        }

        if(progress_ != null){
            progress_.do_progress_record(total_ - total_remaining + size, total_);
        }
    }


    private void run_seek(RandomAccessFile out_stream, int offset) throws IOException {
        out_stream.seek(offset);
    }

    public void call(URL url, File local_file) throws parse_exception_t, http_exception_t, IOException, MalformedURLException, exec_cancelled{

        Authenticator.setDefault (new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user_id_, password_.toCharArray());
            }
        });

        RandomAccessFile out_stream = new RandomAccessFile(local_file, "rw");

        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            try {
                connection.setRequestMethod("GET");
                connection.connect();

                int code = connection.getResponseCode();

                if (!(code == 200 || (code >= 300 && code <= 304))) {
                    throw new http_exception_t(code, connection.getResponseMessage());
                }

                InputStream in_stream = connection.getInputStream();
                Boolean epilogue_p = false;

                while (!epilogue_p) {
                    switch (verify_field(in_stream, "request")) {
                        case "write":
                            run_write(in_stream, out_stream,
                                    Integer.parseInt(verify_field(in_stream, "size")),
                                    Integer.parseInt(verify_field(in_stream, "remaining")));
                            break;
                        case "seek":
                            run_seek(out_stream, Integer.parseInt(verify_field(in_stream, "offset")));
                            break;
                        case "epilogue":
                            epilogue_p = true;
                            break;
                    }

                    if (canceller_ != null && canceller_.is_cancelled()) {
                        throw new exec_cancelled();
                    }
                }
            } finally {
                connection.disconnect();
            }
        }finally{
            out_stream.close();
        }

    }

    public void call(URL url, File local_dir, String local_file) throws parse_exception_t, http_exception_t, IOException, MalformedURLException, exec_cancelled {
        call(url, new File(local_dir, local_file));
    }

    public void call(URL url, String local_file) throws parse_exception_t, http_exception_t, IOException, MalformedURLException, exec_cancelled {
        call(url, new File(local_file));
    }

}
