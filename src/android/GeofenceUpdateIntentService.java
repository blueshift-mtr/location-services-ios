package com.blueshift.cordova.location;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.app.IntentService;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import android.app.PendingIntent;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationStatusCodes;
import com.google.android.gms.location.GeofenceStatusCodes;

import com.google.android.gms.location.GeofencingApi;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.maps.model.LatLng;


public class GeofenceUpdateIntentService
extends IntentService {
    
    
    protected static final String TAG = "GeofenceUpdateIntentService";
    
    private ToneGenerator toneGenerator;
    
    
    public GeofenceUpdateIntentService() {
        // Use the TAG to name the worker thread.
        super(TAG);
        Log.i(TAG, "CONSTRUCT GeofenceUpdateIntentService");
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
    }
    
    
    
    /**
     * Handles incoming intents.
     * @param intent sent by Location Services. This Intent is provided to Location
     *               Services (inside a PendingIntent) when addGeofences() is called.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        
        try {
            JSONArray jsonFences = new JSONArray(intent.getStringExtra("fences"));
            Log.e(TAG, "GOT INTENT EXTRA?" + intent.getStringExtra("fences"));
            
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
            
            for (Geofence geofence : triggeringGeofences) {
                for(int i = 0; i < jsonFences.length(); i++) {
                    Log.i(TAG, "Did We get a match?" + jsonFences.getJSONObject(i).getString("name") + " " + geofence.getRequestId());
                    if(geofence.getRequestId().equals(jsonFences.getJSONObject(i).getString("name"))) {
                        Log.i(TAG, "Match!");
                        POST(jsonFences.getJSONObject(i).getString("url"), jsonFences.getJSONObject(i));
                    }
                }
            }
        } catch(JSONException e) {
            e.printStackTrace();
        }
        
        if (geofencingEvent.hasError()) {
            String errorMessage = getErrorString(this,
                                                 geofencingEvent.getErrorCode());
            Log.e(TAG, errorMessage);
            return;
        }
        
        Log.i(TAG, "HANDLING INTENT" + geofencingEvent);
        
        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        
        Toast.makeText(this, "HAY WE TRIGGERED SOMETHIN", Toast.LENGTH_LONG).show();
        
        //startTone("dialtone");
        
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
            geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            
            // Get the geofences that were triggered. A single event can trigger multiple geofences.
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
            
            // Get the transition details as a String.
            String geofenceTransitionDetails = getGeofenceTransitionDetails(
                                                                            this,
                                                                            geofenceTransition,
                                                                            triggeringGeofences
                                                                            );
            
            // Send notification and log the transition details.
            //sendNotification(geofenceTransitionDetails);
            Toast.makeText(this, geofenceTransitionDetails, Toast.LENGTH_LONG).show();
            Log.i(TAG, geofenceTransitionDetails);
        } else {
            // Log the error.
            Log.e(TAG, "INVIALID:" + geofenceTransition);
        }
    }
    
    private String getGeofenceTransitionDetails(
                                                Context context,
                                                int geofenceTransition,
                                                List<Geofence> triggeringGeofences) {
        
        String geofenceTransitionString = getTransitionString(geofenceTransition);
        
        // Get the Ids of each geofence that was triggered.
        ArrayList triggeringGeofencesIdsList = new ArrayList();
        for (Geofence geofence : triggeringGeofences) {
            triggeringGeofencesIdsList.add(geofence.getRequestId());
        }
        String triggeringGeofencesIdsString = TextUtils.join(", ",  triggeringGeofencesIdsList);
        
        return geofenceTransitionString + ": " + triggeringGeofencesIdsString;
    }
    
    private String getTransitionString(int transitionType) {
        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                return "ENTERED";
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                return "EXITED";
            default:
                return "DWELL";
        }
    }
    
    public String getErrorString(Context context, int errorCode) {
        Resources mResources = context.getResources();
        switch (errorCode) {
            case GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE:
                return "GEOFENCE NOT AVAILABLE";
            case GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES:
                return "TOO MANY GEOFENCES";
            case GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS:
                return "TOO MANY PENDING CALLBACKS";
            default:
                return "UNKNOWN ERROR";
        }
    }
    
    private Boolean POST(String url, JSONObject data) {
        try {
            Log.e(TAG, "POSTING TO SERVER");
            if(data == null) {
                data = new JSONObject();
            }
            
            HttpClient http = getTolerantClient(url);
            HttpPost request = new HttpPost(url);
            
            
            StringEntity se = new StringEntity(data.toString());
            request.setEntity(se);
            request.setHeader("Accept", "application/json");
            request.setHeader("Content-type", "application/json");
            
            Log.d(TAG, "Posting to " + request.getURI().toString());
            
            HttpResponse response = http.execute(request);
            
            Log.i(TAG, "Response received: " + response.getStatusLine());
            
            int res = response.getStatusLine().getStatusCode();
            
            
        } catch( Exception e) {
            Log.e(TAG, "ERROR POSTING TO SERVER" + e.toString());
        }
        return true;
    }
    
    public DefaultHttpClient getTolerantClient(String url) {
        DefaultHttpClient client = new DefaultHttpClient();
        if(!(url.substring(0, 5)).equals("https")) {
            return client;
        }
        
        HostnameVerifier hostnameVerifier = org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
        
        SchemeRegistry registry = new SchemeRegistry();
        SSLSocketFactory socketFactory = SSLSocketFactory.getSocketFactory();
        socketFactory.setHostnameVerifier((X509HostnameVerifier) hostnameVerifier);
        registry.register(new Scheme("https", socketFactory, 443));
        SingleClientConnManager mgr = new SingleClientConnManager(client.getParams(), registry);
        DefaultHttpClient httpClient = new DefaultHttpClient(mgr, client.getParams());
        
        // Set verifier
        HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);
        
        return httpClient;
    }
    
    public boolean isConnected(){
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Activity.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected())
            return true;
        else
            return false;
    }
    
    /**
     * Plays debug sound
     * @param name
     */
    private void startTone(String name) {
        int tone = 0;
        int duration = 1000;
        
        if (name.equals("beep")) {
            tone = ToneGenerator.TONE_PROP_BEEP;
        } else if (name.equals("beep_beep_beep")) {
            tone = ToneGenerator.TONE_CDMA_CONFIRM;
        } else if (name.equals("long_beep")) {
            tone = ToneGenerator.TONE_CDMA_ABBR_ALERT;
        } else if (name.equals("doodly_doo")) {
            tone = ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE;
        } else if (name.equals("chirp_chirp_chirp")) {
            tone = ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD;
        } else if (name.equals("dialtone")) {
            tone = ToneGenerator.TONE_SUP_RINGTONE;
        }
        toneGenerator.startTone(tone, duration);
    }
    
}