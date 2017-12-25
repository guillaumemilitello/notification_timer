package com.simpleworkout.timer;

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
    private int color = Color.BLACK;
    private int visibility = View.GONE;

    TimerTextView(TextView textView) {
        this.textView = textView;
        this.layoutParams = (LinearLayout.LayoutParams) textView.getLayoutParams();
    }

    void setParameters(TimerTextViewParameters parameters) {
        Log.d(TAG, "TimerTextView: textView=" + textView + ", parameters=" + parameters);
        this.parameters = parameters;
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

    void setTypeface(Typeface typeface) {
        if (this.typeface != typeface) {
            this.typeface = typeface;
            textView.setTypeface(typeface);
        }
    }

    void setDigits(int digits, boolean sideMargins) {
        if (parameters != null && this.digits != digits) {
            this.digits = digits;
            textView.setTextSize(parameters.getTextSize(digits));
            if (sideMargins) {
                layoutParams.setMargins(parameters.getLeftMargin(digits), parameters.getTopMargin(digits), parameters.getRightMargin(digits), parameters.getBottomMargin(digits));
            } else {
                layoutParams.setMargins(0, parameters.getTopMargin(digits), 0, parameters.getBottomMargin(digits));
            }
            Log.d(TAG, "TimerTextView: digits=" + digits + ", sideMargins=" + sideMargins + " parameters=" + parameters);
        }
    }
}
