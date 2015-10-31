package com.amemusic.mymusic;

import android.util.JsonReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Created by klentz on 10/29/15.
 */
public class my_song_reader {

    private grid_col_t[] grid_cols_;

    private media_t read_song(JsonReader reader) throws IOException {

        media_t ret = new media_t();
        my_json_helper helper = new my_json_helper(reader);

        reader.beginObject();

        while (reader.hasNext()) {
            String name = reader.nextName();

            switch(name){
                case "MUSIC_ID":
                    ret.set_music_id(helper.try_int());
                    break;
                case "DTS_RELEASED":
                    ret.set_impact_dts(helper.try_date("MM/dd/yyyy"));
                    break;
                case "TITLE":
                    ret.set_title(helper.try_string());
                    break;
                case "ARTIST":
                    ret.set_artist(helper.try_string());
                    break;
                case "EDIT":
                    ret.set_edit(helper.try_string());
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }

        reader.endObject();

        return ret;
    }

    public my_song_reader(grid_col_t[] grid_cols)
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
