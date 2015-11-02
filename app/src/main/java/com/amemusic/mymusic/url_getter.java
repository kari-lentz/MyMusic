package com.amemusic.mymusic;

import android.content.Context;
import android.os.AsyncTask;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by klentz on 10/29/15.
 */
public class url_getter extends AsyncTask<URL, Integer, ArrayList<media_t>> {

    private Context context_;
    private ListView lv_;
    private TextView tv_status_;
    private View header_;
    private grid_cols_t grid_cols_;
    Exception e_;

    public url_getter(Context context, ListView lv, TextView tv_status, View header, grid_cols_t grid_cols ){
        super();

        context_ = context;
        lv_ = lv;
        tv_status_ = tv_status;
        header_ = header;
        grid_cols_ = grid_cols;

        e_ = null;
    }

    private ArrayList<media_t> fetch_media(URL url) throws MalformedURLException, IOException {

        final int BUFFER_SIZE=2048;
        char buffer []= new char[BUFFER_SIZE];

        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

        try {
            return (new my_song_reader(grid_cols_)).call(urlConnection.getInputStream());
        }
        finally {
            urlConnection.disconnect();
        }
    }

    @Override
    protected ArrayList<media_t> doInBackground(URL ... urls){

        ArrayList<media_t> ret;

        try {
            ret=fetch_media(urls[0]);
            e_ = null;
        }catch(Exception e) {
            ret = new ArrayList<media_t>();
            e_ = e;
        }

        return ret;
    }

    @Override
    protected void onPostExecute(ArrayList<media_t> media_list){

        if(e_ == null) {

            ViewGroup.LayoutParams params = header_.getLayoutParams();
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            header_.setLayoutParams(params);

            media_adapter_t adapter = new media_adapter_t(context_, grid_cols_, media_list);
            tv_status_.setText("Ready");
            lv_.setAdapter(adapter);
        }else {
            tv_status_.setText("Server Error");
            Toast.makeText(context_,
                    e_.toString(), Toast.LENGTH_LONG)
                    .show();
        }
    }

}
