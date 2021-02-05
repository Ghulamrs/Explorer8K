// WanderAzimuth.java
// Version 3.65
// November 23, 2018.

// Shares azimuth between LocationService and MapsAcivity classes

package com.morningwalk.ihome.explorer;

class WanderAzimuth {
    private static WanderAzimuth instance;

    // Global variable
    private int count = 0;
    private float data = 0.0f;
    private float lat  = 33.6938f;
    private float lng  = 73.0652f;
    private boolean available  = false; // 0. azimuth & location available
    private boolean available1 = false; // 1. location update available
    private boolean available2 = false; // 2. location sending thread available
    private boolean available3 = false; // 3. location receiving thread available
    private boolean available4 = true;  // 4. 'On-line' message show available

    // Restrict the constructor from being instantiated
    private WanderAzimuth(){}
    public void setFloat(float d) {
        this.data=d;
    }
    public float getFloat() {
        return this.data;
    }
    public void setlat(float d) {
        this.lat=d;
    }
    public float getlat() {
        return this.lat;
    }
    public void setlng(float d) {
        this.lng=d;
    }
    public float getlng() {
        return this.lng;
    }
    public void setAvailable(boolean b) { this.available=b; }
    public boolean getAvailable() {
        return this.available;
    }
    public void setAvailable1(boolean b) {
        if(++count%20==0) this.available4 = true;
        this.available1=b;
    }
    public boolean getAvailable1() {
        return this.available1;
    }
    public void setAvailable2(boolean b) { this.available2=b; }
    public boolean getAvailable2() {
        return this.available2;
    }
    public void setAvailable3(boolean b) { this.available3=b; }
    public boolean getAvailable3() {
        return this.available3;
    }
    public void setAvailable4(boolean b) { this.available4=b; }
    public boolean getAvailable4() {
        return this.available4;
    }
    public boolean getAvailables() {
        return (this.available4 && this.available3 && this.available2 && this.available1);
    }

    public static synchronized WanderAzimuth getInstance(){
        if(instance==null){
            instance = new WanderAzimuth();
        }
        return instance;
    }
}
