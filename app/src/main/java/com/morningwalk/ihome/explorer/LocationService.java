// LocationService.java
// Version 1.1
// July 21, 2018.
// updated: November 30, 2018

package com.morningwalk.ihome.explorer;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import static android.widget.Toast.makeText;

public class LocationService extends Service implements LocationListener, Runnable {
    final static int OutageCount = 5;
    private UserPreferences up;
    private LocationManager locationManager;
    JSONParser jsonParser = new JSONParser();
    private Thread muploadThread;
    volatile List<PointInfo> mList = new ArrayList<PointInfo>();
    WanderAzimuth wah = WanderAzimuth.getInstance();
    boolean isOutage = true;
    Database db = null;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onCreate() {
        db = new Database (this);
        up = new UserPreferences(getBaseContext());
        if(up.whoami()) {
            locationManager = (LocationManager) getApplication().getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 10, this);

            muploadThread = new Thread (LocationService.this);
            muploadThread.start ();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(locationManager != null) {
            locationManager.removeUpdates(this);

            mList.clear ();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        double v = location.getSpeed ();
        mList.add (new PointInfo<Double> (location.getLatitude (), location.getLongitude (), location.getAltitude (), v));
        if(OutageCount <= mList.size ()) {
            int n = db.getPointsCount ();
            for(int i=0; i<mList.size (); i++) {
                PointInfo<Double> pi = mList.get (i);
                db.insertPoint (n+i, pi);
            }
            isOutage = true;
            mList.clear ();
        }

        wah.setAvailable1 (true);
        if(v >= 1.0 && wah.getAvailable ()==false) {
            wah.setFloat(location.getBearing ());
            wah.setlat ((float)location.getLatitude ());
            wah.setlng ((float)location.getLongitude ());
            wah.setAvailable(true);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
        Intent it = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(it);
    }

    public void SendList(List<PointInfo> list) {
        try {
            while (list.size () > 0) {
                PointInfo<Double> pt = list.get (0);
                Register register = new Register (up.getPid (), pt.getAlt (), pt.getVel ());
                register.execute (pt.getLat () + "", pt.getLng () + "", "");
                list.remove (0);
                if(list.size () > 0) Thread.sleep (20);
            }
        }
        catch (Exception e) {
//            Toast.makeText(getApplicationContext(), e.getMessage (), Toast.LENGTH_SHORT).show();
        }
    }

    public boolean Online() {
        try {
            ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            return (netInfo != null && netInfo.isConnected() && netInfo.isAvailable ());
        }
        catch (Exception e) {
//            Toast.makeText(getApplicationContext(), "Online: "+e.getMessage (), Toast.LENGTH_SHORT).show();
        }

        return false;
    }

    @Override
    public void run() {
        while (true) {
            try {
                if (Online ()) {
                    if( isOutage) {
                        SendList(db.getAllPoints ());
                        isOutage = false;
                    }
                    else if(mList.size () > 0) {
                        SendList(mList);
                    }
                    wah.setAvailable2 (true);
                }
                Thread.sleep (1000);
            }
            catch (Exception e) {
//                Toast.makeText(getApplicationContext(), e.getMessage (), Toast.LENGTH_SHORT).show();
            }
        }
    }

    //  Register Class
    private class Register extends AsyncTask<String, String, JSONObject> {
        private double altitude;
        private double speed;
        private int pid;

        Register(int pid, double altitude, double speed) {
            this.pid = pid;
            this.altitude = altitude;
            this.speed = speed;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected JSONObject doInBackground(String... args) {
            String pid = this.pid+"";
            String lat = args[0];
            String lng = args[1];
            String alt = this.altitude+"";
            String spd = this.speed+"";

            ArrayList<NameValue> params = new ArrayList<NameValue>();
            params.add(new NameValue("pid", pid));
            params.add(new NameValue("lat", lat));
            params.add(new NameValue("lng", lng));
            params.add(new NameValue("alt", alt));
            params.add(new NameValue("spd", spd));

            return jsonParser.makeHttpRequest(getString(R.string.base_url)+getString(R.string.despatch_url), "POST", params);
        }

        protected void onPostExecute(JSONObject result) {
//            try {
                if (result == null) {
                    Toast.makeText(getApplicationContext(), jsonParser.error, Toast.LENGTH_SHORT).show();
                }
//                else {
  //                  Toast.makeText(getApplicationContext(), result.getString("message"), Toast.LENGTH_SHORT).show();
    //            }
      //      }
        //    catch (JSONException e) {
          //      e.printStackTrace();
            //}
        }
    }
}
