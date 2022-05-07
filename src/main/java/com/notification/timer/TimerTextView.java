package com.notification.timer;

import android.graphics.Color;
import android.graphics.Typeface;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

class TimerTextView {

    private static final String TAG = "TimerTextView";

    private final TextView textView;
    private final LinearLayout.LayoutParams layoutParams;
    private TimerTextViewParameters parameters;
    private Typeface typeface;

    private int digits = 0;
    private String text = "";
    private boolean bold = false;
    private int color = Color.BLACK;
    private int visibility = View.GONE;
    private boolean sideMargins = false;

    TimerTextView(TextView textView) {
        this.textView = textView;
        this.layoutParams = (LinearLayout.LayoutParams) textView.getLayoutParams();
    }

    void setParameters(TimerTextViewParameters parameters, boolean sideMargins) {
        this.parameters = parameters;
        this.sideMargins = sideMargins;
        // force an update of the text size and the margins at the next setDigits()
        digits = 0;
    }

    public void setText(String text) {
        if (!this.text.equals(text)) {
            this.text = text;
            textView.setText(text);
        }
    }

    public void setTextColor(int color) {
        if (this.color != color) {
            this.color = color;
            textView.setTextColor(color);
        }
    }

    void setVisibility(int visibility) {
        if (this.visibility != visibility) {
            this.visibility = visibility;
            textView.setVisibility(visibility);
        }
    }

    void setTypeface(Typeface typeface, boolean bold) {
        if (this.typeface != typeface) {
            this.typeface = typeface;
            textView.setTypeface(typeface);
        }
        this.bold = bold;
    }

    void setDigits(int digits) {
        if (parameters != null && this.digits != digits) {
            this.digits = digits;
            final float textSize = parameters.getTextSize(digits);
            textView.setTextSize(textSize);
            if (sideMargins) { // separators
                layoutParams.setMargins(parameters.getLeftMargin(digits), parameters.getTopMargin(digits), parameters.getRightMargin(digits), parameters.getBottomMargin(digits));
                LinearLayout.LayoutParams textViewLayoutParams = (LinearLayout.LayoutParams) textView.getLayoutParams();
                final int sideMargin = (int) (textSize / (2.5 * parameters.getDensity() - 12));
                textViewLayoutParams.leftMargin = sideMargin;
                textViewLayoutParams.rightMargin = sideMargin;
                textViewLayoutParams.bottomMargin = (int)(textViewLayoutParams.topMargin * 0.725);
                textView.setLayoutParams(textViewLayoutParams);
            } else {
                layoutParams.setMargins(0, parameters.getTopMargin(digits), 0, parameters.getBottomMargin(digits));
                if (!bold) {
                    LinearLayout.LayoutParams textViewLayoutParams = (LinearLayout.LayoutParams) textView.getLayoutParams();
                    textViewLayoutParams.bottomMargin = (int)(textViewLayoutParams.topMargin * 0.88125);
                    textView.setLayoutParams(textViewLayoutParams);
                }
            }
            Log.d(TAG, "setDigits: digits=" + digits + ", textSize=" + textSize + ",sideMargins=" + sideMargins + " parameters=" + parameters);
        }
    }
}
