package com.simpleworkout.timer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

public class NoPaddingTextView extends TextView {

    private final static String TAG = "NoPaddingTextView";

    private Rect rect;
    private int blankSpace;

    public NoPaddingTextView(Context context) {
        super(context);
        init();
    }

    public NoPaddingTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setIncludeFontPadding(false);
        rect = new Rect();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        TextPaint textPaint = getPaint();
        textPaint.setTextSize(getTextSize());
        textPaint.setTypeface(getTypeface());
        textPaint.getTextBounds(getText().toString(), 0, getText().length(), rect);
        blankSpace = getMeasuredHeight() - rect.height();
        Log.d(TAG, "onMeasure: text=" + getText() + ", blankSpace=" + blankSpace + ", height=" + getMeasuredHeight() + ", rect=" + rect.height());
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public int getBlankSpace() {
        return blankSpace;
    }
}
