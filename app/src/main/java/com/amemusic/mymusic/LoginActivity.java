package com.amemusic.mymusic;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
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
    final static String tag_ = "LoginActivity";

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
    public class UserLoginTask extends AsyncTask<Void, String, auth_block_t> {

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
        protected void onProgressUpdate(String ... msg){
            if(msg.length > 0) {
                tv_status_.setText(msg[0]);
            }
        }

        @Override
        protected auth_block_t doInBackground(Void... params) {

            final int BUFFER_SIZE=my_core.BUFFER_SIZE;
            char buffer []= new char[BUFFER_SIZE];
            e_ = null;
            auth_block_t ret = null;

            publishProgress("Signing in ...");

            try {
                // TODO: attempt authentication against a network service.
                URL url = new URL(String.format("%s/get-auth.py?user-id=%s&password=%s", media_t.MAIN_URL, user_id_, password_));

                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                try {

                    connection.setRequestMethod("GET");
                    connection.connect();

                    int code = connection.getResponseCode();

                    if(code == 301 || code == 302){
                        throw new http_redirect_t(code, connection.getHeaderField("Location"), connection.getResponseMessage());
                    }
                    else if(!(code == 200)){
                        throw new http_exception_t(code, connection.getResponseMessage());
                    }

                    StringWriter writer = new StringWriter();
                    InputStream in = connection.getInputStream();
                    InputStreamReader isr = new InputStreamReader(in, "latin1");
                    try {
                        for (int ret_bytes = isr.read(buffer, 0, BUFFER_SIZE); ret_bytes != -1; ret_bytes = isr.read(buffer, 0, BUFFER_SIZE)) {
                            writer.write(buffer, 0, ret_bytes);
                        }
                    }
                    finally {
                        isr.close();
                    }

                    JSONObject json_auth_block = new JSONObject(writer.toString());
                    my_json_helper helper = new my_json_helper(json_auth_block);
                    writer.getBuffer().setLength(0);

                    String status = helper.try_string("status");

                    switch(status){
                        case "YES":
                            status_msg_="Login successful";
                            ret = new auth_block_t().read(json_auth_block);
                            break;
                        case "EXPIRED":
                            ret = null;
                            status_msg_ = "Accout expired.  Call 800-521-2537";
                            break;
                        case "NO":
                            status_msg_ = "User or password invalid";
                            ret = null;
                            break;
                        default:
                            status_msg_="Server Error";
                            ret = null;
                            break;
                    }

                }
                finally {
                    connection.disconnect();
                }
            }
            catch(http_redirect_t e){
                String alt_url = e.get_alt_url();
                Log.i(tag_, String.format("REDIRECTING to alt url:%s", alt_url));
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(alt_url)));
                status_msg_ = "Connection problem";
                e_ = e;
            }
            catch(Exception e){
                status_msg_ = "Connection problem";
                e_ = e;
            }

            // TODO: register the new account here.
            return ret;
        }

        @Override
        protected void onPostExecute(final auth_block_t auth_block) {

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

            if(auth_block != null){
                Intent intent = new Intent(context_, MainActivity.class);
                intent.putExtra(String.format("%s.auth_block", context_.getPackageName()), auth_block);
                intent.putExtra(String.format("%s.password", context_.getPackageName()), password_);
                startActivity(intent);
            }

            edit.commit();

            if(e_ != null){
                Log.e("LoginActivity", e_.toString());
                Snackbar.make(tv_status_, e_.toString(), Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
            else{
                Log.i(tag_, "Logged In");
            }
        }
    }
}

