package com.codetroopers.betterpickers.numberpicker;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import com.codetroopers.betterpickers.widget.ZeroTopPaddingTextView;
import com.notification.timer.R;

public class NumberView extends LinearLayout {

    private ZeroTopPaddingTextView mNumber;
    private final Typeface mTypefaceLekton;

    private ColorStateList mTextColor;

    /**
     * Instantiate a NumberView
     *
     * @param context the Context in which to inflate the View
     */
    public NumberView(Context context) {
        this(context, null);
    }

    /**
     * Instantiate a NumberView
     *
     * @param context the Context in which to inflate the View
     * @param attrs attributes that define the title color
     */
    public NumberView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mTypefaceLekton = Typeface.createFromAsset(context.getAssets(), "fonts/Lekton-Regular.ttf");

        // Init defaults
        mTextColor = getResources().getColorStateList(R.color.dialog_text_color_holo);
    }

    /**
     * Set a theme and restyle the views. This View will change its title color.
     *
     * @param themeResId the resource ID for theming
     */
    public void setTheme(int themeResId) {
        if (themeResId != -1) {
            TypedArray a = getContext().obtainStyledAttributes(themeResId, R.styleable.BetterPickersDialogFragment);

            mTextColor = a.getColorStateList(R.styleable.BetterPickersDialogFragment_bpTextColor);
        }

        restyleViews();
    }

    private void restyleViews() {
        if (mNumber != null) {
            mNumber.setTextColor(mTextColor);
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mNumber = (ZeroTopPaddingTextView) findViewById(R.id.number);
        // Set the lowest time unit with thin font
        if (mNumber != null) {
            mNumber.setTypeface(mTypefaceLekton);
            mNumber.updatePaddingForBoldDate();
        }

        restyleViews();
    }

    /**
     * Set the number shown
     *
     * @param numbersDigit the non-decimal digits
     * @param decimalDigit the decimal digits
     * @param showDecimal whether it's a decimal or not
     * @param isNegative whether it's positive or negative
     */
    public void setNumber(String numbersDigit, String decimalDigit, boolean showDecimal,
            boolean isNegative) {
        if (mNumber != null) {
            if (numbersDigit.equals("")) {
                // Set to -
                mNumber.setText("-");
                mNumber.setEnabled(false);
                mNumber.updatePaddingForBoldDate();
                mNumber.setVisibility(View.VISIBLE);
            } else if (showDecimal) {
                // Set to bold
                mNumber.setText(numbersDigit);
                mNumber.setEnabled(true);
                mNumber.updatePaddingForBoldDate();
                mNumber.setVisibility(View.VISIBLE);
            } else {
                // Set to thin
                mNumber.setText(numbersDigit);
                mNumber.setEnabled(true);
                mNumber.updatePaddingForBoldDate();
                mNumber.setVisibility(View.VISIBLE);
            }
        }
    }
}