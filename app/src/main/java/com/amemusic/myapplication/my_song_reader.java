package com.amemusic.myapplication;

import android.util.JsonReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Created by klentz on 10/29/15.
 */
public class my_song_reader {

    static private String read_song(JsonReader reader) throws IOException {
        String title = new String(), artist = new String();

        reader.beginObject();

        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("TITLE")) {
                title = reader.nextString();
            } else if (name.equals("ARTIST")) {
                artist = reader.nextString();
            } else {
                reader.skipValue();
            }
        }

        reader.endObject();
        return title + " - " + artist;
    }

    static public ArrayList<String> call(InputStream in) throws IOException {
        ArrayList<String> ret = new ArrayList<String>();
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
