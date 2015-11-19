package com.amemusic.mymusic;

/**
 * Created by klentz on 11/7/15.
 */
import java.io.File;
import java.io.IOException;
import java.net.URL;

class music_getter extends file_getter{

    private String base_url_;

    music_getter(String codec, String user, String password){
        super(user, password);
        base_url_ = String.format("%s/fetch?codec=%s", media_t.DOWNLOAD_URL, codec.toLowerCase());
    }

    public void call(int music_id, File dest_dir, String local_file) throws IOException, parse_exception_t, http_exception_t, file_getter.exec_cancelled{
        URL url = new URL(String.format("%s&music-id=%d", base_url_, music_id));
        super.call(url, dest_dir, local_file);
    }

    public void call(media_t media) throws IOException, parse_exception_t, http_exception_t, ext_fs_exception_t, file_getter.exec_cancelled{
        ext_fs.pass_writable();
        ext_fs.pass_readable();
        File dest_dir = ext_fs.get_thd_dir(String.format("THD/temp"));
        call(media.get_music_id(), dest_dir, media.get_file_name());
    }

    public music_getter progress(file_getter.progress_i progress){
        super.progress(progress);
        return this;
    }

    public music_getter canceller(file_getter.canceller_i canceller){
        super.canceller(canceller);
        return this;
    }
}
