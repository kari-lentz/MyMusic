package com.amemusic.mymusic;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

/**
 * Created by klentz on 10/31/15.
 */
public class grid_cols_t {

    private Hashtable<String, grid_col_t> col_hash_;
    private ArrayList<grid_col_t> cols_;
    private Iterator<grid_col_t> it_;

    public grid_cols_t(grid_col_t[] grid_cols){

        col_hash_ = new Hashtable<String, grid_col_t>();
        cols_ = new ArrayList<grid_col_t>();

        for(int idx=0; idx < grid_cols.length; ++idx){
            grid_col_t temp = grid_cols[idx];
            col_hash_.put(temp.get_key(), temp);
            cols_.add(temp);
        }

        rewind();
    }

    public grid_col_t get(int idx){
        return cols_.get(idx);
    }

    public grid_col_t get(String key){
        return col_hash_.get(key);
    }

    void rewind(){
       it_ = cols_.iterator();
    }

    boolean has_next() {
        return it_.hasNext();
    }

    grid_col_t next(){
        return it_.next();
    }
}
