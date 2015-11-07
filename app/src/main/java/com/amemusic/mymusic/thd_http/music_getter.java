package com.amemusic.mymusic.thd_http;

/**
 * Created by klentz on 11/7/15.
 */
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

    public void call(int music_id, String local_file) throws IOException, MalformedURLException, parse_exception_t, http_exception_t{
        URL url = new URL(base_url_ + "&music-id=" + Integer.toString(music_id));
        file_getter_.call(url, local_file);
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
