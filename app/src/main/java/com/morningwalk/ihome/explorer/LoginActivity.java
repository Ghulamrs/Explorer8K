// LoginActivity.java
// Version 1.1
// July 21, 2018.

package com.morningwalk.ihome.explorer;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Application;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;

import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;

import org.json.JSONException;
import org.json.JSONObject;

import static android.widget.Toast.*;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {

    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    public String IdAssigned="";
    JSONParser jsonParser = new JSONParser();
    private final static String[] ref_user_array = {
            "invalid", "gra", "shan", "ahsan", "saeed", "gra1", "namrah", "asif", "khalid", "qamar", "erfa", "unknown"};
    private final static int ref_user_count = 12;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        UserPreferences up = new  UserPreferences(getApplicationContext ());
        if(up.whoami()) {
            Intent i = new Intent(getApplicationContext(), MapsActivity.class);
            startActivity(i);
        }
        else {
            // Set up the login form.
            mEmailView = (AutoCompleteTextView) findViewById (R.id.email);
            mPasswordView = (EditText) findViewById (R.id.password);
            mPasswordView.setOnEditorActionListener (new TextView.OnEditorActionListener () {
                @Override
                public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                    if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                        attemptLogin ();
                        return true;
                    }
                    return false;
                }
            });

            Button mEmailSignInButton = (Button) findViewById (R.id.email_sign_in_button);
            mEmailSignInButton.setOnClickListener (new OnClickListener () {
                @Override
                public void onClick(View view) {
                    attemptLogin ();
                }
            });

            mLoginFormView = findViewById (R.id.login_form);
            mProgressView = findViewById (R.id.login_progress);
        }
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        }
        else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first form field with an error.
            focusView.requestFocus();
        }
        else {
            // Show a progress spinner, and kick off a background task to perform the user login attempt.
            showProgress(true);
            Register register = new Register();
            register.execute(email, password, "");
        }
    }

    private boolean isEmailValid(String email) {
        return email.length() > 2;
    }

    private boolean isPasswordValid(String password) {
        return password.length() > 3;
    }

    //  Register Class
    private class Register extends AsyncTask<String, String, JSONObject> {
        private String name="";
        private String pswd="";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected JSONObject doInBackground(String... args) {
            name = args[0];
            pswd = args[1];

            ArrayList<NameValue> params = new ArrayList<NameValue>();
            params.add(new NameValue("name", name));
            params.add(new NameValue("pswd", pswd));
            return jsonParser.makeHttpRequest(getString(R.string.base_url)+getString(R.string.login_url), "POST", params);
        }

        protected void onPostExecute(JSONObject result) {
            try {
                showProgress(false);
                if (result != null) {
                    IdAssigned = result.getString("success");
                    int pid = Integer.parseInt(IdAssigned);
                    if(pid > 0) {
                        UserPreferences up = new  UserPreferences(getBaseContext());
                        up.Save(pid, name);
                    }
                    else {
///////////////////////////////////////////////////////////////////////////////////////////////////
// Specially written for specific id enforcement - only store this pid in handset as it might be
// already registered and url has denied to re-register it - this is a special case registration
// ******************** MUST BE OMITTED FROM NORMAL APPLICATION CODE ***************************
                        UserPreferences up = new UserPreferences(getBaseContext());
                        pid = getReferenceId(name);
//                        //pid=1; //name=new String(getString (R.string.login_user));
                        up.Save(pid, name);
// ******************** MUST BE OMITTED FROM NORMAL APPLICATION CODE ***************************
///////////////////////////////////////////////////////////////////////////////////////////////////
                    }
                    makeText(getApplicationContext(),result.getString("message"), LENGTH_LONG).show();
                    if(pid > 0) invoke_map_activity();
                } else {
                    makeText(getApplicationContext(), "Error : "+jsonParser.error, LENGTH_LONG).show();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            finish();
        }

        void invoke_map_activity() {
            Intent i = new Intent(getApplicationContext(), MapsActivity.class);
            startActivity(i);
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        }
        else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    int getReferenceId(String Name) {
        for(int i = 0; i < ref_user_count; i++) {
            String info = ref_user_array[i];
            if(info.equalsIgnoreCase (Name)) return i;
        }
        return 0;
    }
};
