package com.amemusic.mymusic;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

/**
 * Created by klentz on 10/29/15.
 */
public class my_song_reader {

    private auth_block_t auth_block_;
    private grid_cols_t grid_cols_;

    private media_t read_song(JSONObject row) {

        media_t ret = new media_t(auth_block_);
        my_json_helper helper = new my_json_helper(row);

        ret.set_music_id(helper.try_int("MUSIC_ID"));
        ret.set_media_type(helper.try_string("MEDIA_TYPE"));
        ret.set_disc(helper.try_string("DISC"));
        ret.set_title(helper.try_string("TITLE"));
        ret.set_artist(helper.try_string("ARTIST"));
        ret.set_edit(helper.try_string("EDIT"));
        ret.set_impact_dts(helper.try_date("DTS_RELEASED", "MM/dd/yyyy"));
        ret.set_warning(helper.try_string("WARNING"));
        ret.set_process_date(helper.try_date("PROCESS_DATE", "yyyyy-MM-dd hh:mm:ss"));
        ret.set_credits_used(helper.try_int("CREDITS_USED"));
        ret.set_total_credits(helper.try_int("TOTAL_CREDITS"));

        Iterator<String> it = row.keys();

        while (it.hasNext()) {

            String key = it.next();

            grid_col_t col = grid_cols_.get(key);

            if(col != null){
                grid_col_t.types_t type = col.get_type();

                switch(type){
                    case STRING:
                        ret.put_data(key, helper.try_string(key));
                        break;
                    case INT:
                        ret.put_data(key, helper.try_int(key));
                        break;
                    case DATE:
                        ret.put_data(key, helper.try_date(key, "MM/dd/yyyy"));
                        break;
                }
            }
        }

        return ret;
    }

    public my_song_reader(auth_block_t auth_block, grid_cols_t grid_cols)
    {
        auth_block_ = auth_block;
        grid_cols_ = grid_cols;
    }

    public ArrayList<media_t> call(String in) throws JSONException {
        ArrayList<media_t> ret = new ArrayList<media_t>();

        JSONArray rows = new JSONArray(in);

        int len = rows.length();

        for(int idx = 0; idx < len; ++idx){
            JSONObject row = rows.getJSONObject(idx);
            ret.add(read_song(row));
        }

        return ret;
    }
}
