// MapsActivity.java
// Version 1.1
// July 21, 2018.

package com.morningwalk.ihome.explorer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

import static android.widget.Toast.*;
import static android.widget.Toast.makeText;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, Runnable, GoogleMap.OnMarkerClickListener  {

    private GoogleMap mMap;
    private static final int DOWNLOAD_MESSAGE = 1;
    private Thread mdownloadThread;
    Message msg;

    LatLng  lm;// = new LatLng (33.6938, 73.0652); // Zero point, Islamabad
    CameraPosition cameraPosition;
    Handler msgHandler;

    ArrayList<MemberInfo> memberList=new ArrayList<MemberInfo>();
    ArrayList<MemberInfo> nameList=new ArrayList<MemberInfo>();
    public UserPreferences up;
    JSONParser jsonParser;

    public Hashtable<Integer,Marker> markers;
    boolean bMarkersInitialized = false;
    boolean bStopTrackingPeople = true;
    int  tid = 0;
    float zoom = 15.0f;
    WanderAzimuth wah = WanderAzimuth.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        up = new UserPreferences(getBaseContext());
        up.whoami();
        tid = up.getPid ();
        lm = up.whereami ();
        zoom = up.getZoom ();
        wah.setFloat (up.getBearing ());
        cameraPosition = new CameraPosition.Builder().target(lm).zoom(zoom).bearing(up.getBearing ()).tilt(up.getTilt ()).build();

        if (!runtime_permissions()) {
            // Obtain the SupportMapFragment and get notified when the map is ready to be used.
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
            enable_service();

            mdownloadThread = new Thread(MapsActivity.this);
            mdownloadThread.start();
        }

        msgHandler = new Handler() {
            @Override
            public void handleMessage(Message msg)
            {
                super.handleMessage(msg);
                if(msg.what == DOWNLOAD_MESSAGE) {
                    if(!bMarkersInitialized) InitializeMarkers();

                    for (int i = 0; i < memberList.size (); i++) {
                        MemberInfo mi = memberList.get (i);
                        LatLng ll = new LatLng (mi.getLat (), mi.getLng ());
                        MarkerAnimation.animateMarkerToHC(markers.get (mi.id), ll, new LatLngInterpolator.Spherical());
                        if (mi.id == tid) lm = ll;
                    }

                    if(!bStopTrackingPeople) {
                        CameraPosition cp = mMap.getCameraPosition ();
                        float bearing = (tid == up.getPid () ? wah.getFloat() :  cp.bearing);
                        cameraPosition = new CameraPosition.Builder ().target (lm).zoom (zoom).bearing (bearing).tilt (cp.tilt).build ();
                        mMap.animateCamera (CameraUpdateFactory.newCameraPosition (cameraPosition));
                    }

                    if(wah.getAvailables ()) {
                        makeText (getApplicationContext(), getString (R.string.title_activity_maps), Toast.LENGTH_SHORT).show();
                        wah.setAvailable4 (false); // done - don't show until this flag asks for it
                    }
                }
                else if(msg.what == DOWNLOAD_MESSAGE+1) {
                    CameraPosition cp = mMap.getCameraPosition ();
                    float bearing = wah.getFloat();
                    cameraPosition = new CameraPosition.Builder ().target (lm).zoom (zoom).bearing (bearing).tilt (cp.tilt).build ();
                    mMap.animateCamera (CameraUpdateFactory.newCameraPosition (cameraPosition));
                }
            }
        };
    }

    @Override
    protected void onDestroy() {
        super.onDestroy ();
        up.SaveCameraOptions (lm, mMap.getCameraPosition()); // also save zoom
    }

    boolean runtime_permissions()
    {
        if(Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 10);
            return true;
        }

        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 10) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                enable_service ();

                // Obtain the SupportMapFragment and get notified when the map is ready to be used.
                SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager ().findFragmentById (R.id.map);
                mapFragment.getMapAsync (this);
            }
        }
    }

    void enable_service() {
        Intent i = new Intent(getApplicationContext(), LocationService.class);
        startService(i);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType (GoogleMap.MAP_TYPE_NORMAL);
        mMap.setMyLocationEnabled (true);
        mMap.setOnMarkerClickListener (this);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(false);

        mMap.addMarker (new MarkerOptions ().position (lm).title (up.getName()).icon (
                BitmapDescriptorFactory.defaultMarker (BitmapDescriptorFactory.HUE_GREEN)));
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        mMap.setOnInfoWindowClickListener (new GoogleMap.OnInfoWindowClickListener () {
            @Override
            public void onInfoWindowClick(Marker marker) {
                makeText(getApplicationContext(), getString (R.string.title_activity_maps), LENGTH_SHORT).show();
            }
        });
        mMap.setOnMapLongClickListener (new GoogleMap.OnMapLongClickListener () {
            @Override
            public void onMapLongClick(LatLng latLng) {
                bStopTrackingPeople = true;
                makeText(getApplicationContext(), "Tracking OFF", LENGTH_SHORT).show();
                //openPlacesDialog();
            }
        });
    }

    public void InitializeMarkers() {
        try {
            Marker marker;
            mMap.clear (); // remove previous marker (if any)
            markers = new Hashtable<Integer,Marker>();
            for (int i = 0; i < memberList.size (); i++) {
                MemberInfo mi = memberList.get (i);
                LatLng ll = new LatLng (mi.getLat (), mi.getLng ());
                if(mi.id == up.getPid ())
                     marker = mMap.addMarker (new MarkerOptions ().position (ll).title (mi.getName ()).zIndex(1.0f).icon (BitmapDescriptorFactory.defaultMarker (BitmapDescriptorFactory.HUE_GREEN)));
                else marker = mMap.addMarker (new MarkerOptions ().position (ll).title (mi.getName ()));//.anchor(0.5f,0.5f).rotation(90.0f));

                markers.put (mi.id, marker);
            }


            bMarkersInitialized = true;
            nameList.addAll (memberList);
        }
        catch (Exception e) {
//            makeText(getApplicationContext(), "Online: "+e.getMessage (), LENGTH_LONG).show();
        }
    }

    public boolean Online() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
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
        try {
            ArrayList<NameValue> params = new ArrayList<NameValue>();
            int index = 1;
//--            params.add(new NameValue("pid", up.getPid()+""));
            Thread.sleep(1000);
            while(true) {
                if(Online ()) {
                    params.add (new NameValue ("pid", up.getPid () + "_" + index));
                    memberList.clear ();
                    jsonParser = new JSONParser ();
                    JSONArray jArr = jsonParser.makeHttpRequest2 (getString (R.string.base_url) + getString (R.string.download_url), "POST", params);
                    if (jArr == null) continue;

                    for (int i = 0; i < jArr.length (); i++) {
                        JSONObject jObj = jArr.getJSONObject (i);

                        MemberInfo mi = new MemberInfo ();
                        mi.setID (jObj.getInt ("id"));
                        if (index > 0) mi.setName (jObj.getString ("name"));
                        mi.setLat (jObj.getDouble ("lat"));
                        mi.setLng (jObj.getDouble ("lng"));

                        memberList.add (mi);
                    }
                    msg = new Message ();
                    msg.what = DOWNLOAD_MESSAGE;
                    msgHandler.sendMessage (msg);
                    if (index > 0) index = 0;

                    Thread.sleep(3000);
                    if (wah.getAvailable ()) {
                        lm = new LatLng ((double) wah.getlat (), (double) wah.getlng ());
                        msg = new Message ();
                        msg.what = DOWNLOAD_MESSAGE + 1;
                        msgHandler.sendMessage (msg);
                        wah.setAvailable (false);
                    }

                    Thread.sleep(3000);
                }
                wah.setAvailable3 (true);
            }
        }
        catch (Exception e) {
            makeText(getApplicationContext(), "Online: "+e.getMessage (), LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        String name = marker.getTitle();

        Iterator<MemberInfo> iter = nameList.iterator();
        while ( iter.hasNext() == true ) {
            MemberInfo info =(MemberInfo)iter.next();
            if (info.getName ().equals(name)) {
                zoom = 15.0f;
                tid = info.getID ();
                lm = marker.getPosition ();
                bStopTrackingPeople = false;
                break;
            }
        }
        makeText(getApplicationContext(), "Tracking " + name, LENGTH_SHORT).show();

        return false;
    }
}
