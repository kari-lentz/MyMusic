package com.amemusic.mymusic;

import android.util.JsonReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by klentz on 10/29/15.
 */
public class my_song_reader {

    private grid_cols_t grid_cols_;

    private media_t try_special(media_t ret, String key, Object value){

        switch(key){
            case "DTS_RELEASED":
                ret.set_impact_dts((Date) value);
                break;
            case "TITLE":
                ret.set_title((String) value);
                break;
            case "ARTIST":
                ret.set_artist((String) value);
                break;
            case "EDIT":
                ret.set_edit((String) value);
                break;
        }

        return ret;
    }

    private media_t consume_special(media_t ret, String key, my_json_helper helper) throws IOException{

        switch(key){
            case "MUSIC_ID":
                ret.set_music_id(helper.try_int());
                break;
            default:
                helper.skipValue();
        }

        return ret;
    }

    private media_t read_song(JsonReader reader) throws IOException {

        media_t ret = new media_t();
        my_json_helper helper = new my_json_helper(reader);

        reader.beginObject();

        while (reader.hasNext()) {
            String key = reader.nextName();

            grid_col_t col = grid_cols_.get(key);

            if(col != null){
                grid_col_t.types_t type = col.get_type();

                switch(type){
                    case STRING:
                        ret.put_data(key, try_special(ret, key, helper.try_string()));
                        break;
                    case INT:
                        ret.put_data(key, try_special(ret, key, helper.try_int()));
                        break;
                    case DATE:
                        ret.put_data(key, try_special(ret, key, helper.try_date("MM/dd/yyyy")));
                        break;
                }
            }
            else{
                consume_special(ret, key, helper);
            }

        }

        reader.endObject();

        return ret;
    }

    public my_song_reader(grid_cols_t grid_cols)
    {
        grid_cols_ = grid_cols;
    }

    public ArrayList<media_t> call(InputStream in) throws IOException {
        ArrayList<media_t> ret = new ArrayList<media_t>();
        JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));

        try {

            reader.beginArray();

            while (reader.hasNext()) {
                ret.add(read_song(reader));
            }

            reader.endArray();

        } finally {
            reader.close();
        }

        return ret;
    }
}
