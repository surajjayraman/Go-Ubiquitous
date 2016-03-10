package com.example.android.sunshine.app.wear;

import android.util.Log;

import com.example.android.sunshine.app.common.Constants;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

public class WatchWeatherListenerService extends WearableListenerService {

    // Logging Identifier for the class
    private static String LOG_TAG = WatchWeatherListenerService.class.getSimpleName();

    private GoogleApiClient mGoogleApiClient;
    // Weather data
    private static String mWeatherTempMax;
    private static String mWeatherTempMin;
    private static int mWeatherId;

    private static boolean alreadyInitialize;
    private static String path;

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(LOG_TAG, "onMessageReceived: " + messageEvent);

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Wearable.API)
                    .build();
        }

        if (!mGoogleApiClient.isConnected())
            mGoogleApiClient.connect();

        DataMap dataMap = DataMap.fromByteArray(messageEvent.getData());

        path = messageEvent.getPath();
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(path);
        DataMap config = putDataMapRequest.getDataMap();

        if (path.equals(Constants.WEATHER_DATA_PATH)) {

            if (dataMap.containsKey(Constants.WEATHER_KEY_ID)) {
                mWeatherId = dataMap.getInt(Constants.WEATHER_KEY_ID);
            }

            if (dataMap.containsKey(Constants.WEATHER_KEY_TEMP_MAX)) {
                mWeatherTempMin = dataMap.getString(Constants.WEATHER_KEY_TEMP_MAX);
            }

            if (dataMap.containsKey(Constants.WEATHER_KEY_TEMP_MIN)) {
                mWeatherTempMax = dataMap.getString(Constants.WEATHER_KEY_TEMP_MIN);
            }

            config.putLong(Constants.WEATHER_KEY_UPDATE_TIME, System.currentTimeMillis());
            config.putInt(Constants.WEATHER_KEY_ID, mWeatherId);
            config.putString(Constants.WEATHER_KEY_TEMP_MAX, mWeatherTempMax);
            config.putString(Constants.WEATHER_KEY_TEMP_MIN, mWeatherTempMin);
        }
    }
}