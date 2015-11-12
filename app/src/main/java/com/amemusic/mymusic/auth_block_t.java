package com.amemusic.mymusic;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by klentz on 11/10/15.
 */
public class auth_block_t implements java.io.Serializable{

    private String user_id_;
    final private ArrayList<Date[]> payment_history_ = new ArrayList<Date[]>();

    auth_block_t(){}

    void add_payment(Date dts_lo, Date dts_hi){
        payment_history_.add(new Date [] {dts_lo, dts_hi});
    }

    auth_block_t read(JSONObject json) throws JSONException {

        user_id_ = json.getString("user-id");

        JSONArray payments = json.getJSONArray("payment-dtses");

        for(int idx=0; idx < payments.length(); ++idx){
            JSONObject payment = payments.getJSONObject(idx);
            my_json_helper helper = new my_json_helper(payment);
            add_payment(helper.try_date("SERVICE_DATE_BEGIN", "yyyy/MM/dd"),
                    helper.try_date("SERVICE_DATE_END", "yyyy/MM/dd"));
        }

        return this;
    }

    String get_user_id(){
        return user_id_;
    }

    Boolean is_restricted(media_t media){
        Boolean ret = true;
        Date process_date = media.get_process_date();

        int len = payment_history_.size();
        for(int idx =0; idx < len; ++idx){
            Date range[] = payment_history_.get(idx);
            if(range.length > 1 && (range[0].before(process_date) && range[1].after(process_date)) || range[0].equals(process_date)) {
                ret = false;
                break;
            }
        }

        return ret;
    }
}
