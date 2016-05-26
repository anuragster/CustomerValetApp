package app.valet.customer.customervaletapp;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
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
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
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

public class MapsActivity extends FragmentActivity implements LocationListener/*, PlaceSelectionListener */{
    private int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;
    private int MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 2;
    private GoogleMap map;
    private LocationManager locationManager;
    private static final long MIN_TIME = 0/*400*/;
    private static final float MIN_DISTANCE = 0/*1000*/;
    private static final String TAG = "TAG";
    private TextView textView;
    private Button parkNow;
    private ImageView myLocation;
    private boolean isPlaceSelected;
    protected Location mLastLocation;
    protected String mLastLocationAddress;
    //private Location mUserSelectedLocation;
    private SearchBoxState currentSearchBoxState;
    private Location textBoxLocation;
    //private String mAddressOutput;
    private AddressResultReceiver mResultReceiver;
    private LocationListener listener;

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
                mLastLocationAddress = addressOutput;
                textView.setText(addressOutput);
                currentSearchBoxState.setAddress(addressOutput);
            }

        }
    }

    protected void startIntentService() {
        Intent intent = new Intent(this, FetchAddressIntentService.class);
        intent.putExtra(Constants.RECEIVER, mResultReceiver);
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, mLastLocation);
        startService(intent);
    }


    @TargetApi(23)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        mResultReceiver = new AddressResultReceiver(new Handler());
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        //Toast.makeText(getApplicationContext(), "test", 1000).show();

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
        }else if(map!=null){
            //locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME, MIN_DISTANCE, this); //You can also use LocationManager.GPS_PROVIDER and LocationManager.PASSIVE_PROVIDER
            //map.setMyLocationEnabled(true);
            //map.animateCamera(CameraUpdateFactory.zoomTo(170.0f));
            //map.setPadding(0, dpToPx(480), 0, 0);
        }

        this.textView = (TextView) findViewById(R.id.email_address);
        this.textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchGoogleMapSearchOverlay();
            }
        });
        this.parkNow = (Button) findViewById(R.id.park_now);
        this.parkNow.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Log.e(TAG, "Button Clicked. Parameters - " + currentSearchBoxState);
            }
        });
        this.listener = this;
        this.myLocation = (ImageView) findViewById(R.id.my_location);
        this.myLocation.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Log.e(TAG, "My Location clicked!!");
                isPlaceSelected = false;
                if(checkPermission()){
                    if(mLastLocation!=null) {
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), 15));
                        textView.setText(mLastLocationAddress);
                    }{
                        Log.e(TAG, "mLastLocation is null!!");
                    }
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME, MIN_DISTANCE, listener);
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DISTANCE, listener);
                }
            }
        });
        //launchGoogleMapSearchOverlay();

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
        if(checkPermission()) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME, MIN_DISTANCE, this);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DISTANCE, this);
        }
    }

    @Override
    protected void onPause(){
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        isReceiverRegistered = false;
        super.onPause();
        Log.e(TAG, "onPause");
        if(checkPermission()) {
            locationManager.removeUpdates(this);
            locationManager.removeUpdates(this);
        }
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

    @TargetApi(23)
    @Override
    public void onLocationChanged(Location location) {
        if(this.isPlaceSelected){
            return;
        }
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15);
        map.animateCamera(cameraUpdate);
        mLastLocation = location;
        currentSearchBoxState = new SearchBoxState(new LatLng(location.getLatitude(), location.getLongitude()), "");
        //Log.e(TAG, "Location changed. Starting intent service.");
        startIntentService();
        //this.textView.setText("address");
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
        }else if(map!=null) {
            //locationManager.removeUpdates(this);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) { }

    @Override
    public void onProviderEnabled(String provider) {
        Log.e(TAG, "Provider enabled");
        Toast.makeText(getApplicationContext(), "Provider enabled", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.e(TAG, "Provider disabled");
        Toast.makeText(getApplicationContext(), "Provider disabled", Toast.LENGTH_SHORT).show();
    }

    // Invoked whenever user searches for a location and selects it.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                this.isPlaceSelected = true;
                if(this.checkPermission()){
                    locationManager.removeUpdates(this);
                    locationManager.removeUpdates(this);
                }
                Place place = PlaceAutocomplete.getPlace(this, data);
                //Log.e("LOGTAG", "Setting address - " + place.getName());
                //mUserSelectedLocation = new Location();
                textView.setText(place.getName());
                currentSearchBoxState.setAddress(place.getName().toString());
                currentSearchBoxState.setLatLng(place.getLatLng());
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
