package com.amemusic.mymusic;

/**
 * Created by klentz on 11/7/15.
 */
import java.lang.Exception;

class http_exception_t extends Exception{

    private int status_code_;
    private String msg_;

    http_exception_t(int status_code, String msg){
        super();
        status_code_ = status_code;
        msg_ = msg;
    }

    @Override
    public String toString(){
        return String.format("%s status_code=%s:%s", super.toString(), status_code_, msg_);
    }

};
