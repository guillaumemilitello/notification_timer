package com.notification.timer;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.Locale;

class TimerTextViewParameters {

    private static final String TAG = "TimerTextViewParameters";

    private final SharedPreferences sharedPreferences;
    private final Context context;

    private final int targetWidth, targetHeight;
    private final float density;
    private final float[] size;
    private final int[] leftMargin, topMargin, rightMargin, bottomMargin;

    private final Paint paint;
    private final Rect rectBounds;

    private final float threshold = 2;

    private static final int SIZE_COUNT = 5;

    float getTextSize(int digits) {
        return size[digits - 2]; // size start from 2
    }

    int getLeftMargin(int digits) {
        return leftMargin[digits - 2];
    }

    int getTopMargin(int digits) {
        return topMargin[digits - 2];
    }

    int getRightMargin(int digits) {
        return rightMargin[digits - 2];
    }

    int getBottomMargin(int digits) {
        return bottomMargin[digits - 2];
    }

    TimerTextViewParameters(MainActivity.LayoutMode layoutMode, int targetWidth, int targetHeight, float density, Context context, SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
        this.context = context;

        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
        this.density = density;

        size = new float[SIZE_COUNT];
        leftMargin = new int[SIZE_COUNT];
        topMargin = new int[SIZE_COUNT];
        rightMargin = new int[SIZE_COUNT];
        bottomMargin = new int[SIZE_COUNT];

        paint = new Paint();
        paint.setTypeface(MainActivity.typefaceLektonBold);
        rectBounds = new Rect();

        float[] loadedSize = loadSizes(layoutMode);

        for (int i=0; i < SIZE_COUNT; ++i) {
            update(i + 2, loadedSize[i]);  // size start from 2
        }

        if (!Arrays.equals(loadedSize, size)) {
            saveSizes(layoutMode, size);
        }
    }

    private void update(int digits, float loadedSize) {
        int digitsIndex = digits - 2;
        // Check size just a threshold below and above
        if (!doesRectFit(digits, loadedSize - threshold) || doesRectFit(digits, loadedSize + threshold)) {
            this.size[digitsIndex] = findBestRectFontSize(digits);
        } else {
            this.size[digitsIndex] = loadedSize;
        }
        Log.d(TAG, "update: digits=" + digits + ", loadedSize=" + loadedSize + ", size=" + size[digitsIndex]);
        updateMargins(digitsIndex);
    }

    private void updateMargins(int digitsIndex) {
        // Ratios are fixed for the Typeface Lekton
        topMargin[digitsIndex] = (int)(size[digitsIndex] * density * -0.425);
        bottomMargin[digitsIndex] = (int)(topMargin[digitsIndex] * 1.125);
        leftMargin[digitsIndex] = (int)(size[digitsIndex] * density * -0.05);
        rightMargin[digitsIndex] = leftMargin[digitsIndex];
    }

    private float findBestRectFontSize(int digits) {
        float size = 300; // TODO: put medium text size
        float sizeHigh, sizeLow;
        if (doesRectFit(digits, size)) {
            sizeLow = size;
            size *= 2;
            while (doesRectFit(digits, size)) {
                sizeLow = size;
                size *= 2;
            }
            sizeHigh = size;
        } else {
            sizeHigh = size;
            size /= 2;
            while (!doesRectFit(digits, size) && size > 10) {
                sizeHigh = size;
                size /= 2;
            }
            sizeLow = size;
        }
        return findBestRectFontSize(digits, sizeHigh, sizeLow);
    }

    private float findBestRectFontSize(int digits, float sizeHigh, float sizeLow) {
        while (sizeHigh - sizeLow > threshold) {
            float size = (sizeHigh + sizeLow) / 2;
            if(doesRectFit(digits, size)) {
                sizeLow = size;
            } else {
                sizeHigh = size;
            }
        }
        Log.d(TAG, "findBestRectFontSize: size=" + sizeLow);
        return sizeLow;
    }

    private boolean doesRectFit(int digits, float rectTextSize) {
        paint.setTextSize(rectTextSize);
        paint.getTextBounds("00:00:00", 0, digits > 4 ? digits + 2 : digits > 2 ? digits + 1 : digits, rectBounds);
        // Threshold are fixed for the Typeface Lekton including font padding
        int width = (int)(rectBounds.width() * density * ((digits == 2)? 1.125 : 0.925));
        int height = (int)(rectBounds.height() * density * 1.5);
        return width < targetWidth && height < targetHeight;
    }

    private void saveSizes(MainActivity.LayoutMode layoutMode, float[] size) {
        if (sharedPreferences != null) {
            SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
            for (int i = 0; i < SIZE_COUNT; ++i) {
                sharedPreferencesEditor.putFloat(String.format(Locale.US, context.getString(R.string.pref_timer_text_size), layoutMode.getIndex(), i), size[i]);
                Log.d(TAG, "saveSizes: key=" + String.format(Locale.US, context.getString(R.string.pref_timer_text_size), layoutMode.getIndex(), i) + ", size=" + size[i]);
            }
            sharedPreferencesEditor.apply();
        }
    }

    private float[] loadSizes(MainActivity.LayoutMode layoutMode) {
        float[] size = new float[SIZE_COUNT];
        if (sharedPreferences != null) {
            for (int i = 0; i < SIZE_COUNT; ++i) {
                size[i] = sharedPreferences.getFloat(String.format(Locale.US, context.getString(R.string.pref_timer_text_size), layoutMode.getIndex(), i), -1);
                Log.d(TAG, "loadSizes: key=" + String.format(Locale.US, context.getString(R.string.pref_timer_text_size), layoutMode.getIndex(), i) + ", size=" + size[i]);
            }
        }
        return size;
    }

    @NonNull
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("targetWidth=").append(targetWidth).append(", targetHeight=").append(targetHeight).append(", density=").append(density).append("\n");
        for (int i=0 ; i < SIZE_COUNT; ++i) {
            s.append("digits=").append(i + 2).append(", textSize=").append(size[i]).append(", margins={").append(leftMargin[i]).append(",").append(topMargin[i]).append(",").append(rightMargin[i]).append(",").append(bottomMargin[i]).append("}").append("\n");
        }
        return s.toString();
    }
}
