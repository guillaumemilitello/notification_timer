package com.codetroopers.betterpickers.hmspicker;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import com.codetroopers.betterpickers.widget.ZeroTopPaddingTextView;
import com.notification.timer.R;

public class HmsView extends LinearLayout {

    private ZeroTopPaddingTextView mHoursOnes, mHoursTens;
    private ZeroTopPaddingTextView mMinutesOnes, mMinutesTens;
    private ZeroTopPaddingTextView mSecondsOnes, mSecondsTens;
    private ZeroTopPaddingTextView mMinusLabel, mMinuteLabel, mHourLabel;
    private final Typeface mTypefaceLekton, mTypefaceLektonBold;

    private ColorStateList mTextColor;

    /**
     * Instantiate an HmsView
     *
     * @param context the Context in which to inflate the View
     */
    public HmsView(Context context) {
        this(context, null);
    }

    /**
     * Instantiate an HmsView
     *
     * @param context the Context in which to inflate the View
     * @param attrs attributes that define the title color
     */
    public HmsView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mTypefaceLekton = Typeface.createFromAsset(context.getAssets(), "fonts/Lekton-Regular.ttf");
        mTypefaceLektonBold = Typeface.createFromAsset(context.getAssets(), "fonts/Lekton-Bold.ttf");

        // Init defaults
        mTextColor = getResources().getColorStateList(R.color.dialog_text_color_holo);
    }

    /**
     * Set a theme and restyle the views. This View will change its text color.
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
        if (mHoursTens != null) {
            mHoursTens.setTextColor(mTextColor);
        }
        if (mHoursOnes != null) {
            mHoursOnes.setTextColor(mTextColor);
        }
        if (mMinutesOnes != null) {
            mMinutesOnes.setTextColor(mTextColor);
        }
        if (mMinutesTens != null) {
            mMinutesTens.setTextColor(mTextColor);
        }
        if (mSecondsOnes != null) {
            mSecondsOnes.setTextColor(mTextColor);
        }
        if (mSecondsTens != null) {
            mSecondsTens.setTextColor(mTextColor);
        }
        if (mMinusLabel != null) {
            mMinusLabel.setTextColor(mTextColor);
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mHoursTens = (ZeroTopPaddingTextView) findViewById(R.id.hours_tens);
        mHoursOnes = (ZeroTopPaddingTextView) findViewById(R.id.hours_ones);
        mMinutesTens = (ZeroTopPaddingTextView) findViewById(R.id.minutes_tens);
        mMinutesOnes = (ZeroTopPaddingTextView) findViewById(R.id.minutes_ones);
        mSecondsTens = (ZeroTopPaddingTextView) findViewById(R.id.seconds_tens);
        mSecondsOnes = (ZeroTopPaddingTextView) findViewById(R.id.seconds_ones);
        mMinusLabel = (ZeroTopPaddingTextView) findViewById(R.id.minus_label);
        mHourLabel = (ZeroTopPaddingTextView) findViewById(R.id.hours_label);
        mMinuteLabel = (ZeroTopPaddingTextView) findViewById(R.id.minutes_label);

        if (mHoursTens != null) {
            mHoursTens.setTypeface(mTypefaceLektonBold);
            mHoursTens.updatePaddingForBoldDate();
        }
        if (mHoursOnes != null) {
            mHoursOnes.setTypeface(mTypefaceLektonBold);
            mHoursOnes.updatePaddingForBoldDate();
        }
        if (mMinutesTens != null) {
            mMinutesTens.setTypeface(mTypefaceLektonBold);
            mMinutesTens.updatePaddingForBoldDate();
        }
        if (mMinutesOnes != null) {
            mMinutesOnes.setTypeface(mTypefaceLektonBold);
            mMinutesOnes.updatePaddingForBoldDate();
        }
        // Set the lowest time unit with thin font (excluding hundredths)
        if (mSecondsTens != null) {
            mSecondsTens.setTypeface(mTypefaceLekton);
            mSecondsTens.updatePaddingForBoldDate();
        }
        if (mSecondsOnes != null) {
            mSecondsOnes.setTypeface(mTypefaceLekton);
            mSecondsOnes.updatePaddingForBoldDate();
        }
        if (mHourLabel != null) {
            mHourLabel.setTypeface(mTypefaceLekton);
            mHourLabel.updatePaddingForBoldDate();
            mHourLabel.setPaddingLeft((int)getResources().getDimension(R.dimen.time_separator_padding));
            mHourLabel.setPaddingRight((int)getResources().getDimension(R.dimen.time_separator_padding));
        }
        if (mMinuteLabel != null) {
            mMinuteLabel.setTypeface(mTypefaceLekton);
            mMinuteLabel.updatePaddingForBoldDate();
            mMinuteLabel.setPaddingLeft((int)getResources().getDimension(R.dimen.time_separator_padding));
            mMinuteLabel.setPaddingRight((int)getResources().getDimension(R.dimen.time_separator_padding));
        }
    }

    /**
     * Set the time shown
     *
     * @param hoursTensDigit the tens digit of the hours TextView
     * @param hoursOnesDigit the ones digit of the hours TextView
     * @param minutesTensDigit the tens digit of the minutes TextView
     * @param minutesOnesDigit the ones digit of the minutes TextView
     * @param secondsTensDigit the tens digit of the seconds TextView
     * @param secondsOnesDigit the ones digit of the seconds TextView
     */
    public void setTime(int hoursTensDigit, int hoursOnesDigit, int minutesTensDigit, int minutesOnesDigit, int secondsTensDigit,
                        int secondsOnesDigit) {
        setTime(false, hoursTensDigit, hoursOnesDigit, minutesTensDigit, minutesOnesDigit, secondsTensDigit, secondsOnesDigit);
    }

    public void setTime(boolean isNegative, int hoursTensDigit, int hoursOnesDigit, int minutesTensDigit, int minutesOnesDigit, int secondsTensDigit,
            int secondsOnesDigit) {

        mMinusLabel.setVisibility(isNegative ? View.VISIBLE : View.GONE);

        if (mHoursTens != null) {
            mHoursTens.setText(String.format("%d", hoursTensDigit));
        }
        if (mHoursOnes != null) {
            mHoursOnes.setText(String.format("%d", hoursOnesDigit));
        }
        if (mMinutesTens != null) {
            mMinutesTens.setText(String.format("%d", minutesTensDigit));
        }
        if (mMinutesOnes != null) {
            mMinutesOnes.setText(String.format("%d", minutesOnesDigit));
        }
        if (mSecondsTens != null) {
            mSecondsTens.setText(String.format("%d", secondsTensDigit));
        }
        if (mSecondsOnes != null) {
            mSecondsOnes.setText(String.format("%d", secondsOnesDigit));
        }
    }
}