package com.codetroopers.betterpickers.mspicker;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.util.Log;

import java.util.Vector;

/**
 * User: derek Date: 5/2/13 Time: 7:55 PM
 */
public class MsPickerBuilder {

    private FragmentManager manager; // Required
    private Integer styleResId; // Required
    private Fragment targetFragment;
    private int mReference;
    private Vector<MsPickerDialogFragment.MsPickerDialogHandlerV2> mMsPickerDialogHandlerV2s = new Vector<MsPickerDialogFragment.MsPickerDialogHandlerV2>();
    private int mMinutes;
    private int mSeconds;
    private String titleText;
    private Integer plusMinusVisibility;

    /**
     * Set the visibility of the +/- button. This takes an int corresponding to Android's View.VISIBLE, View.INVISIBLE,
     * or View.GONE.  When using View.INVISIBLE, the +/- button will still be present in the layout but be
     * non-clickable. When set to View.GONE, the +/- button will disappear entirely, and the "0" button will occupy its
     * space.
     *
     * @param plusMinusVisibility an int corresponding to View.VISIBLE, View.INVISIBLE, or View.GONE
     * @return the current Builder object
     */
    public MsPickerBuilder setPlusMinusVisibility(int plusMinusVisibility) {
        this.plusMinusVisibility = plusMinusVisibility;
        return this;
    }


    /**
     * Attach a FragmentManager. This is required for creation of the Fragment.
     *
     * @param manager the FragmentManager that handles the transaction
     * @return the current Builder object
     */
    public MsPickerBuilder setFragmentManager(FragmentManager manager) {
        this.manager = manager;
        return this;
    }

    /**
     * Attach a style resource ID for theming. This is required for creation of the Fragment. Two stock styles are
     * provided using R.style.BetterPickersDialogFragment and R.style.BetterPickersDialogFragment.Light
     *
     * @param styleResId the style resource ID to use for theming
     * @return the current Builder object
     */
    public MsPickerBuilder setStyleResId(int styleResId) {
        this.styleResId = styleResId;
        return this;
    }

    /**
     * Attach a target Fragment. This is optional and useful if creating a Picker within a Fragment.
     *
     * @param targetFragment the Fragment to attach to
     * @return the current Builder object
     */
    public MsPickerBuilder setTargetFragment(Fragment targetFragment) {
        this.targetFragment = targetFragment;
        return this;
    }

    /**
     * Attach a reference to this Picker instance. This is used to track multiple pickers, if the user wishes.
     *
     * @param reference a user-defined int intended for Picker tracking
     * @return the current Builder object
     */
    public MsPickerBuilder setReference(int reference) {
        this.mReference = reference;
        return this;
    }

    /**
     * Attach universal objects as additional handlers for notification when the Picker is set. For most use cases, this
     * method is not necessary as attachment to an Activity or Fragment is done automatically.  If, however, you would
     * like additional objects to subscribe to this Picker being set, attach Handlers here.
     *
     * @param handler an Object implementing the appropriate Picker Handler
     * @return the current Builder object
     */
    public MsPickerBuilder addMsPickerDialogHandler(MsPickerDialogFragment.MsPickerDialogHandlerV2 handler) {
        this.mMsPickerDialogHandlerV2s.add(handler);
        return this;
    }

    /**
     * Remove objects previously added as handlers.
     *
     * @param handler the Object to remove
     * @return the current Builder object
     */
    public MsPickerBuilder removeMsPickerDialogHandler(MsPickerDialogFragment.MsPickerDialogHandlerV2 handler) {
        this.mMsPickerDialogHandlerV2s.remove(handler);
        return this;
    }

    /**
     * Set some initial values for the picker
     *
     * @param minutes the initial minutes value
     * @param seconds the initial seconds value
     * @return the current Builder object
     */
    public MsPickerBuilder setTime(int minutes, int seconds) {
        this.mMinutes = bounded(minutes, 0, 99);
        this.mSeconds = bounded(seconds, 0, 99);
        return this;
    }

    /**
     * Set some initial values for the picker
     *
     * @param timeInSeconds the time in seconds
     * @return the current Builder object
     */
    public MsPickerBuilder setTimeInSeconds(int timeInSeconds) {
        int remaining = timeInSeconds % 3600;
        int minutes = remaining / 60;
        int seconds = remaining % 60;

        return this.setTime(minutes, seconds);
    }

    /**
     * Set the picker title text
     *
     * @param titleText the String text
     * @return the current Builder object
     */
    public MsPickerBuilder setTitleText(String titleText) {
        this.titleText = titleText;
        return this;
    }

    /**
     * Set some initial values for the picker
     *
     * @param timeInMilliseconds the time in milliseconds
     * @return the current Builder object
     */
    public MsPickerBuilder setTimeInMilliseconds(long timeInMilliseconds) {
        return this.setTimeInSeconds((int) (timeInMilliseconds / 1000L));
    }

    /**
     * Instantiate and show the Picker
     */
    public void show() {
        if (manager == null || styleResId == null) {
            Log.e("MsPickerBuilder", "setFragmentManager() and setStyleResId() must be called.");
            return;
        }
        FragmentTransaction ft = manager.beginTransaction();
        final Fragment prev = manager.findFragmentByTag("ms_dialog");
        if (prev != null) {
            ft.remove(prev).commit();
            ft = manager.beginTransaction();
        }
        ft.addToBackStack(null);

        final MsPickerDialogFragment fragment = MsPickerDialogFragment.newInstance(mReference, styleResId, plusMinusVisibility, titleText);
        if (targetFragment != null) {
            fragment.setTargetFragment(targetFragment, 0);
        }
        fragment.setMsPickerDialogHandlersV2(mMsPickerDialogHandlerV2s);

        if ((mMinutes | mSeconds) != 0) {
            fragment.setTime(mMinutes, mSeconds);
        }

        fragment.show(ft, "ms_dialog");
    }

    private static int bounded(int i, int min, int max) {
        return Math.min(Math.max(i, min), max);
    }
}
