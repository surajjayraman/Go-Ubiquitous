package com.example.android.sunshine.app.wear;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.example.android.sunshine.app.R;
import com.example.android.sunshine.app.common.Constants;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * See <a hred="http://developer.android.com/training/wearables/watch-faces/service.html">Building a Watch Face Service</a>
 * See <a hred="http://catinean.com/2015/03/07/creating-a-watch-face-with-android-wear-api/">Creating a Watchface with Android Wear | PART 1</a>
 */
public class DigitalWatchFaceService extends CanvasWatchFaceService {

    // Logging Identifier for the class
    private static String LOG_TAG = DigitalWatchFaceService.class.getSimpleName();

    // Update rate in milliseconds for interactive mode. We update once a second since seconds are
    // displayed in interactive mode.
    private static final long INTERACTIVE_TICK_PERIOD_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    @Override
    public Engine onCreateEngine() {
        // Return the concrete implementation of the Engine
        return new WatchFaceEngine();
    }

    private class WatchFaceEngine extends CanvasWatchFaceService.Engine {

        private static final String KEY_UUID = "uuid";


        // Handler that will post a runnable only if the watch is visible and not in ambient mode in order to start ticking
        private Handler mTimeTick;
        // Instance of a watch face
        private DigitalWatchFace mDigitalWatchFace;
        // To synchronize with the data layer API, we have to firstly connect to it through a GoogleApiClient object
        private GoogleApiClient mGoogleApiClient;
        // Get notified every time there is a change in the data layer
        private final DataApi.DataListener onDataChangedListener = new DataApi.DataListener() {
            @Override
            public void onDataChanged(DataEventBuffer dataEvents) {
                // Iterate through all data events
                for (DataEvent event : dataEvents) {
                    // Look only for DataEvent.TYPE_CHANGED events
                    if (event.getType() == DataEvent.TYPE_CHANGED) {
                        DataItem item = event.getDataItem();
                        // Discriminate between unique paths
                        if (Constants.WATCH_FACE_SETTINGS_PATH.equals(item.getUri().getPath())) {
                            processConfigurationChange(item);
                        }
                        if (Constants.WEATHER_DATA_PATH.equals(item.getUri().getPath())) {
                            processWeatherData(item);
                        }
                    } else if (event.getType() == DataEvent.TYPE_DELETED) {
                        // DataItem deleted
                    }
                }

                dataEvents.release();
                invalidateIfNecessary();
            }

        };

        // Only notified when the service is firstly connected
        private final ResultCallback<DataItemBuffer> onConnectedResultCallback = new ResultCallback<DataItemBuffer>() {
            @Override
            public void onResult(DataItemBuffer dataItems) {
                for (DataItem item : dataItems) {
                    if (Constants.WATCH_FACE_SETTINGS_PATH.equals(item.getUri().getPath())) {
                        processConfigurationChange(item);
                    }

                }
                dataItems.release();
                invalidateIfNecessary();
            }
        };

        // A callback to update on the status of the connection
        GoogleApiClient.ConnectionCallbacks mConnectionCallbacks = new GoogleApiClient.ConnectionCallbacks() {

            @Override
            public void onConnected(@Nullable Bundle bundle) {
                Log.d(LOG_TAG, "connected GoogleAPI");
                Wearable.DataApi.addListener(mGoogleApiClient, onDataChangedListener);
                Wearable.DataApi.getDataItems(mGoogleApiClient).setResultCallback(onConnectedResultCallback);
                // Request information about watch settings
                requestWatchSettingsInfo();
                // Request information about the weather
                requestWeatherInfo();
            }

            @Override
            public void onConnectionSuspended(int i) {
                Log.e(LOG_TAG, "suspended GoogleAPI");
            }
        };

        // A callback to update on the status of trying to connect
        GoogleApiClient.OnConnectionFailedListener mOnConnectionFailedListener = new GoogleApiClient.OnConnectionFailedListener() {
            @Override
            public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                Log.d(LOG_TAG, "connectedFailed GoogleAPI");
            }
        };

        private void requestWatchSettingsInfo() {
            Wearable.DataApi.getDataItems(mGoogleApiClient).setResultCallback(onConnectedResultCallback);
        }

        private void requestWeatherInfo() {
            PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(Constants.WEATHER_DATA_PATH);
            putDataMapRequest.getDataMap().putString(KEY_UUID, UUID.randomUUID().toString());
            PutDataRequest request = putDataMapRequest.asPutDataRequest();

            Wearable.DataApi.getDataItems(mGoogleApiClient).setResultCallback(onConnectedResultCallback);
            Wearable.DataApi.putDataItem(mGoogleApiClient, request)
                    .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                        @Override
                        public void onResult(DataApi.DataItemResult dataItemResult) {
                            if (!dataItemResult.getStatus().isSuccess()) {
                                Log.d(LOG_TAG, "Failed asking phone for weather data");
                            } else {
                                Log.d(LOG_TAG, "Successfully asked for weather data");
                            }
                        }
                    });
        }

        // Define your watch face style and other graphical elements.
        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            // In defining the watch face style, you can customise how the UI elements such as the battery
            // indicator are drawn over the watch face or how the cards are behaving in both normal and ambient mode.
            setWatchFaceStyle(new WatchFaceStyle.Builder(DigitalWatchFaceService.this)
                    // Specify that the first card peeked and shown on the watch will have a single
                    // line tail (i.e. it will have small height)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    // Watch enters in ambient mode, no peek card will be visible
                    .setAmbientPeekMode(WatchFaceStyle.AMBIENT_PEEK_MODE_HIDDEN)
                    // Background of the peek card should only be shown briefly, and only if the peek card
                    // represents an interruptive notification
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    // Set the UI time to false because we will already show the time on the watch by drawing it onto the canvas
                    .setShowSystemUiTime(false)
                    .build());

            mTimeTick = new Handler(Looper.myLooper());
            startTimerIfNecessary();
            // Initialize the Watch Face
            mDigitalWatchFace = DigitalWatchFace.newInstance(DigitalWatchFaceService.this);
            mDigitalWatchFace.updateBackgroundColourTo(getResources().getColor(R.color.digital_background));
            // Client to synchronise with data API
            mGoogleApiClient = new GoogleApiClient.Builder(DigitalWatchFaceService.this)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(mConnectionCallbacks)
                    .addOnConnectionFailedListener(mOnConnectionFailedListener)
                    .build();
        }

        private void startTimerIfNecessary() {
            mTimeTick.removeCallbacks(timeRunnable);
            if (isVisible() && !isInAmbientMode()) {
                mTimeTick.post(timeRunnable);
            }
        }

        // Actual runnable posted by mTimeTick handler. It invalidates the watch and schedules
        // another run of itself on the handler with a delay of one second (since we want to tick every second) if necessary
        private final Runnable timeRunnable = new Runnable() {
            @Override
            public void run() {
                onSecondTick();

                if (isVisible() && !isInAmbientMode()) {
                    mTimeTick.postDelayed(this, INTERACTIVE_TICK_PERIOD_UPDATE_RATE_MS);
                }
            }
        };

        private void onSecondTick() {
            invalidateIfNecessary();
        }

        private void invalidateIfNecessary() {
            if (isVisible() && !isInAmbientMode()) {
                invalidate();
            }
        }

        // Called when the watch becomes visible or not.
        @Override
        public void onVisibilityChanged(boolean visible) {
            //Must call super() first
            super.onVisibilityChanged(visible);
            if (visible) {
                // Engage client when WatchFace is visible
                mGoogleApiClient.connect();
                Log.d(LOG_TAG, "GoogleApiClent Connection Requested");

            } else {
                // Free client when the watch face is not visible anymore.
                releaseGoogleApiClient();
            }
            startTimerIfNecessary();
        }

        // Lets draw the DigitalWatchFace
        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            super.onDraw(canvas, bounds);
            mDigitalWatchFace.draw(canvas, bounds);
        }

        // Release connection when not needed
        private void releaseGoogleApiClient() {
            if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                Wearable.DataApi.removeListener(mGoogleApiClient, onDataChangedListener);
                mGoogleApiClient.disconnect();
                Log.d(LOG_TAG, "GoogleApiClent Connection Disconnected");
            }
        }

        // Called when the device enters or exits ambient mode. While on ambient mode, one should be considerate
        // to preserve battery consumption by providing a black and white display and not provide any animation such as displaying seconds.
        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            Log.d(LOG_TAG, "Enter Ambient Mode");
            // Battery performance optimizations
            mDigitalWatchFace.setAntiAlias(!inAmbientMode);
            // We set the color to gray in Ambien
            mDigitalWatchFace.setColor(inAmbientMode ? Color.GRAY : Color.WHITE);
            // Hide seconds in order to minimize the amount of animations (draws)
            mDigitalWatchFace.setShowSeconds(!isInAmbientMode());
            // In ambient mode, we would want to default to black and white colours
            if (inAmbientMode) {
                mDigitalWatchFace.updateBackgroundColourToDefault();
                mDigitalWatchFace.updateDateAndTimeColourToDefault();
            } else {
                // Restore its colours to the selected ones
                mDigitalWatchFace.restoreBackgroundColour();
                mDigitalWatchFace.restoreDateAndTimeColour();
            }
            invalidate();
            startTimerIfNecessary();
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);
            mDigitalWatchFace.setWindowInsets(insets);
        }

        // Callback is invoked every minute when the watch is in ambient mode. It is very important to consider that this callback is only
        // invoked while on ambient mode, as it's name is rather confusing suggesting that this callbacks every time.
        @Override
        public void onTimeTick() {
            super.onTimeTick();
            // Above being said, usually, here we will have only to invalidate() the watch in order to trigger onDraw(). In order to keep track
            // of time outside ambient mode, we will have to provide our own mechanism.
            invalidate();
        }

        private void processConfigurationChange(DataItem item) {
            //  Acquire
            DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
            // Upgrade watch settings
            mDigitalWatchFace.updateConfigurationChanges(dataMap);
            invalidate();
        }

        private void processWeatherData(DataItem item) {
            //  Acquire
            DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
            // Update the weather data
            mDigitalWatchFace.updateWeatherData(dataMap);
            invalidate();
        }

        @Override
        public void onDestroy() {
            mTimeTick.removeCallbacks(timeRunnable);
            releaseGoogleApiClient();
            super.onDestroy();
        }
    }
}
