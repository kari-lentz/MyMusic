package com.amemusic.mymusic;

/**
 * Created by klentz on 11/7/15.
 */
import java.lang.Exception;

class http_exception_t extends Exception{

    private int status_code_;

    http_exception_t(int status_code, String msg){
        super(msg + ":" + "status_code=" + Integer.toString(status_code));
        status_code_ = status_code;
    }

    int get_status_code(){
        return status_code_;
    }
};
