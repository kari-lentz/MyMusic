package com.amemusic.mymusic;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MainActivity extends AppCompatActivity {

    Context context_;
    auth_block_t auth_block_;
    String password_;
    resize_context_t resize_context_ = null;
    ConcurrentLinkedQueue<media_t> media_queue_ = new ConcurrentLinkedQueue<>();
    download_task download_task_ = null;

    View header_;
    TextView tv_status_;
    ListView lv_;
    grid_cols_t grid_cols_;
    ProgressBar progress_bar_;
    TextView tv_percent_;
    String media_type_ = "MP3";
    Hashtable<Integer, media_t> ht_media_= new Hashtable<Integer, media_t>();;

    final Hashtable<Integer, String> ht_media_play_errors_ = new Hashtable();
    MediaPlayer media_player_ = null;

    final String tag_ = "MainActivity";

    private media_t fetch_selected(){
        media_t ret;

        media_adapter_t adapter = (media_adapter_t) lv_.getAdapter();

        if(adapter != null) {
            int position = adapter.get_selected_position();
            ret = (position != -1) ? (media_t) lv_.getItemAtPosition(position) : null;
        }
        else {
            ret = null;
        }

        return ret;
    }

    private void update_progress(media_t media, boolean transition_state_p, int progress){

        if(transition_state_p) {
            media_adapter_t adapter = (media_adapter_t) lv_.getAdapter();

            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }

             media_t.states_t state = media.get_download();
            if(state == media_t.states_t.DOWNLOADED || state == media_t.states_t.MAX_DOWNLOADS){
                tv_status_.setText(String.format("Downloaded %s", media.get_file_name()));
            }
            else if(state == media_t.states_t.DOWNLOADING){
                tv_status_.setText(String.format("Downloading %s", media.get_file_name()));
            }
        }

        if (progress >= 0) {
            tv_percent_.setText(String.format("%d%s", progress, "%"));
            progress_bar_.setProgress(progress);
        }
    }

    private void continue_download_tasks(boolean force_new_p){
        if (force_new_p || download_task_ == null || download_task_.getStatus() != AsyncTask.Status.RUNNING) {
            //start brand new download task if previous one finished
            download_task_ = new download_task(this, tv_status_, media_queue_, media_t.get_codec(), auth_block_.get_user_id(), password_);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                download_task_.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
            else {
                download_task_.execute();
            }
        }
    }

    private void load_media_play_errors() {

         Hashtable ht = ht_media_play_errors_;

        ht.put(MediaPlayer.MEDIA_ERROR_UNKNOWN, "Unknown");
        ht.put(MediaPlayer.MEDIA_ERROR_SERVER_DIED, "Server Died");
        ht.put(MediaPlayer.MEDIA_ERROR_IO, "IO");
        ht.put(MediaPlayer.MEDIA_ERROR_MALFORMED, "Malformed");
        ht.put(MediaPlayer.MEDIA_ERROR_UNSUPPORTED, "Unsupported");
        ht.put(MediaPlayer.MEDIA_ERROR_TIMED_OUT, "Timed Out");
        ht.put(-2147483648, "System");
    }

    private void play(){
        final media_t media = fetch_selected();

        if (media != null) {

            try {
                if (media_player_ == null) {
                    media_player_ = new MediaPlayer();
                    load_media_play_errors();
                }

                media_player_.setAudioStreamType(AudioManager.STREAM_MUSIC);

                // encrypt Authdata
                byte[] toEncrypt = (String.format("%s:%s", auth_block_.get_user_id(), password_)).getBytes();
                String encoded = Base64.encodeToString(toEncrypt, Base64.DEFAULT);

                // create header
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Basic " + encoded);

                Uri uri = Uri.parse(media.get_play_link());

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                    media_player_.setDataSource(this, uri, headers);
                }
                else
                {
                    throw new Exception("Currently must have Ice Cream Sandwich or higher to enjoy media play");
                }

                media_player_.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        try {
                            Log.i(tag_, String.format("Begin playing %s", media.get_file_name()));
                            mp.start();
                        } catch (Exception e) {
                            Log.e(tag_, e.toString());
                        }
                    }
                });

                media_player_.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                    @Override
                    public boolean onError(MediaPlayer mp, int what, int extra) {

                        String category = ht_media_play_errors_.get(what);

                        if(category == null) {
                            category = "Unknown Category";
                        }

                        String info = ht_media_play_errors_.get(extra);

                        if(info == null) {
                            info = "Unknown origin";
                        }

                        String descr = String.format("category: %s info: %s", category, info);

                        Log.e(tag_, String.format("%s", descr));
                        make_toast(String.format("Media play error %s", descr));

                        return false;
                    }
                });

                media_player_.prepareAsync();
            }
            catch (Exception e) {
                Log.e(tag_, e.toString());
                tv_status_.setText("Play Unsuccesful");
                make_toast(e.toString());
            }
        }
        else {
            make_toast("no media selected");
        }
    }

    private void start_download_tasks(){

        media_t media = fetch_selected();

        if(media != null) {

            media_t.can_t can = media.can_download();

            if(can.can()) {

                make_toast(String.format("queing %s for download", media.get_file_name()));
                media.flag_queued();
                media_queue_.add(media);
                update_progress(media, true, -1);

                continue_download_tasks(false);
            }
            else
            {
                make_toast(can.reason());
            }
        }
        else{
            make_toast("no media selected");
        }
    }

    private void cancel_download_task(){

        media_t media = fetch_selected();

        if (media != null) {

            media_t.can_t can = media.can_cancel();

            if(can.can()){

                if (media_queue_.remove(media)) {
                    media.flag_cancelled();
                    update_progress(media, true, -1);
                    make_toast(String.format("%s no longer on download queue", media.get_file_name()));
                }
                else if (download_task_ != null && download_task_.getStatus() == AsyncTask.Status.RUNNING) {
                    download_task_.cancel(true);
                 }
                else{
                    make_toast(String.format("%s not currently selected for download", media.get_file_name()));
                }
            }
            else {
                make_toast("nothing selected for cancel");
            }
        }
        else {
            make_toast("no tracks available");
        }
    }

    private void refresh(){
        TextView tv = (TextView) findViewById(R.id.txt_status);
        tv.setText("Loading ...");

        try {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                new grid_task().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new URL(String.format("%s/get-media.py?media_type=%s&disc_type=ALL&user_id=%s&json=t", media_t.MAIN_URL, media_type_, auth_block_.get_user_id())));
            }
            else{
                new grid_task().execute(new URL(String.format("%s/get-media.py?media_type=%s&disc_type=ALL&user_id=%s&json=t", media_t.MAIN_URL, media_type_, auth_block_.get_user_id())));
            }

        } catch (MalformedURLException e) {
            Log.e(tag_, e.toString());
            tv_status_.setText("Incomplete URL");
        }
    }

    private void make_toast(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context_ = this;
        auth_block_ = (auth_block_t) getIntent().getSerializableExtra(String.format("%s.auth_block", this.getPackageName()));
        password_ = getIntent().getStringExtra(String.format("%s.password", this.getPackageName()));

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tv_status_ = (TextView) findViewById(R.id.txt_status);
        progress_bar_ = (ProgressBar) findViewById(R.id.progress_download);
        tv_percent_ = (TextView) findViewById(R.id.txt_percent);
        lv_= (ListView) findViewById(R.id.lv_media);

        grid_cols_ = new grid_cols_t(new grid_col_t[]{
                new grid_col_download_t(this, auth_block_, 150),
                new grid_col_date_t(this, "DTS_RELEASED", "Impact Date", 80),
                new grid_col_title_t(this, 200),
                new grid_col_t(this, "ARTIST", "Artist", 200, grid_col_t.types_t.STRING),
                new grid_col_t(this, "EDIT", "Edit", 100, grid_col_t.types_t.STRING),
                new grid_col_t(this, "DISC", "Disc", 70, grid_col_t.types_t.STRING),
                new grid_col_t(this, "LABEL", "Label", 100, grid_col_t.types_t.STRING),
                new grid_col_t(this, "FORMAT", "Genre", 100, grid_col_t.types_t.STRING),
                new grid_col_t(this, "BPM", "BPM", 50, grid_col_t.types_t.INT),
                new grid_col_t(this, "INTRO", "Intro", 100, grid_col_t.types_t.INT),
                new grid_col_t(this, "RUN", "Run", 100, grid_col_t.types_t.INT),
                new grid_col_t(this, "CHART", "Chart", 100, grid_col_t.types_t.STRING)
        });

        header_= findViewById(R.id.lv_media_header);
        int header_color = ContextCompat.getColor(header_.getContext(), R.color.GRID_HEADER_BACKGROUND_COLOR);
        header_.setBackgroundColor(header_color);

        grid_cols_.rewind();

        while(grid_cols_.has_next()) {

            final grid_col_t col = grid_cols_.next();

            final View header_col = View.inflate(header_.getContext(), R.layout.lv_media_col, null);
            ((ViewGroup) header_).addView(header_col);
            TextView tv = (TextView) header_col.findViewById(R.id.lv_media_col);
            tv.setText(col.get_header());

            ViewGroup.LayoutParams params = header_col.getLayoutParams();
            params.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, col.get_width(), header_col.getResources().getDisplayMetrics());
            header_col.setLayoutParams(params);

            header_col.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            float xpos = my_core.px_to_dp(v, event.getRawX());
                            resize_context_ = new resize_context_t(lv_, v, col, xpos) ;
                            break;
                    }

                    return true;
                }
            });
        }

        final View my_view = findViewById(R.id.media_horizontal_scroller);
        my_view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (resize_context_ != null) {
                    float xpos = my_core.px_to_dp(v, event.getRawX());

                    switch (event.getActionMasked()) {

                        case MotionEvent.ACTION_MOVE:
                            resize_context_.call(xpos);
                            break;
                        case MotionEvent.ACTION_UP:
                            resize_context_.call(xpos);
                            resize_context_ = null;
                            break;
                        case MotionEvent.ACTION_OUTSIDE:
                            resize_context_.call(xpos);
                            resize_context_ = null;
                            break;
                        default:
                            return false;
                    }
                    return true;
                } else {
                    //re-enable scrolling
                    return v.onTouchEvent(event);
                }
            }
        });

        lv_.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                media_adapter_t adapter = ((media_adapter_t) lv_.getAdapter());
                if (adapter != null) {
                    adapter.update_selected_pos(view, position);
                }
            }
        });

        refresh();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean ret;

        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch(item.getItemId()){
            case R.id.action_settings:
                make_toast("Settings not Implemented");
                ret = true;
                break;
            case R.id.action_play:
                play();
                ret= true;
                break;
            case R.id.action_download:
                start_download_tasks();
                ret = true;
                break;
            case R.id.action_cancel:
                cancel_download_task();
                ret = true;
                break;
            case R.id.action_refresh:
                refresh();
                ret= true;
                break;
            default:
                ret= super.onOptionsItemSelected(item);
        }

        return ret;
    }

    interface media_factory_i{
        media_t media_factory(int music_id);
    }

    /**
     * Created by klentz on 10/29/15.
     */
    public class grid_task extends AsyncTask<URL, Integer, ArrayList<media_t>> implements media_factory_i{

        Exception e_;

        public grid_task(){
            super();

            e_ = null;
        }

        public media_t media_factory(int music_id){
            media_t ret;
            media_t media = ht_media_.get(music_id);

            if(media != null) {
                ret =  media;
            }
            else{
                ret =  new media_t(auth_block_);
                ht_media_.put(music_id, ret);
                ret.set_music_id(music_id);
            }

            return ret;
        }

        private ArrayList<media_t> fetch_media(URL url) throws  IOException, JSONException {

            final int BUFFER_SIZE=my_core.BUFFER_SIZE;
            char buffer []= new char[BUFFER_SIZE];

            Log.i(tag_, "fetching media");

            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            try {
                StringWriter writer = new StringWriter();
                try {
                    InputStream in = urlConnection.getInputStream();
                    InputStreamReader isr = new InputStreamReader(in, "latin1");
                    try {
                        for (int ret = isr.read(buffer, 0, BUFFER_SIZE); ret != -1; ret = isr.read(buffer, 0, BUFFER_SIZE)) {
                            writer.write(buffer, 0, ret);
                        }
                    } finally {
                        isr.close();
                    }
                    Log.i(tag_, "fetched media info");
                    return (new my_song_reader(auth_block_, grid_cols_, this)).call(writer.toString());
                }
                finally {
                    writer.getBuffer().setLength(0);
                }
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

                media_adapter_t adapter = (media_adapter_t) lv_.getAdapter();

                if(adapter != null) {
                    adapter.set_media_list(media_list);
                }
                else {
                    adapter = new media_adapter_t(context_, grid_cols_, media_list);
                    lv_.setAdapter(adapter);
                }

                tv_status_.setText("Ready");

            }
            else {
                tv_status_.setText("Server Error");

                Snackbar.make(tv_status_, e_.toString(), Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();

            }
        }
    }

    interface task_progress_i{
        void call();
    }

    public class download_task extends AsyncTask<Void, task_progress_i, Integer> implements file_getter.progress_i, file_getter.canceller_i {

        Context context_;
        TextView tv_status_;
        ConcurrentLinkedQueue<media_t> queue_;

        file_getter.music_getter music_getter_;
        Exception e_ = null;
        int ctr_ = 0;
        String tag_ = "download_task";
        media_t current_media_ = null;

        download_task(Context context, TextView tv_status, ConcurrentLinkedQueue<media_t> queue, String codec, String user_id, String password) {
            super();

            context_ = context;
            tv_status_ = tv_status;
            queue_ = queue;
            music_getter_ = new file_getter.music_getter(context.getFilesDir(), codec, user_id, password).progress(this).canceller(this);
        }

        @Override
        public boolean is_cancelled() {
            return this.isCancelled();
        }

        void handle_cancel() {
            if (current_media_ != null && current_media_.get_download() == media_t.states_t.DOWNLOADING) {
                current_media_.flag_cancelled();
                update_progress(current_media_, true, 0);
                make_toast(String.format("Download of %s cancelled in progress", current_media_.get_file_name()));
            }

            if (!media_queue_.isEmpty()) {
               continue_download_tasks(true);
            }
        }

        void record_download(media_t media) throws IOException, JSONException {
            final int BUFFER_SIZE = my_core.BUFFER_SIZE;
            char buffer[] = new char[BUFFER_SIZE];

            URL url = new URL(String.format("%s/music-downloaded.py?media_type=%s&music_id=%d&downloaded_discs=%s&user_id=%s&json=1",
                    media_t.MAIN_URL, media.get_media_type(), media.get_music_id(), Uri.encode(media.get_disc()), auth_block_.get_user_id()));

            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            try {
                StringWriter writer = new StringWriter();
                InputStream in = urlConnection.getInputStream();
                InputStreamReader isr = new InputStreamReader(in, "latin1");
                for (int ret = isr.read(buffer, 0, BUFFER_SIZE); ret != -1; ret = isr.read(buffer, 0, BUFFER_SIZE)) {
                    writer.write(buffer, 0, ret);
                }
                JSONObject reply = new JSONArray(writer.toString()).getJSONObject(0);
                final my_json_helper helper = new my_json_helper(reply);
                final media_t my_media = media;

                Log.i(tag_, String.format("downloaded %s", media.get_file_name()));
                publishProgress(new task_progress_i() {
                    @Override
                    public void call() {
                        my_media.flag_downloaded(helper.try_int("CREDITS_USED"), helper.try_date("DTS_DOWNLOADED", "yyyy-MM-dd hh:mm:ss"));
                        update_progress(my_media, true, 100);

                    }
                });

            } finally {
                urlConnection.disconnect();
            }
        }

        @Override
        protected void onProgressUpdate(task_progress_i... progress) {

            if (progress.length > 0) {
                progress[0].call();
            }
        }

        @Override
        public void do_progress_record(final int progress, final int total) {

             if (current_media_ != null) {

                final media_t media = current_media_;
                publishProgress(new task_progress_i() {
                    @Override
                    public void call() {

                        int percent = (progress >= 1024 && total >= 1024) ? (progress / 1024) * 100 / (total / 1024) : 0;
                        update_progress(media, false, percent);
                    }
                });
            }
        }

        @Override
        protected Integer doInBackground(Void... params) {
            media_t media;

            try {
                e_ = null;
                while ((media = queue_.poll()) != null) {
                    final String file_name = media.get_file_name();
                    final media_t my_media = media;

                    Log.i(tag_, String.format("downloading %s", file_name));
                    publishProgress(new task_progress_i() {
                        @Override
                        public void call() {
                            my_media.flag_downloading();
                            update_progress(my_media, true, 0);
                        }
                    });

                    current_media_ = media;
                    music_getter_.call(media);
                    record_download(media);

                    ctr_++;
                }
            } catch (file_getter.exec_cancelled e) {
                Log.i(tag_, "caught cancel");
                e_ = e;
            } catch (Exception e) {
                e_ = e;
            }

            return ctr_;
        }

        @Override
        protected void onCancelled(Integer result) {
            Log.i(tag_, "handling cancel post-api 11 style");
            handle_cancel();
        }

        @Override
        protected void onCancelled() {
            Log.i(tag_, "handling cancel post-api 3 style");
            handle_cancel();
        }

        @Override
        protected void onPostExecute(Integer num_files) {
            if (e_ == null) {
                tv_status_.setText(String.format("Finished downloading %d files", num_files));
            }
            else if(e_ instanceof file_getter.exec_cancelled){
                Log.i(tag_, "handling the cancel pre-api 3 style");
                handle_cancel();
            }
            else {

                Log.e(tag_, e_.toString());
                tv_status_.setText("Server Error");
                Snackbar.make(tv_status_, e_.toString(), Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        }
    }
}
