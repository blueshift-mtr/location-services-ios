package com.blueshift.cordova.location;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;

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


import com.google.android.gms.location.GeofencingRequest;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationServices;

import com.google.android.gms.location.LocationStatusCodes;

import com.google.android.gms.location.GeofencingApi;

import com.google.android.gms.maps.model.LatLng;

import android.content.IntentFilter;
import android.content.BroadcastReceiver;


public class GeofenceUpdateService
extends Service
implements  GoogleApiClient.ConnectionCallbacks,
GoogleApiClient.OnConnectionFailedListener {
    
    private static final String TAG = "GeofenceUpdateService";
    
    protected GoogleApiClient locationClientAPI;
    
    protected ArrayList<Geofence> geofences;
    
    private JSONArray jsonFences;
  
    private PendingIntent geoServicePI;
    
    private static final String STOP_GEOFENCES = "com.blueshift.cordova.location.STOP_GEOFENCES";
    
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        Log.e(TAG, "GeofenceUpdateService onCreate");
        
        geoServicePI = null;
        geofences = new ArrayList<Geofence>();
        
        registerReceiver(stopGeofenceServiceReceiver, new IntentFilter(STOP_GEOFENCES));
        
    }
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            String source = "intent";
            Log.e (TAG, source + " was null, flags=" + flags + " bits=" + Integer.toBinaryString (flags));
            return START_STICKY;
        }
        
        Log.e(TAG, "GeofenceUpdateService onStartCommand");
        
        try {
            jsonFences = new JSONArray(intent.getStringExtra("fences"));
        } catch(JSONException e) {
            e.printStackTrace();
        }
        
       
        initGeofences();
        
        connectToPlayAPI();
        
        return START_STICKY;
    }
    
    protected synchronized void connectToPlayAPI() {
        Log.d(TAG, "- Geofence, connecting to GAPI");;
        locationClientAPI =  new GoogleApiClient.Builder(this)
        .addApi(LocationServices.API)
        .addConnectionCallbacks(this)
        .addOnConnectionFailedListener(this)
        .build();
        locationClientAPI.connect();
    }
    
    
    private boolean initGeofences() {
        
        for(int i = 0; i < jsonFences.length(); i++) {
            try {
                
                JSONObject f = jsonFences.getJSONObject(i);
                
                JSONObject coord = f.getJSONObject("coordinate");
                
                int dur = f.getInt("expDuration");
                if(dur == 0) {
                    dur = -1;
                }
                
                Log.e(TAG, "Setting Up Fence" + f.getString("name") + " " + f.getInt("expDuration") + " " + coord.getDouble("latitude") + " " + coord.getDouble("longitude") + " " + f.getInt("radius"));
                
                Geofence g = new Geofence.Builder()
                .setRequestId(f.getString("name"))
                .setExpirationDuration(dur)
                .setCircularRegion(coord.getDouble("latitude"),  coord.getDouble("longitude"), f.getInt("radius"))
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();
                
                geofences.add(g);
            } catch(JSONException e) {
                e.printStackTrace();
            }
        }
        
        Log.e(TAG, "Building Geofences - OK");
        
        return true;
    }
    
    
    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        Log.e(TAG, "Building Pending Intent...");
        if (geoServicePI != null) {
            return geoServicePI;
        }
        Intent intent = new Intent(this, GeofenceUpdateIntentService.class);
        
        intent.putExtra("fences", jsonFences.toString());
        
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
    
    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        
        // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
        // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
        // is already inside that geofence.
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        
        // Add the geofences to be monitored by geofencing service.
        builder.addGeofences(geofences);
        
        // Return a GeofencingRequest.
        return builder.build();
    }
    
    private void buildGeofences() {
        if(!locationClientAPI.isConnected()) {
            Toast.makeText(this, "You didnt connect!", Toast.LENGTH_SHORT).show();
            return;
        }
        
        LocationServices.GeofencingApi.addGeofences(
                                                    locationClientAPI,
                                                    getGeofencingRequest(),
                                                    getGeofencePendingIntent()
                                                    ).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status status) {
                if (status.isSuccess()) {
                    
                    Log.e(TAG, "Succesfully added geofences!");
                    
                    /*Location loc = LocationServices.FusedLocationApi.getLastLocation(locationClientAPI);
                     if(loc != null) {
                     LocationServices.FusedLocationApi.setMockMode(locationClientAPI, true);
                     LocationServices.FusedLocationApi.setMockLocation(locationClientAPI, loc);
                     }*/
                } else {
                    // Get the status code for the error and log it using a user-friendly message.
                    /* String errorMessage = GeofenceErrorMessages.getErrorString(this,
                     status.getStatusCode());*/
                    Log.e(TAG, "ERROR" + status);
                }
            }
        });
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        Log.i(TAG, "OnBind" + intent);
        return null;
    }
    
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "Conntected To Geofence API");
        buildGeofences();
        
    }
    
    @Override
    public void onConnectionFailed(com.google.android.gms.common.ConnectionResult result) {
        Log.e(TAG, "We failed to connect to the Geo API!" + result);
        Toast.makeText(this, "COULDNT CONNECT, TRY AGAIN", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onConnectionSuspended(int cause) {
        // locationClientAPI.connect();
    }
    
    
    @Override
    public boolean stopService(Intent intent) {
        Log.i(TAG, "- Received stop: " + intent);
        this.cleanUp();
        Toast.makeText(this, "Removed Locations", Toast.LENGTH_SHORT).show();
        
        return super.stopService(intent);
    }
    
    @Override
    public void onDestroy() {
        Log.w(TAG, "------------------------------------------ Destroyed Geofence update Service");
        this.cleanUp();
        super.onDestroy();
    }
    
    private void cleanUp() {
        // this.disable();
        Log.i(TAG, "Removing geofences");
        unregisterReceiver(stopGeofenceServiceReceiver);
        LocationServices.GeofencingApi.removeGeofences(locationClientAPI, getGeofencePendingIntent());
        
    }
    
    //@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
    }
    
    private BroadcastReceiver stopGeofenceServiceReceiver = new BroadcastReceiver() {
          @Override
          public void onReceive(Context context, Intent intent) {
              Log.i(TAG, "RECIEVED BROADCAST FROM" + context + " WITH INTENT" + intent);
              Log.i(TAG, "Killing geofence service... got kill switch");
              cleanUp();
          }
          
    };
}