package com.amemusic.mymusic.thd_http;

import android.os.Environment;
import java.io.File;

/**
 * Created by klentz on 11/7/15.
 */
public class ext_fs {

    static public void pass_writable() throws ext_fs_exception_t{
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            throw new ext_fs_exception_t("External file storage not mounted for write, possibly SD card missing");
        }
    }

    /* Checks if external storage is available to at least read */
    static void pass_readable() throws ext_fs_exception_t{
        String state = Environment.getExternalStorageState();
        if (!(Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state))) {
            throw new ext_fs_exception_t("External file storage not mounted for read, possibly SD card missing");
        }
    }

    static public File get_thd_dir(String thd_dir) throws ext_fs_exception_t{
        // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MUSIC), thd_dir);
        if (!file.isDirectory() && !file.mkdirs()) {
            if(!file.isDirectory()){
                throw new ext_fs_exception_t(String.format("Failed to create %s %s", Environment.DIRECTORY_MUSIC, thd_dir));
            }
        }

        return file;
    }

    static public void pass_free_space(File file, int size) throws ext_fs_exception_t{
        if(file.getFreeSpace() < 2.5 * size){
            throw new ext_fs_exception_t(String.format("Insufficient disc space for %s", file.getName()));
        }
    }
}
