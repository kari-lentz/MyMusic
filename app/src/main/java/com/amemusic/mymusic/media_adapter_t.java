package com.amemusic.mymusic;


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
import java.util.Iterator;

/**
 * Created by klentz on 10/29/15.
 */
public class media_adapter_t extends ArrayAdapter<media_t> {

    private grid_cols_t grid_cols_;
    private View last_hover_view_;
    private int normal_color_;
    private int hover_color_;

    public media_adapter_t(Context context, grid_cols_t grid_cols, ArrayList<media_t> media_list){

        super(context,android.R.layout.simple_list_item_1, android.R.id.text1, media_list);

        grid_cols_ = grid_cols;

        last_hover_view_ = null;
        normal_color_ = ContextCompat.getColor(context, R.color.GRID_BACKGROUND_COLOR);
        hover_color_ = ContextCompat.getColor(context, R.color.GRID_HOVER_BACKGROUND_COLOR);
     }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater= LayoutInflater.from(getContext());

        if(convertView == null) {

            convertView = inflater.inflate(R.layout.lv_media_row, null);

            if ((convertView instanceof ViewGroup)) {

                ViewGroup viewGroup = (ViewGroup) convertView;

                ArrayList<TextView>  tv_list =  new ArrayList<TextView>();
                grid_cols_.rewind();

                while(grid_cols_.has_next()) {
                    grid_col_t col = grid_cols_.next();

                    View tvl = View.inflate(convertView.getContext(), R.layout.lv_media_col, null);
                    viewGroup.addView(tvl);

                    TextView tv = (TextView) tvl.findViewById(R.id.lv_media_col);
                    ViewGroup.LayoutParams params = tv.getLayoutParams();
                    params.width=col.get_width();
                    tv.setLayoutParams(params);

                    tv_list.add(tv);
                }

                convertView.setTag(tv_list);
            }

            convertView.setOnHoverListener(new View.OnHoverListener() {
                @Override
                public boolean onHover(View v, MotionEvent event) {

                    if (v != last_hover_view_ && last_hover_view_ != null) {
                        last_hover_view_.setBackgroundColor(normal_color_);
                    }

                    v.setBackgroundColor(hover_color_);
                    last_hover_view_ = v;

                    return false;
                }
            });
        }

        ArrayList<TextView> tv_list = (ArrayList<TextView>) convertView.getTag();

        media_t media = (media_t) this.getItem(position);

        grid_cols_.rewind();
        Iterator<TextView> it = tv_list.iterator();

        while(grid_cols_.has_next()) {

            grid_col_t col = grid_cols_.next();
            TextView tv = it.next();

            tv.setText(col.string(media.get_data(col.get_key())));
        }

        return convertView;
    }
}
