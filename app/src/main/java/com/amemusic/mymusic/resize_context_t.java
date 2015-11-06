package com.amemusic.mymusic;

import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.amemusic.mymusisc.R;

/**
 * Created by klentz on 11/3/15.
 */
public class resize_context_t {

    private ListView lv_;
    private View header_col_;
    private grid_col_t col_;
    private float mx_;

   resize_context_t(ListView lv, View header_col, grid_col_t col, float x){
       lv_ = lv;
       header_col_ = header_col;
       col_ = col;
       mx_ = x;
   }

    void call(float x){

        int offset = (int) (x - mx_);
        int width = col_.get_width() + offset;
        if(width < 50){
            width = 50;
        }
        col_.set_width(width);

        ViewGroup.LayoutParams params = header_col_.getLayoutParams();
        params.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, params.width = col_.get_width(), header_col_.getResources().getDisplayMetrics());
        header_col_.setLayoutParams(params);
        header_col_.requestLayout();

        BaseAdapter adapter = (BaseAdapter) lv_.getAdapter();

        if(adapter != null) {
            adapter.notifyDataSetChanged();
        }

        mx_ = x;
    }
}
