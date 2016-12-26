package com.simpleworkout.timer;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Spinner;

/**
 * Created by guillaume on 26/12/2016.
 */

public class PresetSpinner extends Spinner {
    OnItemSelectedListener listener;

    public PresetSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setSelection(int position) {
        super.setSelection(position);
        if (listener != null)
            listener.onItemSelected(null, null, position, 0);
    }

    public void setOnItemSelectedEvenIfUnchangedListener(
            OnItemSelectedListener listener) {
        this.listener = listener;
    }
}
