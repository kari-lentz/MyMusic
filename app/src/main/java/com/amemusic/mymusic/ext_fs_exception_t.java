package com.amemusic.mymusic;

/**
 * Created by klentz on 11/7/15.
 */
import java.lang.Exception;

class ext_fs_exception_t extends Exception{

    private int status_code_;

    ext_fs_exception_t(String msg){
        super(msg);
    }
};
