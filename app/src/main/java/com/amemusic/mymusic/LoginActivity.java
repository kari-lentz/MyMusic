package com.amemusic.mymusic;

import android.support.v7.app.AppCompatActivity;
import android.os.AsyncTask;
import android.os.Bundle;

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



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // Set up the login form.

    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String user_id_;
        private final String password_;
        private String status_msg_ ="";
        private Exception e_ = null;

        UserLoginTask(String user_id, String password) {
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
                URL url = new URL(String.format("http://tophitsdirect.com/get-auth.py?user-id=%s&password=%s", user_id_, password_));

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
                        case "Yes":
                            ret = true;
                            break;
                        case "EXPIRED":
                            ret = false;
                            status_msg_ = "Accout expired.  Call 800-521-2537";
                            break;
                        case "NO":
                            ret = false;
                            break;
                        default:
                            ret = false;
                            status_msg_="Server Error";
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
        protected void onPostExecute(final Boolean success) {
        }

    }
}

