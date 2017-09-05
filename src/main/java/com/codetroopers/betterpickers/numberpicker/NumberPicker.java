package com.codetroopers.betterpickers.numberpicker;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.simpleworkout.timer.MainActivity;
import com.simpleworkout.timer.R;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;

public class NumberPicker extends LinearLayout implements Button.OnClickListener,
        Button.OnLongClickListener, CheckBox.OnCheckedChangeListener {

    protected int mInputSize = 20;
    protected final Button mNumbers[] = new Button[10];
    protected int mInput[] = new int[mInputSize];
    protected int mInputPointer = -1;
    protected Button mLeft, mRight;
    protected ImageButton mDelete;
    protected NumberView mEnteredNumber;
    protected final Context mContext;

    private TextView mLabel;
    private CheckBox mCheckbox;
    private boolean mChecked;
    private boolean mInfinity;
    private int mSign;
    private String mLabelText = "";
    private String mCheckboxLabelText = "";
    private Button mSetButton;
    private static final int CLICKED_DECIMAL = 10;

    public static final int SIGN_POSITIVE = 0;
    public static final int SIGN_NEGATIVE = 1;

    protected View mDivider;
    private ColorStateList mTextColor;
    private int mKeyBackgroundResId;
    private int mButtonBackgroundResId;
    private int mDividerColor;
    private int mDeleteDrawableSrcResId;
    private int mTheme = -1;

    private BigDecimal mMinNumber = null;
    private BigDecimal mMaxNumber = null;

    /**
     * Instantiates a NumberPicker object
     *
     * @param context the Context required for creation
     */
    public NumberPicker(Context context) {
        this(context, null);
    }

    /**
     * Instantiates a NumberPicker object
     *
     * @param context the Context required for creation
     * @param attrs   additional attributes that define custom colors, selectors, and backgrounds.
     */
    public NumberPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        LayoutInflater layoutInflater =
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(getLayoutId(), this);

        // Init defaults
        mTextColor = getResources().getColorStateList(R.color.dialog_text_color_holo_dark);
        mKeyBackgroundResId = R.drawable.key_background_dark;
        mButtonBackgroundResId = R.drawable.button_background_dark;
        mDeleteDrawableSrcResId = R.drawable.ic_backspace_dark;
        mDividerColor = getResources().getColor(R.color.default_divider_color_dark);
    }

    protected int getLayoutId() {
        return R.layout.number_picker_view;
    }

    /**
     * Change the theme of the Picker
     *
     * @param themeResId the resource ID of the new style
     */
    public void setTheme(int themeResId) {
        mTheme = themeResId;
        if (mTheme != -1) {
            TypedArray a = getContext().obtainStyledAttributes(themeResId, R.styleable.BetterPickersDialogFragment);

            mTextColor = a.getColorStateList(R.styleable.BetterPickersDialogFragment_bpTextColor);
            mKeyBackgroundResId = a.getResourceId(R.styleable.BetterPickersDialogFragment_bpKeyBackground, mKeyBackgroundResId);
            mButtonBackgroundResId = a.getResourceId(R.styleable.BetterPickersDialogFragment_bpButtonBackground, mButtonBackgroundResId);
            mDividerColor = a.getColor(R.styleable.BetterPickersDialogFragment_bpDividerColor, mDividerColor);
            mDeleteDrawableSrcResId = a.getResourceId(R.styleable.BetterPickersDialogFragment_bpDeleteIcon, mDeleteDrawableSrcResId);
        }

        restyleViews();
    }

    private void restyleViews() {
        for (Button number : mNumbers) {
            if (number != null) {
                number.setTextColor(mTextColor);
                number.setBackgroundResource(mKeyBackgroundResId);
            }
        }
        if (mDivider != null) {
            mDivider.setBackgroundColor(mDividerColor);
        }
        if (mLeft != null) {
            mLeft.setTextColor(mTextColor);
            mLeft.setBackgroundResource(mKeyBackgroundResId);
        }
        if (mRight != null) {
            mRight.setTextColor(mTextColor);
            mRight.setBackgroundResource(mKeyBackgroundResId);
        }
        if (mDelete != null) {
            mDelete.setBackgroundResource(mButtonBackgroundResId);
            mDelete.setImageDrawable(getResources().getDrawable(mDeleteDrawableSrcResId));
        }
        if (mEnteredNumber != null) {
            mEnteredNumber.setTheme(mTheme);
        }
        if (mLabel != null) {
            mLabel.setTextColor(mTextColor);
        }
        if (mCheckbox != null) {
            mCheckbox.setTextColor(mTextColor);
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mDivider = findViewById(R.id.divider);

        for (int i = 0; i < mInput.length; i++) {
            mInput[i] = -1;
        }
        mChecked = false;
        mInfinity = false;

        View v1 = findViewById(R.id.first);
        View v2 = findViewById(R.id.second);
        View v3 = findViewById(R.id.third);
        View v4 = findViewById(R.id.fourth);
        View v5 = findViewById(R.id.fifth);
        mEnteredNumber = (NumberView) findViewById(R.id.number_text);
        mDelete = (ImageButton) findViewById(R.id.delete);
        mDelete.setOnClickListener(this);
        mDelete.setOnLongClickListener(this);

        mNumbers[1] = (Button) v1.findViewById(R.id.key_left);
        mNumbers[2] = (Button) v1.findViewById(R.id.key_middle);
        mNumbers[3] = (Button) v1.findViewById(R.id.key_right);

        mNumbers[4] = (Button) v2.findViewById(R.id.key_left);
        mNumbers[5] = (Button) v2.findViewById(R.id.key_middle);
        mNumbers[6] = (Button) v2.findViewById(R.id.key_right);

        mNumbers[7] = (Button) v3.findViewById(R.id.key_left);
        mNumbers[8] = (Button) v3.findViewById(R.id.key_middle);
        mNumbers[9] = (Button) v3.findViewById(R.id.key_right);

        mLeft = (Button) v4.findViewById(R.id.key_left);
        mNumbers[0] = (Button) v4.findViewById(R.id.key_middle);
        mRight = (Button) v4.findViewById(R.id.key_right);
        setLeftRightEnabled();

        for (int i = 0; i < 10; i++) {
            mNumbers[i].setOnClickListener(this);
            mNumbers[i].setText(String.format("%d", i));
            mNumbers[i].setTag(R.id.numbers_key, new Integer(i));
        }
        updateNumber();

        Resources res = mContext.getResources();
        mLeft.setText(res.getString(R.string.infinity));
        mRight.setText(res.getString(R.string.number_picker_seperator));
        mLeft.setOnClickListener(this);
        mRight.setOnClickListener(this);
        mLabel = (TextView) findViewById(R.id.label);
        mCheckbox = (CheckBox) v5.findViewById(R.id.key_checkbox);
        mCheckbox.setOnCheckedChangeListener(this);
        mSign = SIGN_POSITIVE;

        // Set the correct label state
        showLabel();
        showLabelCheckbox();
        updateCheckbox(MainActivity.getInitPickerZero());

        restyleViews();
        updateKeypad();
    }

    /**
     * Using View.GONE, View.VISIBILE, or View.INVISIBLE, set the visibility of the plus/minus indicator
     *
     * @param visiblity an int using Android's View.* convention
     */
    public void setPlusMinusVisibility(int visiblity) {
        if (mLeft != null) {
            mLeft.setVisibility(visiblity);
        }
    }

    /**
     * Using View.GONE, View.VISIBILE, or View.INVISIBLE, set the visibility of the decimal indicator
     *
     * @param visiblity an int using Android's View.* convention
     */
    public void setDecimalVisibility(int visiblity) {
        if (mRight != null) {
            mRight.setVisibility(visiblity);
        }
    }

    /**
     * Set a minimum required number
     *
     * @param min the minimum required number
     */
    public void setMin(BigDecimal min) {
        mMinNumber = min;
    }

    /**
     * Set a maximum required number
     *
     * @param max the maximum required number
     */
    public void setMax(BigDecimal max) {
        mMaxNumber = max;
    }

    /**
     * Update the 0 button to determine whether it is able to be clicked.
     */
    public void updateZeroButton() {
        boolean enabled = mInputPointer >= 0 && !mInfinity;
        if (mNumbers[0] != null) {
            mNumbers[0].setEnabled(enabled);
            mNumbers[0].setAlpha(enabled? (float) 1: (float) 0.3);
        }
    }

    /**
     * Update the delete button to determine whether it is able to be clicked.
     */
    public void updateDeleteButton() {
        boolean enabled = mInputPointer != -1;
        if (mDelete != null) {
            mDelete.setEnabled(enabled);
            mDelete.setAlpha(enabled? (float) 1: (float) 0.3);
        }
    }

    @Override
    public void onClick(View v) {
        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        doOnClick(v);
        updateDeleteButton();
    }

    protected void doOnClick(View v) {
        Integer val = (Integer) v.getTag(R.id.numbers_key);
        if (val != null) {
            // A number was pressed
            addClickedNumber(val);
        } else if (v == mDelete) {
            if (mInfinity) {
                reset();
                enableKeypad(true);
                mInfinity = false;
            }
            else if (mInputPointer >= 0) {
                for (int i = 0; i < mInputPointer; i++) {
                    mInput[i] = mInput[i + 1];
                }
                mInput[mInputPointer] = 0;
                mInputPointer--;
            }
        } else if (v == mLeft) {
            onLeftClicked();
        } else if (v == mRight) {
            onRightClicked();
        }
        updateKeypad();
    }

    @Override
    public boolean onLongClick(View v) {
        v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        if (v == mDelete) {
            mDelete.setPressed(false);
            reset();
            updateKeypad();
            return true;
        }
        return false;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        mChecked = isChecked;
    }

    private void updateKeypad() {
        // Update state of keypad
        updateLeftRightButtons();
        // Update the number
        updateNumber();
        // enable/disable the "set" key
        enableSetButton();
        // Update the backspace button
        updateDeleteButton();
        // Update the zero button
        updateZeroButton();
    }

    /**
     * Set the text displayed in the small label
     *
     * @param labelText the String to set as the label
     */
    public void setLabelText(String labelText) {
        mLabelText = labelText;
        showLabel();
    }

    public void setCheckBoxLabelText(String labelText) {
        mCheckboxLabelText = labelText;
        showLabelCheckbox();
    }

    private void showLabel() {
        if (mLabel != null) {
            mLabel.setText(mLabelText);
        }
    }

    private void showLabelCheckbox() {
        if (mCheckbox != null) {
            mCheckbox.setText(mCheckboxLabelText);
        }
    }

    private void updateCheckbox(boolean checked) {
        if (mCheckbox != null) {
            mCheckbox.setChecked(checked);
        }
    }

    /**
     * Reset all inputs.
     */
    public void reset() {
        for (int i = 0; i < mInputSize; i++) {
            mInput[i] = -1;
        }
        mInputPointer = -1;
        updateNumber();
    }

    public void enableKeypad(boolean enable) {
        for (int i = 0; i < 10; i++) {
            mNumbers[i].setEnabled(enable);
        }
        mLeft.setEnabled(enable);
        mCheckbox.setEnabled(enable);
    }

    // Update the number displayed in the picker:
    protected void updateNumber() {
        String numberString = getEnteredNumberString();
        numberString = numberString.replaceAll("\\-", "");
        String[] split = numberString.split("\\.");
        if (split.length >= 2) {
            if (split[0].equals("")) {
                mEnteredNumber.setNumber("0", split[1], containsDecimal(),
                        mSign == SIGN_NEGATIVE);
            } else {
                mEnteredNumber.setNumber(split[0], split[1], containsDecimal(),
                        mSign == SIGN_NEGATIVE);
            }
        } else if (split.length == 1) {
            if (split[0].equals(String.valueOf(Integer.MAX_VALUE))) {
                split[0] = "∞";
            }
            mEnteredNumber.setNumber(split[0], "", containsDecimal(),
                    mSign == SIGN_NEGATIVE);
        } else if (numberString.equals(".")) {
            mEnteredNumber.setNumber("0", "", true, mSign == SIGN_NEGATIVE);
        }
    }

    protected void setLeftRightEnabled() {
        mLeft.setEnabled(true);
        mRight.setEnabled(canAddDecimal());
        if (!canAddDecimal()) {
            mRight.setContentDescription(null);
        }
    }

    private void addClickedNumber(int val) {
        if (mInputPointer < mInputSize - 1) {
            // For 0 we need to check if we have a value of zero or not
            for (int i = mInputPointer; i >= 0; i--) {
                mInput[i + 1] = mInput[i];
            }
            mInputPointer++;
            mInput[0] = val;
        }
    }

    /**
     * Clicking on the bottom left button will toggle the sign.
     */
    private void onLeftClicked() {
        mInfinity = true;
        reset();
        int[] intMaxValueArray = new int[] {7,4,6,3,8,4,7,4,1,2};
        for (int i = 0; i < intMaxValueArray.length; ++i) {
            mInput[i] = intMaxValueArray[i];
            mInputPointer++;
        }
        updateNumber();
        enableKeypad(false);
    }

    /**
     * Clicking on the bottom right button will add a decimal point.
     */
    private void onRightClicked() {
        if (canAddDecimal()) {
            addClickedNumber(CLICKED_DECIMAL);
        }
    }

    private boolean containsDecimal() {
        boolean containsDecimal = false;
        for (int i : mInput) {
            if (i == 10) {
                containsDecimal = true;
            }
        }
        return containsDecimal;
    }

    /**
     * Checks if the user allowed to click on the right button.
     *
     * @return true or false if the user is able to add a decimal or not
     */
    private boolean canAddDecimal() {
        return !containsDecimal();
    }

    private String getEnteredNumberString() {
        String value = "";
        for (int i = mInputPointer; i >= 0; i--) {
            if (mInput[i] == -1) {
                // Don't add
            } else if (mInput[i] == CLICKED_DECIMAL) {
                value += ".";
            } else {
                value += mInput[i];
            }
        }
        return value;
    }

    /**
     * Returns the number inputted by the user
     *
     * @return a double representing the entered number
     */
    public BigDecimal getEnteredNumber() {
        String value = "0";
        for (int i = mInputPointer; i >= 0; i--) {
            if (mInput[i] == -1) {
                break;
            } else if (mInput[i] == CLICKED_DECIMAL) {
                value += ".";
            } else {
                value += mInput[i];
            }
        }
        if (mSign == SIGN_NEGATIVE) {
            value = "-" + value;
        }

        return new BigDecimal(value);
    }

    public boolean getChecked() {
        return mChecked;
    }

    private void updateLeftRightButtons() {
        mRight.setEnabled(canAddDecimal());
    }

    /**
     * Enable/disable the "Set" button
     */
    private void enableSetButton() {
        if (mSetButton == null) {
            return;
        }

        // Nothing entered - disable
        if (mInputPointer == -1) {
            mSetButton.setEnabled(false);
            return;
        }

        // If the user entered 1 digits or more
        mSetButton.setEnabled(mInputPointer >= 0);
    }

    /**
     * Expose the set button to allow communication with the parent Fragment.
     *
     * @param b the parent Fragment's "Set" button
     */
    public void setSetButton(Button b) {
        mSetButton = b;
        enableSetButton();
    }

    /**
     * Returns the number as currently inputted by the user
     *
     * @return an String representation of the number with no decimal
     */
    public BigInteger getNumber() {
        BigDecimal bigDecimal = getEnteredNumber().setScale(0, BigDecimal.ROUND_FLOOR);
        return bigDecimal.toBigIntegerExact();
    }

    /**
     * Returns the decimal following the number
     *
     * @return a double representation of the decimal value
     */
    public double getDecimal() {
        return getEnteredNumber().remainder(BigDecimal.ONE).doubleValue();
    }

    /**
     * Returns whether the number is positive or negative
     *
     * @return true or false whether the number is positive or negative
     */
    public boolean getIsNegative() {
        return mSign == SIGN_NEGATIVE;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        final Parcelable parcel = super.onSaveInstanceState();
        final SavedState state = new SavedState(parcel);
        state.mInput = mInput;
        state.mSign = mSign;
        state.mInputPointer = mInputPointer;
        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        final SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());

        mInputPointer = savedState.mInputPointer;
        mInput = savedState.mInput;
        if (mInput == null) {
            mInput = new int[mInputSize];
            mInputPointer = -1;
        }
        mSign = savedState.mSign;
        updateKeypad();
    }

    public void setNumber(Integer integerPart, Double decimalPart, Integer mCurrentSign) {
        if (mCurrentSign != null) {
            mSign = mCurrentSign;
        } else {
            mSign = SIGN_POSITIVE;
        }

        if (decimalPart != null) {
            String decimalString = doubleToString(decimalPart);
            // remove "0." from the string
            readAndRightDigits(TextUtils.substring(decimalString, 2, decimalString.length()));
            mInputPointer++;
            mInput[mInputPointer] = CLICKED_DECIMAL;
        }

        if (integerPart != null) {
            readAndRightDigits(String.valueOf(integerPart));
        }
        updateKeypad();
    }

    private void readAndRightDigits(String digitsToRead) {
        for (int i = digitsToRead.length() - 1; i >= 0; i--) {
            mInputPointer++;
            mInput[mInputPointer] = digitsToRead.charAt(i) - '0';
        }
    }

    /**
     * Method used to format double and avoid scientific notation x.xE-x (ex: 4.0E-4)
     *
     * @param value double value to format
     * @return string representation of double value
     */
    private String doubleToString(double value) {
        // Use decimal format to avoid
        DecimalFormat format = new DecimalFormat("0.0");
        format.setMaximumFractionDigits(Integer.MAX_VALUE);
        return format.format(value);
    }


    private static class SavedState extends BaseSavedState {

        int mInputPointer;
        int[] mInput;
        int mSign;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            mInputPointer = in.readInt();
            int size = in.readInt();
            if (size > 0) {
                mInput = new int[size];
                in.readIntArray(mInput);
            }
            mSign = in.readInt();
        }


        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(mInputPointer);
            if (mInput != null) {
                dest.writeInt(mInput.length);
                dest.writeIntArray(mInput);
            } else {
                dest.writeInt(0);
            }
            dest.writeInt(mSign);
        }

        public static final Creator<SavedState> CREATOR
                = new Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
