package com.notification.timer;

import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

class HelpOverlay {

    private static final String TAG = "HelpOverlay";

    private final LinearLayout layout;
    private final ImageView imageViewForeground;
    private final ImageView imageViewBackground;

    private int currentImageClick;

    HelpOverlay(MainActivity mainActivity) {
        layout = mainActivity.findViewById(R.id.layoutHelp);
        imageViewBackground = mainActivity.findViewById(R.id.imageViewHelpBackground);
        imageViewForeground = mainActivity.findViewById(R.id.imageViewHelpForeground);
        TextView textViewAbove = mainActivity.findViewById(R.id.textViewHelpAbove);
        TextView textViewBelow = mainActivity.findViewById(R.id.textViewHelpBelow);
        imageViewForeground.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                next();
            }
        });
        textViewAbove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hide();
            }
        });
        textViewBelow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hide();
            }
        });
    }

    private void setVisible(boolean visible) {
        Log.d(TAG, "setVisible: visible=" + visible);
        int visibility = visible? View.VISIBLE : View.INVISIBLE;
        layout.setVisibility(visibility);
        imageViewBackground.setVisibility(visibility);
        imageViewForeground.setVisibility(visibility);
    }

    void show() {
        Log.d(TAG, "show");
        setVisible(true);
        currentImageClick = 0;
        imageViewForeground.setImageResource(R.drawable.help);
        imageViewBackground.setImageResource(R.drawable.help_background);
    }

    private void next() {
        ++currentImageClick;
        Log.d(TAG, "next: currentImageClick=" + currentImageClick);
        if (currentImageClick == 2){
            hide();
        }
    }

    private void hide() {
        Log.d(TAG, "hide");
        setVisible(false);
    }
}
