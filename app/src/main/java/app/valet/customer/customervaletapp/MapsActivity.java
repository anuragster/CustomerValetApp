package app.valet.customer.customervaletapp;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import app.valet.customer.customervaletapp.widget.state.SearchBoxState;

public class MapsActivity extends FragmentActivity /*, PlaceSelectionListener */{

    private static final String TAG = "TAG";
    private final String IS_PLACE_SELECTED_NAME = "isPlaceSelected";
    private final String SEARCH_BOX_STATE_NAME = "searchBoxState";
    private final String LAST_LOCATION_ADDRESS_NAME = "mLastLocationAddress";
    private int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;
    private int MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 2;
    private GoogleApiClient mGoogleApiClient;
    private GoogleApiClient.ConnectionCallbacks callbacks;
    private GoogleApiClient.OnConnectionFailedListener connectionFailedListener;
    public GoogleMap map;
    //private LocationCoordinator locationManager;
    public TextView mTextView;
    private Button mParkNow;
    private ImageView myLocation;
    public boolean isPlaceSelected; // Saved state
    //protected Location mLastLocation;
    protected String mLastLocationAddress; // Saved state
    private Place mPlaceSelected;
    //private Location mUserSelectedLocation;
    public SearchBoxState currentSearchBoxState; // Saved state
    private Location textBoxLocation;
    //private String mAddressOutput;
    public AddressResultReceiver mResultReceiver;
    private LocationListener listener;
    private boolean isMapScreenLoaded;
    private Activity mapActivity;

    // GCM starts
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private boolean isReceiverRegistered;
    // GCM ends


    class AddressResultReceiver extends ResultReceiver {
        private Creator CREATOR;
        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            // Display the address string
            // or an error message sent from the intent service.
            String addressOutput = resultData.getString(Constants.RESULT_DATA_KEY);
            //displayAddressOutput();
            //Log.e("LOGCAT", "mAddressOutput - " + mAddressOutput);

            // Show a toast message if an address was found.
            if (resultCode == Constants.SUCCESS_RESULT) {
                //showToast(getString(R.string.address_found));
                //Log.e("LOGCAT", "mAddressOutput Success - " + addressOutput);
                ((MapsActivity) mapActivity).mLastLocationAddress = addressOutput;
                ((MapsActivity) mapActivity).mTextView.setText(addressOutput);
                ((MapsActivity) mapActivity).currentSearchBoxState.setAddress(addressOutput);
            }

        }
    }


    private void initMapUI(){
        //setContentView(R.layout.activity_maps);
        this.mTextView = (TextView) findViewById(R.id.email_address);
        this.mTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchGoogleMapSearchOverlay();
            }
        });
        this.mParkNow = (Button) findViewById(R.id.park_now);
        this.mParkNow.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Log.e(TAG, "Button Clicked. Parameters - " + currentSearchBoxState);
                (new ParkNowClickHTTPPost()).execute();
            }
        });
        updateUI();
        this.myLocation = (ImageView) findViewById(R.id.my_location);
        this.myLocation.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Log.e(TAG, "My Location clicked!!");
                isPlaceSelected = false;
                if(checkPermission()){
                    Location location = LocationCoordinator.getInstance(mapActivity).mLastLocation;
                    if(location!=null) {
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 15));
                        mTextView.setText(mLastLocationAddress);
                    }else{
                        Log.e(TAG, "mLastLocation is null!!");
                    }
                    LocationCoordinator.getInstance(mapActivity).requestLocationUpdates();
                }
            }
        });
        isMapScreenLoaded = true;
    }

    private void handleLocationChanged(Location location){
        if(location == null){
            Log.e(TAG, "Location is null");
            return;
        }
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15);
        map.animateCamera(cameraUpdate);

        currentSearchBoxState = new SearchBoxState(location.getLatitude(), location.getLongitude(), "");
        LocationCoordinator.getInstance(this).startIntentService();
    }

    @TargetApi(23)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "onCreate!!");
        mapActivity = this;
        LocationCoordinator lc = LocationCoordinator.getInstance(this);

        //setContentView(R.layout.activity_splash);
        setContentView(R.layout.activity_maps);
        initMapUI();
        mResultReceiver = new AddressResultReceiver(new Handler());
        map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        //mResultReceiver = new AddressResultReceiver(new Handler());
        //locationManager = (LocationCoordinator) getSystemService(Context.LOCATION_SERVICE);

        //Toast.makeText(getApplicationContext(), "test", 1000).show();

        updateValuesFromBundle(savedInstanceState);
        //this.listener = this;

        // GCM starts
        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //mRegistrationProgressBar.setVisibility(ProgressBar.GONE);
                SharedPreferences sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(context);
                boolean sentToken = sharedPreferences
                        .getBoolean(QuickstartPreferences.SENT_TOKEN_TO_SERVER, false);
                if (sentToken) {
                    Log.e(TAG, "Token has been sent!!");
                } else {
                    Log.e(TAG, "Token has not been sent!!");
                }
            }
        };
        registerReceiver();
        if (checkPlayServices()) {
            // Start IntentService to register this application with GCM.
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
        }
        // GCM ends

        // Initializing google API client
        /*if(callbacks == null){
            callbacks = new GoogleApiClient.ConnectionCallbacks() {
                @Override
                public void onConnected(@Nullable Bundle bundle) {
                    Log.e(TAG, "onConnected");
                    if(checkPermission()) {
                        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                                mGoogleApiClient);
                        if (mLastLocation != null) {
                            app.valet.customer.customervaletapp.LocationCoordinator.getInstance(mapActivity).handleLocationChanged();
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
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(callbacks)
                    .addOnConnectionFailedListener(connectionFailedListener)
                    .addApi(LocationServices.API)
                    .build();
        }*/
        handleLocationChanged(lc.getmLastLocation());
    }

    private void updateValuesFromBundle(Bundle savedInstanceState){
        Log.e(TAG, "updateValues is called!!");
        if(savedInstanceState!=null){
            Log.e(TAG, "savedInstance state is not null");
            if(savedInstanceState.keySet().contains(IS_PLACE_SELECTED_NAME)){
                isPlaceSelected = savedInstanceState.getBoolean(IS_PLACE_SELECTED_NAME);
                Log.e(TAG, "Retrieved isPlaceSelected - " + isPlaceSelected);
            }
            if(savedInstanceState.keySet().contains(SEARCH_BOX_STATE_NAME)){
                currentSearchBoxState = (SearchBoxState) savedInstanceState.getSerializable(SEARCH_BOX_STATE_NAME);
                Log.e(TAG, "Retrieved searchBoxState - " + currentSearchBoxState);
            }
            if(savedInstanceState.keySet().contains(LAST_LOCATION_ADDRESS_NAME)){
                mLastLocationAddress = savedInstanceState.getString(LAST_LOCATION_ADDRESS_NAME);
                Log.e(TAG, "Retrieved mLastLocationAddress - " + mLastLocationAddress);
            }
            updateUI();
        }
    }

    private void updateUI(){
        if(mTextView!= null && currentSearchBoxState!=null) {
            mTextView.setText(currentSearchBoxState.getAddress());
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        Log.e(TAG, "onSaveInstanceState!!");

        savedInstanceState.putBoolean(IS_PLACE_SELECTED_NAME, isPlaceSelected);
        savedInstanceState.putSerializable(SEARCH_BOX_STATE_NAME, currentSearchBoxState);
        savedInstanceState.putString(LAST_LOCATION_ADDRESS_NAME, mLastLocationAddress);

        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        Log.e(TAG, "checkPlayServices");
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        Log.e(TAG, "ResultCode - " + resultCode);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }


    private void registerReceiver(){
        Log.e(TAG, "registerReceiver");
        if(!isReceiverRegistered) {
            Log.e(TAG, "registerReceiver - Not registered. Registering now.");
            LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                    new IntentFilter(QuickstartPreferences.REGISTRATION_COMPLETE));
            isReceiverRegistered = true;
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        Log.e(TAG, "onResume");
        registerReceiver();
        LocationCoordinator.getInstance(this).requestLocationUpdates();
    }

    @Override
    protected void onPause(){
        super.onPause();
        //LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        //isReceiverRegistered = false;
        Log.e(TAG, "onPause");
        // Stop location updates to save battery power.
        LocationCoordinator.getInstance(this).removeLocationUpdates();
    }

    private void launchGoogleMapSearchOverlay(){
        try {
            Intent intent =
                    new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY)
                            .build(this);
            startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);
        } catch (GooglePlayServicesRepairableException e) {
            // TODO: Handle the error.
        } catch (GooglePlayServicesNotAvailableException e) {
            // TODO: Handle the error.
        }
    }

    private int dpToPx(int dp) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return px;
    }

    @TargetApi(23)
    private boolean checkPermission(){
        if (ContextCompat.checkSelfPermission(MapsActivity.this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(MapsActivity.this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION)) {

                checkSelfPermission("You have to accept to enjoy the most hings in this app");
            } else {
                ActivityCompat.requestPermissions(MapsActivity.this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION);

            }
        }else{
            return true;
        }
        return false;
    }





    // Invoked whenever user searches for a location and selects it.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                this.isPlaceSelected = true;
                LocationCoordinator.getInstance(mapActivity).removeLocationUpdates();
                Place place = PlaceAutocomplete.getPlace(this, data);
                mPlaceSelected = place;
                //Log.e("LOGTAG", "Setting address - " + place.getName());
                //mUserSelectedLocation = new Location();
                mTextView.setText(place.getName());
                currentSearchBoxState.setAddress(place.getName().toString());
                currentSearchBoxState.setLatitude(place.getLatLng().latitude);
                currentSearchBoxState.setLongitude(place.getLatLng().longitude);
                map.addMarker(new MarkerOptions().position(place.getLatLng()).title(place.getName().toString()));
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), 15));
                //Log.e(TAG, "Place: " + place.getName());
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(this, data);
                // TODO: Handle the error.
                Log.e(TAG, status.getStatusMessage());

            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
            }
        }
    }

}
