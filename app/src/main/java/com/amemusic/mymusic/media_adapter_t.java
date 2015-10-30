package com.amemusic.mymusic;

import android.app.Activity;
import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.amemusic.mymusisc.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

/**
 * Created by klentz on 10/29/15.
 */
public class media_adapter_t extends ArrayAdapter<media_t> {

    private View last_hover_view_;
    private int normal_color_;
    private int hover_color_;

    public media_adapter_t(Context context, ArrayList<media_t> media_list){

        super(context,android.R.layout.simple_list_item_1, android.R.id.text1, media_list);

        last_hover_view_ = null;

        normal_color_ = ContextCompat.getColor(context, R.color.GRID_BACKGROUND_COLOR);
        hover_color_ = ContextCompat.getColor(context, R.color.GRID_HOVER_BACKGROUND_COLOR);
     }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater= LayoutInflater.from(getContext());

        TextView t1, t2, t3, t4;

        if(convertView == null) {

            convertView = inflater.inflate(R.layout.lv_media_row, null);

            convertView.setOnHoverListener(new View.OnHoverListener() {
                @Override
                public boolean onHover(View v, MotionEvent event) {

                    if(v != last_hover_view_ && last_hover_view_ != null){
                        last_hover_view_.setBackgroundColor(normal_color_);
                    }

                    v.setBackgroundColor(hover_color_);
                    last_hover_view_ = v;

                    return false;
                }
            });
        }

        t1=(TextView) convertView.findViewById(R.id.lv_media_impact_dts);
        t2=(TextView) convertView.findViewById(R.id.lv_media_title);
        t3=(TextView) convertView.findViewById(R.id.lv_media_artist);
        t4=(TextView) convertView.findViewById(R.id.lv_media_edit);

         media_t media = (media_t) this.getItem(position);

        t1.setText((new SimpleDateFormat("MM/dd/yyyy")).format(media.get_impact_dts()));
        t2.setText(media.get_title());
        t3.setText(media.get_artist());
        t4.setText(media.get_edit());

        return convertView;
    }
}
