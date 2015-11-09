package com.amemusic.mymusic;

/**
 * Created by klentz on 11/7/15.
 */
import java.io.File;
import java.io.IOException;
        import java.net.MalformedURLException;
        import java.net.URL;

class music_getter {

    private String base_url_;
    file_getter file_getter_;

    music_getter(String codec, String user, String password){
        file_getter_ = new file_getter(codec, user, password);
        base_url_ = "http://www.tophitsdirect.com/download-engine/fetch?codec=" + codec.toLowerCase();
    }

    public void call(int music_id, File dest_dir, String local_file) throws IOException, parse_exception_t, http_exception_t{
        URL url = new URL(String.format("%s&music-id=%d", base_url_, music_id));
        file_getter_.call(url, dest_dir, local_file);
    }

    public void call(int music_id, File local_file) throws IOException, parse_exception_t, http_exception_t{
        URL url = new URL(String.format("%s&music-id=%d", base_url_, music_id));
        file_getter_.call(url, local_file);
    }

    public void call(int music_id, String local_file) throws IOException, parse_exception_t, http_exception_t{
        this.call(music_id, new File(local_file));
    }

    public void call(media_t media) throws IOException, parse_exception_t, http_exception_t, ext_fs_exception_t{
        ext_fs.pass_writable();
        ext_fs.pass_readable();
        File dest_dir = ext_fs.get_thd_dir(String.format("THD/%s", media.get_disc()));
         call(media.get_music_id(), dest_dir, media.get_file_name());
    }

    public static void main(String[] args) {
        try{
            music_getter my_getter = new music_getter("alac", "TH_KLentz2", "tillman");
            my_getter.call(1208557, "temp.m4p");
        }
        catch(Exception e){
            System.out.println("caught exception:" + e.toString());
        }
    }
}
