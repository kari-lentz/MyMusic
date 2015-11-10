package com.amemusic.mymusic;


import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.amemusic.mymusic.R;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by klentz on 10/29/15.
 */
public class media_adapter_t extends BaseAdapter {

    private Context context_;
    private grid_cols_t grid_cols_;
    private ArrayList<media_t> media_list_;
    private int selected_position_;
    private int default_color_;
    private int selected_color_;
    private View selected_view_;

    public media_adapter_t(Context context, grid_cols_t grid_cols, ArrayList<media_t> media_list){

        super();

        context_ = context;
        grid_cols_ = grid_cols;
        media_list_ = media_list;

        selected_position_ = -1;
        selected_view_ = null;

        default_color_ = ContextCompat.getColor(context, R.color.GRID_BACKGROUND_COLOR);
        selected_color_ = ContextCompat.getColor(context, R.color.GRID_SELECTED_BACKGROUND_COLOR);
    }

    public int get_selected_position(){
        return selected_position_;
    }

    public void update_unselected_pos(View view){
        view.setBackgroundColor(default_color_);
        if(view == selected_view_){
            selected_view_ = null;
        }

        view.invalidate();
    }

    public void update_selected_pos(View view, int position){

        view.setBackgroundColor(selected_color_);

        if(selected_view_ != null && selected_view_ != view){
            selected_view_.setBackgroundColor(default_color_);
        }

        selected_position_ = position;
        selected_view_ = view;

        view.invalidate();
        selected_view_.invalidate();
    }

    @Override
    public Object getItem(int position){
        return media_list_.get(position);
    }

    @Override
    public int getCount(){
        return media_list_.size();
    }

    @Override
    public long getItemId(int position){
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater= LayoutInflater.from(context_);

        if(convertView == null) {

            convertView = inflater.inflate(R.layout.lv_media_row, null);

            if ((convertView instanceof ViewGroup)) {

                ViewGroup viewGroup = (ViewGroup) convertView;

                ArrayList<ViewGroup>  col_views =  new ArrayList<ViewGroup>();
                grid_cols_.rewind();

                while(grid_cols_.has_next()) {
                    grid_col_t col = grid_cols_.next();

                    View tvl = View.inflate(convertView.getContext(), R.layout.lv_media_col, null);
                    viewGroup.addView(tvl);
                    col_views.add((ViewGroup) tvl);
                }

                convertView.setTag(col_views);
            }

        }

        ArrayList<ViewGroup> col_views = (ArrayList<ViewGroup>) convertView.getTag();

        if(position == selected_position_) {
            update_selected_pos(convertView, position);
        }
        else{
            update_unselected_pos(convertView);
        }

        media_t media = (media_t) this.getItem(position);

        grid_cols_.rewind();
        Iterator<ViewGroup> it = col_views.iterator();

        while(grid_cols_.has_next()) {

            grid_col_t col = grid_cols_.next();
            ViewGroup tvl = it.next();

            ViewGroup.LayoutParams params = tvl.getLayoutParams();
            params.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, col.get_width(), convertView.getResources().getDisplayMetrics());
            tvl.setLayoutParams(params);

            TextView tv = (TextView) tvl.findViewById(R.id.lv_media_col);
            tv.setText(col.string(media));

            tv.setTextColor(col.get_text_color(media));
        }

        return convertView;
    }
}
