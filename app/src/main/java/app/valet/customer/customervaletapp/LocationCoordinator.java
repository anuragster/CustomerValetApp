package app.valet.customer.customervaletapp;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;

import app.valet.customer.customervaletapp.widget.state.SearchBoxState;

/**
 * Created by ASAX on 28-05-2016.
 */
public class LocationCoordinator implements LocationListener{
    private static final String TAG = "TAG";
    private static final long MIN_TIME = 0/*400*/;
    private static final float MIN_DISTANCE = 0/*1000*/;
    private int MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 2;
    private GoogleApiClient mGoogleApiClient;
    private GoogleApiClient.ConnectionCallbacks callbacks;
    private GoogleApiClient.OnConnectionFailedListener connectionFailedListener;
    private android.location.LocationManager locationManager;
    protected Location mLastLocation;

    public LocationListener listener;
    private Activity mapActivity;
    private static volatile LocationCoordinator INSTANCE;

    public boolean isLocationSet(){
        return mLastLocation!=null;
    }

    public void requestLocationUpdates(){
        if(checkPermission()) {
            locationManager.requestLocationUpdates(android.location.LocationManager.NETWORK_PROVIDER, MIN_TIME, MIN_DISTANCE, this);
            locationManager.requestLocationUpdates(android.location.LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DISTANCE, this);
        }
    }

    public void removeLocationUpdates(){
        if(checkPermission()) {
            locationManager.removeUpdates(this);
            locationManager.removeUpdates(this);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) { }

    @Override
    public void onProviderEnabled(String provider) {
        Log.e(TAG, "Provider enabled");
        Toast.makeText(mapActivity.getApplicationContext(), "Provider enabled", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.e(TAG, "Provider disabled");
        Toast.makeText(mapActivity.getApplicationContext(), "Provider disabled", Toast.LENGTH_SHORT).show();
    }

    @TargetApi(23)
    @Override
    public void onLocationChanged(Location location) {
        if(mapActivity!=null && mapActivity instanceof MapsActivity) {
            if (((MapsActivity) mapActivity).isPlaceSelected) {
                return;
            }
        }
        //Log.e(TAG, "Location update received!!");
//        if(!isMapScreenLoaded){
//            initMapUI();
//        }
        mLastLocation = location;
        handleLocationChanged();
    }

    public Location getmLastLocation(){
        return mLastLocation;
    }

    public void handleLocationChanged(){
        if(mLastLocation == null){
            Log.e(TAG, "mLastLocation is null!!");
            return;
        }
        LatLng latLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
        if(mapActivity!=null && mapActivity instanceof MapsActivity) {
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15);
            ((MapsActivity) mapActivity).map.animateCamera(cameraUpdate);

            ((MapsActivity) mapActivity).currentSearchBoxState = new SearchBoxState(mLastLocation.getLatitude(), mLastLocation.getLongitude(), "");
            startIntentService();
        }
        //Log.e(TAG, "Location changed. Starting intent service.");

        //this.mTextView.setText("address");
    }

    @TargetApi(23)
    private boolean checkPermission(){
        if (ContextCompat.checkSelfPermission(mapActivity,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(mapActivity,
                    android.Manifest.permission.ACCESS_FINE_LOCATION)) {

                mapActivity.checkSelfPermission("You have to accept to enjoy the most hings in this app");
            } else {
                ActivityCompat.requestPermissions(mapActivity,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION);

            }
        }else{
            return true;
        }
        return false;
    }

    private LocationCoordinator(Activity activity){
        init(activity);
    }

    public static LocationCoordinator getInstance(Activity activity){
        if(INSTANCE == null){
            synchronized (LocationCoordinator.class){
                if(INSTANCE == null){
                    INSTANCE = new LocationCoordinator(activity);
                }
            }
        }
        INSTANCE.mapActivity = activity;
        return INSTANCE;
    }

    protected void startIntentService() {
        Intent intent = new Intent(mapActivity, FetchAddressIntentService.class);
        intent.putExtra(Constants.RECEIVER, ((MapsActivity)mapActivity).mResultReceiver);
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, mLastLocation);
        ((MapsActivity)mapActivity).startService(intent);
    }

    private void init(Activity activity){
        //map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        mapActivity = activity;
        //mResultReceiver = new AddressResultReceiver(new Handler());
        locationManager = (android.location.LocationManager) mapActivity.getSystemService(Context.LOCATION_SERVICE);
        this.listener = this;
        requestLocationUpdates();
        // Initializing google API client
        if(callbacks == null){
            callbacks = new GoogleApiClient.ConnectionCallbacks() {
                @Override
                public void onConnected(@Nullable Bundle bundle) {
                    Log.e(TAG, "onConnected");
                    if(checkPermission()) {
                        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                                mGoogleApiClient);
                        if (mLastLocation != null) {
                            if(mapActivity!=null && mapActivity instanceof MapsActivity) {
                                handleLocationChanged();
                            }
                        }
                    }
                }

                @Override
                public void onConnectionSuspended(int i) {
                    Log.e(TAG, "onConnectionSuspended");
                }
            };
        }

        if(connectionFailedListener == null){
            connectionFailedListener = new GoogleApiClient.OnConnectionFailedListener() {
                @Override
                public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                    Log.e(TAG, "onConnectionFailed");
                }
            };
        }

        if(mGoogleApiClient == null){
            mGoogleApiClient = new GoogleApiClient.Builder(mapActivity)
                    .addConnectionCallbacks(callbacks)
                    .addOnConnectionFailedListener(connectionFailedListener)
                    .addApi(LocationServices.API)
                    .build();
        }

    }
}
