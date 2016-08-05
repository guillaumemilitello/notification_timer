package com.codetroopers.betterpickers.mspicker;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.codetroopers.betterpickers.R;

import java.util.Vector;

/**
 * Dialog to set alarm time.
 */
    public class MsPickerDialogFragment extends DialogFragment {

    private static final String REFERENCE_KEY = "MsPickerDialogFragment_ReferenceKey";
    private static final String THEME_RES_ID_KEY = "MsPickerDialogFragment_ThemeResIdKey";
    private static final String PLUS_MINUS_VISIBILITY_KEY = "MsPickerDialogFragment_PlusMinusVisibilityKey";

    private MsPicker mPicker;

    private int mReference = -1;
    private int mTheme = -1;
    private ColorStateList mTextColor;
    private int mDialogBackgroundResId;
    private Vector<MsPickerDialogHandlerV2> mMsPickerDialogHandlerV2s = new Vector<MsPickerDialogHandlerV2>();
    private int mMinutes;
    private int mSeconds;
    private int mPlusMinusVisibility = View.INVISIBLE;

    /**
     * Create an instance of the Picker (used internally)
     *
     * @param reference  an (optional) user-defined reference, helpful when tracking multiple Pickers
     * @param themeResId the style resource ID for theming
     * @return a Picker!
     */
    public static MsPickerDialogFragment newInstance(int reference, int themeResId, Integer plusMinusVisibility) {
        final MsPickerDialogFragment frag = new MsPickerDialogFragment();
        Bundle args = new Bundle();
        args.putInt(REFERENCE_KEY, reference);
        args.putInt(THEME_RES_ID_KEY, themeResId);
        if (plusMinusVisibility != null) {
            args.putInt(PLUS_MINUS_VISIBILITY_KEY, plusMinusVisibility);
        }
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null && args.containsKey(REFERENCE_KEY)) {
            mReference = args.getInt(REFERENCE_KEY);
        }
        if (args != null && args.containsKey(THEME_RES_ID_KEY)) {
            mTheme = args.getInt(THEME_RES_ID_KEY);
        }
        if (args != null && args.containsKey(PLUS_MINUS_VISIBILITY_KEY)) {
            mPlusMinusVisibility = args.getInt(PLUS_MINUS_VISIBILITY_KEY);
        }

        setStyle(DialogFragment.STYLE_NO_TITLE, 0);

        // Init defaults
        mTextColor = getResources().getColorStateList(R.color.dialog_text_color_holo_dark);
        mDialogBackgroundResId = R.drawable.dialog_full_holo_dark;

        if (mTheme != -1) {
            TypedArray a = getActivity().getApplicationContext().obtainStyledAttributes(mTheme, R.styleable.BetterPickersDialogFragment);

            mTextColor = a.getColorStateList(R.styleable.BetterPickersDialogFragment_bpTextColor);
            mDialogBackgroundResId = a.getResourceId(R.styleable.BetterPickersDialogFragment_bpDialogBackground, mDialogBackgroundResId);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(com.simpleworkout.timer.R.layout.ms_picker_dialog, null);

        Button doneButton = (Button) view.findViewById(R.id.done_button);
        Button cancelButton = (Button) view.findViewById(R.id.cancel_button);

        cancelButton.setTextColor(mTextColor);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
        doneButton.setTextColor(mTextColor);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
                for (MsPickerDialogHandlerV2 handler : mMsPickerDialogHandlerV2s) {
                    handler.onDialogMsSet(mReference, mPicker.isNegative(), mPicker.getMinutes(), mPicker.getSeconds());
                }

                final Activity activity = getActivity();
                final Fragment fragment = getTargetFragment();

                if (activity instanceof MsPickerDialogHandlerV2) {
                    final MsPickerDialogHandlerV2 act =
                            (MsPickerDialogHandlerV2) activity;
                    act.onDialogMsSet(mReference, mPicker.isNegative(), mPicker.getMinutes(), mPicker.getSeconds());
                } else if (fragment instanceof MsPickerDialogHandlerV2) {
                    final MsPickerDialogHandlerV2 frag =
                            (MsPickerDialogHandlerV2) fragment;
                    frag.onDialogMsSet(mReference, mPicker.isNegative(), mPicker.getMinutes(), mPicker.getSeconds());
                }
            }
        });

        mPicker = (MsPicker) view.findViewById(com.simpleworkout.timer.R.id.ms_picker);
        mPicker.setSetButton(doneButton);
        mPicker.setTime(mMinutes, mSeconds);
        mPicker.setTheme(mTheme);
        mPicker.setPlusMinusVisibility(mPlusMinusVisibility);

        getDialog().getWindow().setBackgroundDrawableResource(mDialogBackgroundResId);

        return view;
    }

    public interface MsPickerDialogHandlerV2 {

        void onDialogMsSet(int reference, boolean isNegative, int minutes, int seconds);
    }

    /**
     * @param handlers a Vector of handlers
     * Attach a Vector of handlers to be notified in addition to the Fragment's Activity and target Fragment.
     */
    public void setMsPickerDialogHandlersV2(Vector<MsPickerDialogHandlerV2> handlers) {
        mMsPickerDialogHandlerV2s = handlers;
    }

    public void setTime(int minutes, int seconds) {
        this.mMinutes = minutes;
        this.mSeconds = seconds;
        if (mPicker != null) {
            mPicker.setTime(minutes, seconds);
        }
    }
}
