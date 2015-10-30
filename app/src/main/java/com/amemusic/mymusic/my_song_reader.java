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

    static private media_t read_song(JsonReader reader) throws IOException {

        media_t ret = new media_t();
        my_json_helper helper = new my_json_helper(reader);

        reader.beginObject();

        while (reader.hasNext()) {
            String name = reader.nextName();

            if (name.equals("MUSIC_ID")){
                ret.set_music_id(helper.try_int());
            } else if (name.equals("DTS_RELEASED")) {
                ret.set_impact_dts(helper.try_date("MM/dd/yyyy"));
            } else if (name.equals("TITLE")) {
                ret.set_title(helper.try_string());
            } else if (name.equals("ARTIST")) {
                ret.set_artist(helper.try_string());
            } else if (name.equals("EDIT")) {
                ret.set_edit(helper.try_string());
            } else {
                reader.skipValue();
            }

        }

        reader.endObject();

        return ret;
    }

    static public ArrayList<media_t> call(InputStream in) throws IOException {
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
