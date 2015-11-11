package com.amemusic.mymusic;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
;
import com.amemusic.mymusic.R;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MainActivity extends AppCompatActivity {

    String user_id_;
    String password_;
    resize_context_t resize_context_ = null;
    ConcurrentLinkedQueue<media_t> media_queue_ = new ConcurrentLinkedQueue<>();
    download_task download_task_ = null;

    TextView tv_status_;

    private void run_view(View header, grid_cols_t grid_cols) {

        final ListView lv = (ListView) findViewById(R.id.lv_media);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                media_adapter_t adapter = ((media_adapter_t) lv.getAdapter());
                if(adapter != null){
                    adapter.update_selected_pos(view, position);
                }
            }
        });

        try {
            new grid_getter(this, lv, tv_status_, header, grid_cols).execute(new URL(String.format("http://tophitsdirect.com/1.0.12.0/get-media.py?media_type=MP3&disc_type=ALL&user_id=%s2&json=t", user_id_)));
        } catch (MalformedURLException e) {
            tv_status_.setText("Incomplete URL");
        }
    }

    private void start_download_task(media_t media){

        media_queue_.add(media);

        if(download_task_ == null || download_task_.getStatus() != AsyncTask.Status.RUNNING){
            //start brand new download task if previous one finished
            download_task_ = new download_task(this, tv_status_, media_queue_, media_t.get_codec(), user_id_, password_);
            download_task_ .execute();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        user_id_ = getIntent().getStringExtra(String.format("%s.user_id", this.getPackageName()));
        password_ = getIntent().getStringExtra(String.format("%s.password", this.getPackageName()));

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tv_status_ = (TextView) findViewById(R.id.txt_status);

        final Context context = this;
        final ListView lv = (ListView) findViewById(R.id.lv_media);

        final grid_cols_t grid_cols = new grid_cols_t(new grid_col_t[]{
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

        final View header= findViewById(R.id.lv_media_header);
        int header_color = ContextCompat.getColor(header.getContext(), R.color.GRID_HEADER_BACKGROUND_COLOR);
        header.setBackgroundColor(header_color);

        grid_cols.rewind();

        while(grid_cols.has_next()) {

            final grid_col_t col = grid_cols.next();

            final View header_col = View.inflate(header.getContext(), R.layout.lv_media_col, null);
            ((ViewGroup) header).addView(header_col);
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
                            resize_context_ = new resize_context_t(lv, v, col, xpos) ;
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

                if(resize_context_ != null) {
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

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                media_adapter_t adapter = (media_adapter_t) lv.getAdapter();

                String msg;
                media_t media = null;

                if(adapter != null) {
                    int position = adapter.get_selected_position();
                    media = (position != -1) ? (media_t) lv.getItemAtPosition(position) : null;
                    msg = media != null ? String.format("downloading %s to %s", media.get_file_name(), media.get_disc()) : "nothing selected";
                }
                else {
                    media = null;
                    msg = "No media available";
                }

                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();

                if(media != null){
                    start_download_task(media);
                }
            }});

        Button btn = (Button) findViewById(R.id.btn_fetch_media);
        btn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        TextView tv = (TextView) findViewById(R.id.txt_status);
                        tv.setText("Loading ...");
                        run_view(header, grid_cols);
                        }});

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
