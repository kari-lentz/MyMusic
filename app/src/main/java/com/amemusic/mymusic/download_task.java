package com.amemusic.mymusic;

/**
 * Created by klentz on 11/9/15.
 */
import android.content.Context;
import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by klentz on 11/8/15.
 */
public class download_task extends AsyncTask<Void, media_t, Integer> {

    Context context_;
    TextView tv_status_;
    ConcurrentLinkedQueue<media_t> queue_;

    music_getter music_getter_;
    Exception e_ = null;
    int ctr_ = 0;
    String tag_ = "download_task";

    download_task(Context context, TextView tv_status, ConcurrentLinkedQueue<media_t> queue, String codec, String user_id, String password){
        super();

        context_ = context;
        tv_status_ = tv_status;
        queue_ = queue;
        music_getter_ = new music_getter(codec, user_id, password);
    }

    @Override
    protected void onProgressUpdate(media_t ... media_list){
        if(media_list.length > 0) {
            media_t media = media_list[0];
            tv_status_.setText(String.format("Downloading %s", media.get_file_name()));
        }
    }

    @Override
    protected Integer doInBackground(Void ... params){
         media_t media;

        try{
            while((media = queue_.poll()) != null){
                Log.i(tag_, String.format("downloading %s", media.get_file_name()));
                publishProgress(media);
                music_getter_.call(media);
                Log.i(tag_, String.format("downloaded %s", media.get_file_name()));
                ctr_++;
            }
        }
        catch(Exception e){
            e_ = e;
        }

        return ctr_;
    }

    @Override
    protected void onPostExecute(Integer num_files){
        if(e_ == null){
            tv_status_.setText(String.format("Finished downloading %d files", num_files));
        }
        else{
            Log.e(tag_, e_.toString());
            tv_status_.setText("Server Error");
            Snackbar.make(tv_status_, e_.toString(), Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();


        }
    }
}
