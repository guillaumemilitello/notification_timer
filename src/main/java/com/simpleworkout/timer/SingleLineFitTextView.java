package com.simpleworkout.timer;

import android.content.Context;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.widget.TextView;

public class SingleLineFitTextView extends TextView {

    private static final String TAG = "SingleLineFitTextView";

    private boolean textViewSizeInitialized = false;

    private RectF rectF, testSizeRectF;
    private TextPaint textPaint;

    private SparseIntArray cachedTextSizes;
    private int minTextSize = 8;
    private float defaultTextSize;

    private float fontWidthScale = 1;

    private void initialize() {
        rectF = new RectF();
        testSizeRectF = new RectF();
        textPaint = new TextPaint(getPaint());
        cachedTextSizes = new SparseIntArray();
        defaultTextSize = getTextSize();
    }

    public SingleLineFitTextView(Context context) {
        super(context);
        initialize();
    }

    public SingleLineFitTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public SingleLineFitTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    @Override
    public void setTextSize(float size) {
        defaultTextSize = size;
        cachedTextSizes.clear();
        adjustTextSize();
    }

    @Override
    public void setTextSize(int unit, float size) {
        defaultTextSize = TypedValue.applyDimension(unit, size, getContext().getResources().getDisplayMetrics());
        cachedTextSizes.clear();
        adjustTextSize();
    }

    public void setMinTextSize(int minTextSize) {
        this.minTextSize = minTextSize;
    }

    public void setFont(String path) {
        setFont(path, 1);
    }

    public void setFont(String path, float widthScale) {
        setTypeface(Typeface.createFromAsset(getContext().getAssets(), "fonts/" + path));
        fontWidthScale = widthScale;
    }

    private void adjustTextSize() {
        if (!textViewSizeInitialized) {
            return;
        }
        int startSize = minTextSize;
        rectF.right = getMeasuredWidth() - getCompoundPaddingLeft() - getCompoundPaddingRight();
        rectF.bottom = getMeasuredHeight() - getCompoundPaddingBottom() - getCompoundPaddingTop();
        super.setTextSize(TypedValue.COMPLEX_UNIT_PX, getBestTextSize(startSize, (int) defaultTextSize));
        Log.d(TAG, "adjsutTextSize: (" + getText() + ") rectF=" + rectF);
    }

    private boolean doesSizeFit(int size) {
        textPaint.setTextSize(size);
        testSizeRectF.bottom = textPaint.getFontSpacing();
        testSizeRectF.right = textPaint.measureText(getText().toString()) * fontWidthScale;
        testSizeRectF.offsetTo(0, 0);
        return rectF.contains(testSizeRectF);
    }

    private int getBestTextSize(int start, int end) {
        Log.d(TAG, "getBestTextSize: start=" + start + ", end=" + end);

        int textLength = getText().length();
        int size = cachedTextSizes.get(textLength);
        if (size != 0) {
            Log.d(TAG, "getBestTextSize: textLength=" + textLength);
            return size;
        }
        size = binaryBestTextSizeSearch(start, end);
        cachedTextSizes.put(textLength, size);
        return size;
    }

    private int binaryBestTextSizeSearch(int start, int end) {
        Log.d(TAG, "binarySearch: start=" + start + ", end=" + end + ", text=" + getText());
        int best = start;
        while(end >= start) {
            int middle = (start + end) / 2;
            if (doesSizeFit(middle)) {
                best = start;
                start = middle + 1;
            } else {
                end = middle - 1;
                best = end;
            }
        }
        return best;
    }

    @Override
    protected void onTextChanged(final CharSequence text, final int start,
                                 final int before, final int after) {
        super.onTextChanged(text, start, before, after);
        adjustTextSize();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        Log.d(TAG, "onSizeChanged: rectF=" + rectF + ", w=" + w + ", h=" + h);
        textViewSizeInitialized = true;
        cachedTextSizes.clear();
        adjustTextSize();
    }
}