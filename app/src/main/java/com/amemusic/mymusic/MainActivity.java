package com.amemusic.mymusic;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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
;

import com.amemusic.mymusisc.R;

import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    resize_context_t resize_context_;

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

        TextView tv_status = (TextView) findViewById(R.id.txt_status);
        try {
            AsyncTask task = new media_getter(this, lv, tv_status, header, grid_cols).execute(new URL("http://tophitsdirect.com/1.0.12.0/get-media.py?media_type=MP3&disc_type=ALL&user_id=TH_KLentz2&json=t"));
        } catch (MalformedURLException e) {
            tv_status.setText("Incomplete URL");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final Context context = this;
        resize_context_ = null;
        final ListView lv = (ListView) findViewById(R.id.lv_media);

        final grid_cols_t grid_cols = new grid_cols_t(new grid_col_t[]{
                new grid_col_date_t("DTS_RELEASED", "Impact Date", 80),
                new grid_col_t("TITLE", "Title", 200, grid_col_t.types_t.STRING),
                new grid_col_t("ARTIST", "Artist", 200, grid_col_t.types_t.STRING),
                new grid_col_t("EDIT", "Edit", 100, grid_col_t.types_t.STRING),
                new grid_col_t("DISC", "Disc", 70, grid_col_t.types_t.STRING),
                new grid_col_t("LABEL", "Label", 100, grid_col_t.types_t.STRING),
                new grid_col_t("FORMAT", "Genre", 100, grid_col_t.types_t.STRING),
                new grid_col_t("BPM", "BPM", 50, grid_col_t.types_t.INT),
                new grid_col_t("INTRO", "Intro", 100, grid_col_t.types_t.INT),
                new grid_col_t("RUN", "Run", 100, grid_col_t.types_t.INT),
                new grid_col_t("CHART", "Chart", 100, grid_col_t.types_t.STRING)
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
                    //TextView tv = (TextView) findViewById(R.id.txt_status);
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
                int position = ((media_adapter_t) lv.getAdapter()).get_selected_position();
                media_t track = (position != -1) ? (media_t) lv.getItemAtPosition(position) : null;
                Snackbar.make(view, track != null ? String.format("%s - %s", track.get_title(), track.get_artist()):"nothing selected", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
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
