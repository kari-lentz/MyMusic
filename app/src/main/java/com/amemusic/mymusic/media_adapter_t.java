package com.amemusic.mymusic;


import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.amemusic.mymusisc.R;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by klentz on 10/29/15.
 */
public class media_adapter_t extends BaseAdapter {

    private Context context_;
    private grid_cols_t grid_cols_;
    private ArrayList<media_t> media_list_;

    private View row_view_;

    public media_adapter_t(Context context, grid_cols_t grid_cols, ArrayList<media_t> media_list){

        super();

        context_ = context;
        grid_cols_ = grid_cols;
        media_list_ = media_list;

        row_view_ = null;
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

            row_view_ = inflater.inflate(R.layout.lv_media_row, null);
            convertView = row_view_;

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
                    params.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,params.width=col.get_width() , convertView.getResources().getDisplayMetrics());
                    tv.setLayoutParams(params);

                    tv_list.add(tv);
                }

                convertView.setTag(tv_list);
            }

        }
        else {
            row_view_ = convertView;
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
