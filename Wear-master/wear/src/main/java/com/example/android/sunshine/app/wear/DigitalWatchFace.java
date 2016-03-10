package com.example.android.sunshine.app.wear;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.format.Time;
import android.util.Log;
import android.view.WindowInsets;

import com.example.android.sunshine.app.R;
import com.example.android.sunshine.app.Utility;
import com.example.android.sunshine.app.common.Constants;
import com.google.android.gms.wearable.DataMap;

/**
 * See <a hred="http://developer.android.com/training/wearables/watch-faces/service.html">Building a Watch Face Service</a>
 * See <a hred="http://catinean.com/2015/03/07/creating-a-watch-face-with-android-wear-api/">Creating a Watchface with Android Wear | PART 1</a>
 */
public class DigitalWatchFace {

    // Logging Identifier for the class
    private static String LOG_TAG = DigitalWatchFace.class.getSimpleName();

    private static final String TIME_FORMAT_WITHOUT_SECONDS = "%02d.%02d";
    private static final String TIME_FORMAT_WITH_SECONDS = TIME_FORMAT_WITHOUT_SECONDS + ".%02d";
    private static final String DATE_FORMAT = "%02d.%02d.%d";

    // Font types
    private static final Typeface NORMAL_TYPEFACE = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
    private static final Typeface BOLD_TYPEFACE = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);

    // Background Paint
    private final Paint backgroundPaint;
    // Weather paints
    private final Paint mTextTempHighPaint;
    private final Paint mTextTempLowPaint;
    // Time-Date paints
    private final Paint mTextTimePaint;
    private final Paint mTextDatePaint;
    private final Time mTime;

    // Weather display fields
    private Bitmap mWeatherIcon;
    private String mWeatherHigh;
    private String mWeatherLow;

    // Insets Dimensions
    private float mDateYOffset;
    private float mDividerYOffset;
    private float mWeatherYOffset;

    private static final int DATE_AND_TIME_DEFAULT_COLOUR = Color.WHITE;
    private static final int BACKGROUND_DEFAULT_COLOUR = Color.BLACK;

    private int backgroundColour = BACKGROUND_DEFAULT_COLOUR;
    private int dateAndTimeColour = DATE_AND_TIME_DEFAULT_COLOUR;

    private boolean shouldShowSeconds = true;
    private static Resources resources;

    public static DigitalWatchFace newInstance(Context context) {
        // Acquire Resources object
        resources = context.getResources();

        Paint timePaint = new Paint();
        timePaint.setTextSize(context.getResources().getDimension(R.dimen.time_size));
        // Will differentiate the drawing between interactive mode and ambient mode
        timePaint.setColor(DATE_AND_TIME_DEFAULT_COLOUR);
        timePaint.setAntiAlias(true);

        Paint datePaint = new Paint();
        datePaint.setTextSize(context.getResources().getDimension(R.dimen.date_size));
        // Will differentiate the drawing between interactive mode and ambient mode
        datePaint.setColor(DATE_AND_TIME_DEFAULT_COLOUR);
        datePaint.setAntiAlias(true);

        // Set the default canvas background color
        Paint backgroundPaint = new Paint();
        backgroundPaint.setColor(BACKGROUND_DEFAULT_COLOUR);

        // Set color of High Temperature
        Paint textTempHighPaint = new Paint();
        textTempHighPaint.setColor(Color.WHITE);
        textTempHighPaint.setTypeface(BOLD_TYPEFACE);
        textTempHighPaint.setAntiAlias(true);

        // Set color of Low Temperature
        Paint textTempLowPaint = new Paint();
        textTempLowPaint.setColor(resources.getColor(R.color.primary_light));
        textTempLowPaint.setTypeface(NORMAL_TYPEFACE);
        textTempLowPaint.setAntiAlias(true);

        return new DigitalWatchFace(timePaint, datePaint, backgroundPaint, textTempHighPaint, textTempLowPaint, new Time());
    }

    DigitalWatchFace(Paint timePaint, Paint datePaint, Paint backgroundPaint, Paint textTempHighPaint, Paint textTempLowPaint, Time time) {
        this.mTextTimePaint = timePaint;
        this.mTextDatePaint = datePaint;
        this.mTextTempHighPaint = textTempHighPaint;
        this.mTextTempLowPaint = textTempLowPaint;
        this.backgroundPaint = backgroundPaint;
        this.mTime = time;
    }

    // Perform all the drawing operations on the canvas.
    public void draw(Canvas canvas, Rect bounds) {
        // Place the mTime to the current mTime
        mTime.setToNow();
        // Set the background color of canvas
        canvas.drawRect(0, 0, bounds.width(), bounds.height(), backgroundPaint);

        String timeText = String.format(shouldShowSeconds ? TIME_FORMAT_WITH_SECONDS : TIME_FORMAT_WITHOUT_SECONDS, mTime.hour, mTime.minute, mTime.second);
        float timeXOffset = computeXOffset(timeText, mTextTimePaint, bounds);
        float timeYOffset = computeTimeYOffset(timeText, mTextTimePaint, bounds);
        // Draw the mTime
        canvas.drawText(timeText, timeXOffset, timeYOffset, mTextTimePaint);

        String dateText = String.format(DATE_FORMAT, mTime.monthDay, (mTime.month + 1), mTime.year);
        float dateXOffset = computeXOffset(dateText, mTextDatePaint, bounds);
        float dateYOffset = computeDateYOffset(dateText, mTextDatePaint);
        // Draw the date
        canvas.drawText(dateText, dateXOffset, timeYOffset + dateYOffset, mTextDatePaint);

        // Draw high and low temp if we have it
        if (mWeatherHigh != null && mWeatherLow != null && mWeatherIcon != null) {
            Log.d(LOG_TAG, "weatherIcon: " + mWeatherIcon + " maxTemp: " + mWeatherHigh + " minTemp: " + mWeatherLow);
            // Draw a line to separate date and time from weather elements
            canvas.drawLine(bounds.centerX() - 20, mDividerYOffset, bounds.centerX() + 20, mDividerYOffset, mTextDatePaint);

            float highTextLen = mTextTempHighPaint.measureText(mWeatherHigh);

            float xOffset = bounds.centerX() - (highTextLen / 2);
            canvas.drawText(mWeatherHigh, xOffset, mWeatherYOffset, mTextTempHighPaint);
            canvas.drawText(mWeatherLow, bounds.centerX() + (highTextLen / 2) + 20, mWeatherYOffset, mTextTempLowPaint);
            float iconXOffset = bounds.centerX() - ((highTextLen / 2) + mWeatherIcon.getWidth() + 30);
            canvas.drawBitmap(mWeatherIcon, iconXOffset, mWeatherYOffset - mWeatherIcon.getHeight(), null);
        }
    }

    public void setWindowInsets(WindowInsets insets) {

        boolean isRound = insets.isRound();

        mDateYOffset = resources.getDimension(isRound ? R.dimen.date_y_offset_round : R.dimen.date_y_offset);
        mDividerYOffset = resources.getDimension(isRound ? R.dimen.divider_y_offset_round : R.dimen.divider_y_offset);
        mWeatherYOffset = resources.getDimension(isRound ? R.dimen.weather_y_offset_round : R.dimen.weather_y_offset);

        float timeTextSize = resources.getDimension(isRound ? R.dimen.time_text_size_round : R.dimen.time_text_size);
        float dateTextSize = resources.getDimension(isRound ? R.dimen.date_text_size_round : R.dimen.date_text_size);
        float tempTextSize = resources.getDimension(isRound ? R.dimen.temp_text_size_round : R.dimen.temp_text_size);

        mTextTimePaint.setTextSize(timeTextSize);
        mTextDatePaint.setTextSize(dateTextSize);
        mTextTempHighPaint.setTextSize(tempTextSize);
        mTextTempLowPaint.setTextSize(tempTextSize);
    }

    // Helper methods in order to compute the x offset of both mTime and date drawings.
    private float computeXOffset(String text, Paint paint, Rect watchBounds) {
        float centerX = watchBounds.exactCenterX();
        float timeLength = paint.measureText(text);
        return centerX - (timeLength / 2.0f);
    }

    // Helper methods in order to compute the y offset of both mTime drawing.
    private float computeTimeYOffset(String timeText, Paint timePaint, Rect watchBounds) {
        float centerY = watchBounds.exactCenterY();
        Rect textBounds = new Rect();
        timePaint.getTextBounds(timeText, 0, timeText.length(), textBounds);
        int textHeight = textBounds.height();
        return centerY + (textHeight / 2.0f);
    }

    // Helper methods in order to compute the y offset of date drawing.
    private float computeDateYOffset(String dateText, Paint datePaint) {
        Rect textBounds = new Rect();
        datePaint.getTextBounds(dateText, 0, dateText.length(), textBounds);
        return textBounds.height() + 10.0f;
    }

    // Update the Date and mTime color
    public void updateDateAndTimeColourTo(int colour) {
        dateAndTimeColour = colour;
        mTextTimePaint.setColor(colour);
        mTextDatePaint.setColor(colour);
    }

    // Update the background color
    public void updateBackgroundColourTo(int colour) {
        backgroundColour = colour;
        backgroundPaint.setColor(colour);
    }

    // Update to default in Ambient mode
    public void updateBackgroundColourToDefault() {
        backgroundPaint.setColor(BACKGROUND_DEFAULT_COLOUR);
    }

    // Update to default in Ambient mode
    public void updateDateAndTimeColourToDefault() {
        mTextTimePaint.setColor(DATE_AND_TIME_DEFAULT_COLOUR);
        mTextDatePaint.setColor(DATE_AND_TIME_DEFAULT_COLOUR);
    }

    // Restore to selected color in non-Ambient mode
    public void restoreDateAndTimeColour() {
        mTextTimePaint.setColor(dateAndTimeColour);
        mTextDatePaint.setColor(dateAndTimeColour);
    }

    // Restore to selected color in non-Ambient mode
    public void restoreBackgroundColour() {
        backgroundPaint.setColor(backgroundColour);
    }


    public void setAntiAlias(boolean antiAlias) {
        mTextTimePaint.setAntiAlias(antiAlias);
        mTextDatePaint.setAntiAlias(antiAlias);
        mTextTempHighPaint.setAntiAlias(antiAlias);
        mTextTempLowPaint.setAntiAlias(antiAlias);
    }

    public void setColor(int color) {
        mTextTimePaint.setColor(color);
        mTextDatePaint.setColor(color);
    }

    public void setShowSeconds(boolean showSeconds) {
        shouldShowSeconds = showSeconds;
    }

    public void updateWeatherData(DataMap dataMap) {
        //  Use keys associated with every item in order to get hold of the sent values
        if (dataMap.containsKey(Constants.WEATHER_KEY_TEMP_MAX)) {
            mWeatherHigh = dataMap.getString(Constants.WEATHER_KEY_TEMP_MAX);
            Log.d(LOG_TAG, "High = " + mWeatherHigh);
        } else {
            Log.d(LOG_TAG, "What? No high?");
        }

        if (dataMap.containsKey(Constants.WEATHER_KEY_TEMP_MIN)) {
            mWeatherLow = dataMap.getString(Constants.WEATHER_KEY_TEMP_MIN);
            Log.d(LOG_TAG, "Low = " + mWeatherLow);
        } else {
            Log.d(LOG_TAG, "What? No low?");
        }

        if (dataMap.containsKey(Constants.WEATHER_KEY_ID)) {
            int weatherId = dataMap.getInt(Constants.WEATHER_KEY_ID);
            Drawable b = resources.getDrawable(Utility.getIconResourceForWeatherCondition(weatherId));
            Bitmap icon = ((BitmapDrawable) b).getBitmap();
            float scaledWidth = (mTextTempHighPaint.getTextSize() / icon.getHeight()) * icon.getWidth();
            mWeatherIcon = Bitmap.createScaledBitmap(icon, (int) scaledWidth, (int) mTextTempHighPaint.getTextSize(), true);

        } else {
            Log.d(LOG_TAG, "What? no weatherId?");
        }
    }

    public void updateConfigurationChanges(DataMap dataMap) {
        //  Use keys associated with every item in order to get hold of the sent values
        if (dataMap.containsKey("KEY_BACKGROUND_COLOUR")) {
            String backgroundColour = dataMap.getString("KEY_BACKGROUND_COLOUR");
            updateBackgroundColourTo(Color.parseColor(backgroundColour));
            Log.d(LOG_TAG, "processConfiguration" + backgroundColour);
        }

        if (dataMap.containsKey("KEY_DATE_TIME_COLOUR")) {
            String timeColour = dataMap.getString("KEY_DATE_TIME_COLOUR");
            updateDateAndTimeColourTo(Color.parseColor(timeColour));
            Log.d(LOG_TAG, "processConfiguration" + timeColour);
        }
    }
}
