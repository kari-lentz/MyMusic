package com.amemusic.mymusic;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {

    private TextView tv_user_id_;
    private TextView tv_password_;
    private TextView tv_status_;
    private CheckBox chk_remember_password_;
    private Button btn_login_;

    SharedPreferences prefs_;

    final static private String PREFS_FILE_= "THD-Preferences";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        this.setTitle(getResources().getString(R.string.title_activity_login));
        // Set up the login form.
        tv_user_id_ = (TextView) findViewById(R.id.txt_user_id);
        tv_password_ = (TextView) findViewById(R.id.txt_password);
        chk_remember_password_ = (CheckBox) findViewById(R.id.chk_remember_password);
        tv_status_ = (TextView) findViewById(R.id.txt_status);
        btn_login_ = (Button) findViewById(R.id.btn_login);

        tv_status_.setText("Ready");

        final Context context = this;

        btn_login_.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new UserLoginTask(context, tv_user_id_.getText().toString(), tv_password_.getText().toString()).execute();
            }
        });

        prefs_ = getSharedPreferences(PREFS_FILE_, MODE_PRIVATE);
        Boolean remember_password_p = prefs_.getBoolean("remember-password", true);
        chk_remember_password_.setChecked(remember_password_p);
        if(remember_password_p){
            tv_user_id_.setText(prefs_.getString("user-id", ""));
            tv_password_.setText(prefs_.getString("password", ""));
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final Context context_;
        private final String user_id_;
        private final String password_;
        private String status_msg_ ="";
        private Exception e_ = null;

        UserLoginTask(Context context, String user_id, String password) {
            context_ = context;
            user_id_ = user_id;
            password_ = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            final int BUFFER_SIZE=2048;
            char buffer []= new char[BUFFER_SIZE];
            e_ = null;
            Boolean ret = false;

            try {
                // TODO: attempt authentication against a network service.
                URL url = new URL(String.format("http://tophitsdirect.com/1.0.12.0/get-auth.py?user-id=%s&password=%s", user_id_, password_));

                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                try {
                    StringWriter writer = new StringWriter();
                    InputStream in = urlConnection.getInputStream();
                    InputStreamReader isr = new InputStreamReader(in, "latin1");
                    for(int ret_bytes = isr.read(buffer, 0, BUFFER_SIZE); ret_bytes != -1; ret_bytes = isr.read(buffer, 0, BUFFER_SIZE)){
                        writer.write(buffer, 0, ret_bytes);
                    }
                    JSONObject auth_block = new JSONObject(writer.toString());
                    my_json_helper helper = new my_json_helper(auth_block);

                    String status = helper.try_string("status");

                    switch(status){
                        case "YES":
                            status_msg_="Login successful";
                            ret = true;
                            break;
                        case "EXPIRED":
                            ret = false;
                            status_msg_ = "Accout expired.  Call 800-521-2537";
                            break;
                        case "NO":
                            status_msg_ = "Login Failure";
                            ret = false;
                            break;
                        default:
                            status_msg_="Server Error";
                            ret = false;
                            break;
                    }

                }
                finally {
                    urlConnection.disconnect();
                }
            }
            catch(Exception e){
                e_ = e;
            }

            // TODO: register the new account here.
            return ret;
        }

        @Override
        protected void onPostExecute(final Boolean success_p) {

            tv_status_.setText(status_msg_);

            Boolean remember_password_p = chk_remember_password_.isChecked();
            SharedPreferences.Editor edit = prefs_.edit();

            edit.putBoolean("remember-password", remember_password_p);

            if(remember_password_p)
            {
                edit.putString("user-id", user_id_);
                edit.putString("password", password_);
            }
            else{
                edit.putString("user-id", "");
                edit.putString("password", "");
            }

            if(success_p){
                Intent intent = new Intent(context_, MainActivity.class);
                intent.putExtra(String.format("%s.user_id", context_.getPackageName()), user_id_);
                intent.putExtra(String.format("%s.password", context_.getPackageName()), password_);
                startActivity(intent);
            }

            edit.commit();

            if(e_ != null){
                Snackbar.make(tv_status_, e_.toString(), Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }

        }

    }
}

