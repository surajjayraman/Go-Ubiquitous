package com.example.android.sunshine.app.wear;

import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.example.android.sunshine.app.Utility;
import com.example.android.sunshine.app.common.Constants;
import com.example.android.sunshine.app.data.WeatherContract;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

public class WearWeatherListenerService extends WearableListenerService {

    // Logging Identifier for the class
    private static String LOG_TAG = WearWeatherListenerService.class.getSimpleName();

    private GoogleApiClient mGoogleApiClient;
    private String mPeerId;

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
        mPeerId = messageEvent.getSourceNodeId();
        Log.d(LOG_TAG, "MessageReceived: " + messageEvent.getPath());
        if (messageEvent.getPath().equals(Constants.WEATHER_SERVICE_REQUIRE_PATH)) {
            startTask();
        }
    }

    private void startTask() {
        Log.d(LOG_TAG, "Start Weather AsyncTask");
        mGoogleApiClient = new GoogleApiClient.Builder(this).addApi(Wearable.API).build();

        Task task = new Task();
        task.execute();
    }

    private class Task extends AsyncTask {

        @Override
        protected Object doInBackground(Object[] params) {
            try {
                Log.d(LOG_TAG, "Task Running");

                if (!mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.connect();
                }

                DataMap config = new DataMap();
                // Acquire the cursor
                Cursor cursor = getTodaysWeather();

                if (cursor.moveToFirst()) {
                    int weatherId = cursor.getInt(cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID));
                    String maxTemp = Utility.formatTemperature(WearWeatherListenerService.this, cursor.getDouble(cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP)));
                    String minTemp = Utility.formatTemperature(WearWeatherListenerService.this, cursor.getDouble(cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP)));

                    Log.d(LOG_TAG, "weatherId: " + weatherId + " maxTemp: " + maxTemp + " minTemp: " + minTemp);

                    // Real weather data
                    config.putInt(Constants.WEATHER_KEY_ID, weatherId);
                    config.putString(Constants.WEATHER_KEY_TEMP_MAX, maxTemp);
                    config.putString(Constants.WEATHER_KEY_TEMP_MIN, minTemp);

                    // Fake weather data
                    //Random random = new Random();
                    //config.putInt("Temperature",random.nextInt(100));
                    //config.putString("Condition", new String[]{"clear","rain","snow","thunder","cloudy"}[random.nextInt
                    // (4)]);
                }
                cursor.close();

                Wearable.MessageApi.sendMessage(mGoogleApiClient, mPeerId, Constants.WEATHER_DATA_PATH, config.toByteArray())
                        .setResultCallback(
                                new ResultCallback<MessageApi.SendMessageResult>() {
                                    @Override
                                    public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                                        Log.d(LOG_TAG, "SendUpdateMessage: " + sendMessageResult.getStatus());
                                    }
                                }
                        );
            } catch (Exception e) {
                Log.d(LOG_TAG, "Task Fail: " + e);
            }
            return null;
        }
    }

    private void refreshWearableData() {
        // Acquire the cursor
        Cursor cursor = getTodaysWeather();

        if (cursor.moveToFirst()) {
            int weatherId = cursor.getInt(cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID));
            String maxTemp = Utility.formatTemperature(this, cursor.getDouble(cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP)));
            String minTemp = Utility.formatTemperature(this, cursor.getDouble(cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP)));

            Log.d(LOG_TAG, "weatherId: " + weatherId + " maxTemp: " + maxTemp + " minTemp: " + minTemp);


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

}
