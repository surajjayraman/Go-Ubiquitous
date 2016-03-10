package com.example.android.sunshine.app.wear;

import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.android.sunshine.app.Utility;
import com.example.android.sunshine.app.data.WeatherContract;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

public class WatchWeatherIntentService extends IntentService {

    // Logging Identifier for the class
    private static String LOG_TAG = WatchWeatherIntentService.class.getSimpleName();

    public static final String ACTION_SEND_WEAR_DATA = "com.example.android.sunshine.app.ACTION_SEND_WEAR_DATA";
    // Weather Key constants
    private static final String WEATHER_PATH = "/weather";
    private static final String WEATHER_KEY_TEMP_MAX = "weather_temp_max";
    private static final String WEATHER_KEY_TEMP_MIN = "weather_temp_min";
    private static final String WEATHER_KEY_ID = "weather_id";

    // Use this to connect to the wear
    private GoogleApiClient mGoogleApiClient;

    // A callback to update on the status of trying to connect
    GoogleApiClient.OnConnectionFailedListener mOnConnectionFailedListener = new GoogleApiClient.OnConnectionFailedListener() {
        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            Log.d(LOG_TAG, "connectedFailed GoogleAPI");
        }
    };

    // A callback to update on the status of the connection
    GoogleApiClient.ConnectionCallbacks mConnectionCallbacks = new GoogleApiClient.ConnectionCallbacks() {

        @Override
        public void onConnected(@Nullable Bundle bundle) {
            Log.d(LOG_TAG, "connected GoogleAPI");
            // Refresh information about the weather
            refreshWearableData();
        }

        @Override
        public void onConnectionSuspended(int i) {
            Log.e(LOG_TAG, "suspended GoogleAPI");
        }
    };

    public WatchWeatherIntentService() {
        super("WatchWeatherIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null && ACTION_SEND_WEAR_DATA.equals(intent.getAction())) {
            mGoogleApiClient = new GoogleApiClient.Builder(WatchWeatherIntentService.this)
                    .addConnectionCallbacks(mConnectionCallbacks)
                    .addOnConnectionFailedListener(mOnConnectionFailedListener)
                    .addApi(Wearable.API)
                    .build();
            mGoogleApiClient.connect();
           // Refresh Weather data available to Wear device
            refreshWearableData();
            Log.d(LOG_TAG, "Send Wear Data");
        }
    }

    private void refreshWearableData() {
        // Acquire the cursor
        Cursor cursor = getTodaysWeather();

        if (cursor.moveToFirst()) {
            int weatherId = cursor.getInt(cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID));
            String maxTemp = Utility.formatTemperature(this, cursor.getDouble(cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP)));
            String minTemp = Utility.formatTemperature(this, cursor.getDouble(cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP)));
            final PutDataMapRequest requestMap = PutDataMapRequest.create(WEATHER_PATH);
            requestMap.getDataMap().putInt(WEATHER_KEY_ID, weatherId);
            requestMap.getDataMap().putString(WEATHER_KEY_TEMP_MAX, maxTemp);
            requestMap.getDataMap().putString(WEATHER_KEY_TEMP_MIN, minTemp);
            Log.d(LOG_TAG, "weatherId: " + weatherId + " maxTemp: " + maxTemp + " minTemp: " + minTemp);

            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.d(LOG_TAG, "Send PendingResult of DataApi");
                    PendingResult<DataApi.DataItemResult> result = Wearable.DataApi.putDataItem(mGoogleApiClient, requestMap.asPutDataRequest());
                }
            });
            thread.start();
        }
        cursor.close();
    }

    private Cursor getTodaysWeather() {
        // Query Weather location
        String locationQuery = Utility.getPreferredLocation(this);
        // Query  Weather Uri
        Uri weatherUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(locationQuery, System.currentTimeMillis());
        // Query Projection
        String projection[] = new String[]{WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
                WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
                WeatherContract.WeatherEntry.COLUMN_MIN_TEMP
        };
        // Acquire the cursor
        Cursor cursor = getContentResolver().query(weatherUri, projection, null, null, null);

        return cursor;
    }

    @Override
    public void onDestroy() {
        mGoogleApiClient.disconnect();
        super.onDestroy();
    }
}
