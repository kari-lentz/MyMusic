package com.amemusic.mymusic;

/**
 * Created by klentz on 11/7/15.
 */
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

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
    final byte[] write_buffer_ = new byte[my_core.BUFFER_SIZE];

    private StringBuilder temp_writer_ = new StringBuilder();;

    enum SPECIAL_CHARS{EOL(10);
        private int value;
        SPECIAL_CHARS(int value){
            this.value = value;
        }
    };

    public file_getter(String user_id, String password){
        user_id_ = user_id;
        password_ = password;

        temp_writer_.ensureCapacity(1024);
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
                temp_writer_.append((char) c);
            }
            else {
                line = temp_writer_.toString();
                temp_writer_.setLength(0);
                break;
            }
        }

        if(line == null){
            throw new parse_exception_t("premature end of line:");
        }

        final String parts[] = line.split(":");

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
        if(total_ == 0) {
            total_ = total_remaining;
        }

        verify_field(in_stream, "payload");

        int remaining = size;
        while(remaining > 0){
            int ret_bytes = in_stream.read(write_buffer_, 0, remaining > my_core.BUFFER_SIZE ? my_core.BUFFER_SIZE : remaining);

            if(ret_bytes == -1){
                break;
            }

            out_stream.write(write_buffer_, 0, ret_bytes);
            remaining -= ret_bytes;
        }

        if(progress_ != null){
            progress_.do_progress_record(total_ - total_remaining + size, total_);
        }
    }


    private void run_seek(RandomAccessFile out_stream, int offset) throws IOException {
        out_stream.seek(offset);
    }

    public File call(URL url, File temp_file, File local_file) throws parse_exception_t, http_exception_t, IOException, MalformedURLException, exec_cancelled{

        boolean success_p = false;
        total_ = 0;

        Authenticator.setDefault(new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user_id_, password_.toCharArray());
            }
        });

         RandomAccessFile out_stream = new RandomAccessFile(temp_file, "rw");

        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            try {
                connection.setRequestMethod("GET");
                connection.connect();

                int code = connection.getResponseCode();

                if (!(code == 200 || (code >= 300 && code <= 304))) {
                    throw new http_exception_t(code, connection.getResponseMessage());
                }

                BufferedInputStream in_stream = new BufferedInputStream(connection.getInputStream());
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
                success_p = true;
             } finally {
                connection.disconnect();
            }
        }finally{
            out_stream.close();

            if(success_p) {
                // commit file
                temp_file.renameTo(local_file);
            }

            temp_file.delete();
        }

        return local_file;
    }

    public File call(URL url, File local_file) throws parse_exception_t, http_exception_t, IOException, MalformedURLException, exec_cancelled {
        return call(url, new File(String.format("%s.temp", local_file.getAbsolutePath())), local_file);
    }

        static class media_file_getter extends file_getter {

        protected File temp_file_dir_;

        media_file_getter(File temp_file_dir, String user, String password){
            super(user, password);
            temp_file_dir_ = temp_file_dir;
        }
    }

    static class working_file_getter extends media_file_getter{

        private String base_url_;

        working_file_getter(File temp_file_dir, String codec, String user, String password){
            super(temp_file_dir, user, password);
            base_url_ = String.format("%s/fetch?codec=%s", media_t.DOWNLOAD_URL, codec.toLowerCase());
        }

        public File call(media_t media) throws IOException, parse_exception_t, http_exception_t, ext_fs_exception_t, exec_cancelled{
            ext_fs.pass_writable();
            ext_fs.pass_readable();
            URL url = new URL(String.format("%s&music-id=%d", base_url_, media.get_music_id()));
            return super.call(url, new File(temp_file_dir_, media.get_working_file_name()));
        }

        public working_file_getter progress(progress_i progress){
            super.progress(progress);
            return this;
        }

        public working_file_getter canceller(canceller_i canceller){
            super.canceller(canceller);
            return this;
        }
    }

    static class tag_file_getter extends media_file_getter {

        private String base_url_;

        tag_file_getter(File temp_file_dir, String user, String password) {
            super(temp_file_dir, user, password);
            base_url_ = String.format("%s/tag-id3.py", media_t.RUN_URL);
        }

        public File call(media_t media, String disc) throws IOException, parse_exception_t, http_exception_t, ext_fs_exception_t, exec_cancelled {
            ext_fs.pass_writable();
            ext_fs.pass_readable();

            URL url = new URL(String.format("%s?music-id=%d&disc=%s", base_url_, media.get_music_id(), disc));
            return super.call(url, new File(temp_file_dir_, media.get_tag_file_name()));
        }
    }

    static class music_getter{

        working_file_getter working_file_getter_;
        tag_file_getter tag_file_getter_;
        File temp_file_dir_;

        music_getter(File temp_file_dir, String codec, String user, String password){

            temp_file_dir_ = temp_file_dir;

            working_file_getter_ = new working_file_getter(temp_file_dir, codec, user, password);
            tag_file_getter_ = new tag_file_getter(temp_file_dir, user, password);
        }

        public music_getter progress(progress_i progress){
            working_file_getter_.progress(progress);
            return this;
        }

        public music_getter canceller(canceller_i canceller){
            working_file_getter_.canceller(canceller);
            return this;
        }

        class box_t{
            private int v_;

            box_t(int v){
                v_ = v;
            }

            int get(){
                return v_;
            }
            void set(int v){v_ = v;}
        }

        void assemble_file(File working_file, File tag_file, File dest_file) throws IOException{
            final box_t  orig_id3_size = new box_t(0);

            my_core.with_open_file(
                    working_file,
                    new my_core.file_callback_i() {
                        @Override
                        public void call(RandomAccessFile in_stream) throws IOException {
                            if (in_stream.readUnsignedByte() == 'I' && in_stream.readUnsignedByte() == 'D' && in_stream.readUnsignedByte() == '3') {
                                if (in_stream.readUnsignedByte() == 3) {
                                    in_stream.skipBytes(4);
                                    orig_id3_size.set(in_stream.readUnsignedByte() * 0x80 + in_stream.readUnsignedByte() + 10);
                                }
                            }
                        }
                    },
                    "r");

            my_core.merge_files(dest_file, new my_core.merge_request_t(tag_file, 0), new my_core.merge_request_t(working_file, orig_id3_size.get()));
        }

        public void call(media_t media) throws IOException, parse_exception_t, http_exception_t, ext_fs_exception_t, exec_cancelled {

            String[] discs = media.get_disc().split("[\\s\\\\]+");

            File working_file = working_file_getter_.call(media);

            try {
                for(int idx = 0; idx < discs.length; ++idx) {

                    String disc = discs[idx];

                    File tag_file = tag_file_getter_.call(media, disc);
                    try {

                        assemble_file(
                                working_file,
                                tag_file,
                                new File(ext_fs.get_thd_dir(String.format("THD/%s", disc)), media.get_file_name()));
                    }
                    finally {
                        tag_file.delete();
                    }
                }
            }
            finally {
                working_file.delete();
            }
        }
    }

}
