package com.amemusic.mymusic;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;;

import com.amemusic.mymusisc.R;

import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private void run_view() {

        grid_cols_t cols = new grid_cols_t(new grid_col_t[]{
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

        ListView lv = (ListView) findViewById(R.id.lv_media);
        TextView tv = (TextView) findViewById(R.id.txt_status);
        try {
            AsyncTask task = new url_getter(this, lv, tv, cols).execute(new URL("http://tophitsdirect.com/1.0.12.0/get-media.py?media_type=MP3&disc_type=ALL&user_id=TH_KLentz2&json=t"));
        } catch (MalformedURLException e) {
            tv.setText("Incomplete URL");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();}});

        Button btn = (Button) findViewById(R.id.btn_fetch_media);
        btn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        TextView tv = (TextView) findViewById(R.id.txt_status);
                        tv.setText("Loading ...");
                        run_view();
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
