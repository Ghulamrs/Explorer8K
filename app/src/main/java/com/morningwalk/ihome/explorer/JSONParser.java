// JSONParser.java
// Version 1.1
// July 21, 2018.


package com.morningwalk.ihome.explorer;

import android.app.Activity;
import android.app.Application;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.util.JsonUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

class JSONParser {
    String json = "";
    public String error = "";
    JSONObject jObj = null;

    // constructor
    public JSONParser() {
    }

    // function get json from url by making HTTP POST or GET mehtod
    public JSONObject makeHttpRequest(String url_string, String method, ArrayList<NameValue> params) {

        // Making HTTP request
        try {
            error = "";
            // check for request method
            if(method.equals("POST")) {
                URL url = new URL(url_string);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try {
                    urlConnection.setDoOutput(true);
                    urlConnection.setChunkedStreamingMode(0);

                    OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
                    Uri.Builder builder = new Uri.Builder();
                    for(int i=0; i<params.size(); i++) {
                        builder.appendQueryParameter(params.get(i).name, params.get(i).value);
                    }

                    String query = builder.build().getEncodedQuery();
                    // Open connection for sending data
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
                    writer.write(query);
                    writer.flush();
                    writer.close();
                    out.close();

                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    readStream(in);
                    in.close();
                }
                finally {
                    urlConnection.disconnect();
                }
            }
        }
        catch (MalformedURLException e) {
            error = e.getMessage();
        }
        catch (IOException e) {
            if(error.equals("")) error = e.toString();
        }

        // try to parse the string to a JSON object
        try {
            jObj = new JSONObject(json);
            jObj.put("error_code", error);
        }
        catch (JSONException e) {
//            Log.e("JSON Parser", "Error parsing data " + e.toString());
            if(error.equals("")) error = e.toString();
        }

        // return JSON String
        return jObj;
    }

    // function get json from url by making HTTP POST or GET mehtod
    public JSONArray makeHttpRequest2(String url_string, String method, ArrayList<NameValue> params) {

        // Making HTTP request
        try {
            error = "";
            // check for request method
            if(method.equals("POST")) {
                URL url = new URL(url_string);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try {
                    urlConnection.setDoOutput(true);
                    urlConnection.setChunkedStreamingMode(0);

                    OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
                    Uri.Builder builder = new Uri.Builder();
                    for(int i=0; i<params.size(); i++) {
                        builder.appendQueryParameter(params.get(i).name, params.get(i).value);
                    }

                    String query = builder.build().getEncodedQuery();
                    // Open connection for sending data
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
                    writer.write(query);
                    writer.flush();
                    writer.close();
                    out.close();

                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    readStream(in);
                    in.close();
                }
                finally {
                    urlConnection.disconnect();
                }
            }
        }
        catch (MalformedURLException e) {
            error = e.getMessage();
        }
        catch (IOException e) {
            if(error.equals("")) error = e.toString();
        }

        // try to parse the string to a JSON object
        JSONArray arr = null;
        try {
            arr = new JSONArray(json);
        }
        catch (JSONException e) {
//            Log.e("JSON Parser", "Error parsing data " + e.toString());
            if(error.equals("")) error = e.toString();
        }

        // return JSON String
        return arr;
    }

    void readStream(InputStream is)
    {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));//, "iso-8859-1"), 8);
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
            json = sb.toString();
        }
        catch (Exception e) {
//            Log.e("Buffer Error", "Error converting result " + e.toString());
            if(error.equals("")) error = e.toString();
        }
    }
}
