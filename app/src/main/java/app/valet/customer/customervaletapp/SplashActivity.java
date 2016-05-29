package app.valet.customer.customervaletapp;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

public class SplashActivity extends Activity {
    private static final String TAG = "TAG";
    /** Duration of wait **/
    private final long SPLASH_DISPLAY_LENGTH = 4000;
    private Activity splashActivity;

    /** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        splashActivity = this;
        Log.e("TAG", "=================SPLASH SCREEN=====================");
        /* New Handler to start the Menu-Activity
         * and close this Splash-Screen after some seconds.*/


        startHeavyProcessing();
        /*try {
            Thread.sleep(SPLASH_DISPLAY_LENGTH);
        }catch(Exception e){}*/
    }

    private void startHeavyProcessing(){
        new LongOperation().execute("");
    }

    private class LongOperation extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            //some heavy processing resulting in a Data String
            /*for (int i = 0; i < 5; i++) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.interrupted();
                }
            }*/
            //return "whatever result you have";
            while(!LocationCoordinator.getInstance(splashActivity).isLocationSet()){
                Log.e(TAG, "Location is not set!!");
                try {
                    Thread.sleep(100);
                }catch(Exception e){

                }
            }
            Log.e(TAG, "Location is set now!!");


            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            Intent i = new Intent(splashActivity, MapsActivity.class);
            //i.putExtra("data", result);
            startActivity(i);
            finish();
        }

        @Override
        protected void onPreExecute() {
            LocationCoordinator.getInstance(splashActivity);
        }

        @Override
        protected void onProgressUpdate(Void... values) {}
    }
}
