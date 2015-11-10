package com.amemusic.mymusic;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by klentz on 11/10/15.
 */
public class auth_block_t {

    private ArrayList<Date[]> payment_history_;

    void add_payment(Date dts_lo, Date dts_hi){
        payment_history_.add(new Date [] {dts_lo, dts_hi});
    }

    void read(JSONObject json) throws JSONException {
        JSONArray payments = json.getJSONArray("payment-dtses");

        for(int idx=0; idx < payments.length(); ++idx){
            JSONObject payment = payments.getJSONObject(idx);
            my_json_helper helper = new my_json_helper(payment);
            add_payment(helper.try_date("SERVICE_DATE_BEGIN", "yyyy/MM/dd"),
                    helper.try_date("SERVICE_DATE_END", "yyyy/MM/dd"));
        }
    }
}
