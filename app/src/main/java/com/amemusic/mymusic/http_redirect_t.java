package com.amemusic.mymusic;

/**
 * Created by klentz on 11/16/15.
 */
public class http_redirect_t extends http_exception_t{

    private String alt_url_;

    http_redirect_t(int status_code, String alt_url, String msg){
        super(status_code, msg);
        alt_url_ = alt_url;
    }

    public String get_alt_url(){
        return alt_url_;
    }
}
