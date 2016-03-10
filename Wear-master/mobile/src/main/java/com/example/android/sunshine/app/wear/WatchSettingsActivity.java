package com.example.android.sunshine.app.wear;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.example.android.sunshine.app.R;
import com.example.android.sunshine.app.common.Constants;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

public class WatchSettingsActivity extends AppCompatActivity implements WatchColorSelectDailog.Listener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    // Logging Identifier for the class
    private static String LOG_TAG = WatchSettingsActivity.class.getSimpleName();

    private static final String TAG_BACKGROUND_COLOUR_CHOOSER = "background_chooser";
    private static final String TAG_DATE_AND_TIME_COLOUR_CHOOSER = "date_time_chooser";
    // To synchronize with the data layer API, we have to firstly connect to it through a GoogleApiClient object
    private GoogleApiClient mGoogleApiClient;
    private WatchFaceConfigurationPreferences mWatchFaceConfigurationPreferences;
    private View mBackgroundColourImagePreview;
    private View mDateAndTimeColourImagePreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        findViewById(R.id.configuration_background_colour).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WatchColorSelectDailog.newInstance(getString(R.string.pick_background_colour))
                        .show(getSupportFragmentManager(), TAG_BACKGROUND_COLOUR_CHOOSER);
            }
        });

        findViewById(R.id.configuration_time_colour).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WatchColorSelectDailog.newInstance(getString(R.string.pick_date_time_colour))
                        .show(getSupportFragmentManager(), TAG_DATE_AND_TIME_COLOUR_CHOOSER);
            }
        });

        mBackgroundColourImagePreview = findViewById(R.id.configuration_background_colour_preview);
        mDateAndTimeColourImagePreview = findViewById(R.id.configuration_date_and_time_colour_preview);
        mWatchFaceConfigurationPreferences = WatchFaceConfigurationPreferences.newInstance(this);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onColourSelected(String colour, String tag) {
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create(Constants.WATCH_FACE_SETTINGS_PATH);

        if (TAG_BACKGROUND_COLOUR_CHOOSER.equals(tag)) {
            mBackgroundColourImagePreview.setBackgroundColor(Color.parseColor(colour));
            mWatchFaceConfigurationPreferences.setBackgroundColour(Color.parseColor(colour));
            putDataMapReq.getDataMap().putString("KEY_BACKGROUND_COLOUR", colour);
        } else {
            mDateAndTimeColourImagePreview.setBackgroundColor(Color.parseColor(colour));
            mWatchFaceConfigurationPreferences.setBackgroundColour(Color.parseColor(colour));
            putDataMapReq.getDataMap().putString("KEY_DATE_TIME_COLOUR", colour);
        }

        Log.d(LOG_TAG, "onColorSelected" + colour);

        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(LOG_TAG, "onConnected");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(LOG_TAG, "onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(LOG_TAG, "onConnectionFailed");
    }
}

