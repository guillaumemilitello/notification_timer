package com.notification.timer;

import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

class HelpOverlay {

    private static final String TAG = "HelpOverlay";

    private final LinearLayout layout;
    private final ImageView imageView;

    private int currentImageClick;

    HelpOverlay(MainActivity mainActivity) {
        layout = mainActivity.findViewById(R.id.layoutHelp);
        imageView = mainActivity.findViewById(R.id.imageViewHelp);
        TextView textViewAbove = mainActivity.findViewById(R.id.textViewHelpAbove);
        TextView textViewBelow = mainActivity.findViewById(R.id.textViewHelpBelow);
        imageView.setOnClickListener(new View.OnClickListener() {
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
        imageView.setVisibility(visibility);
    }

    void show() {
        Log.d(TAG, "show");
        setVisible(true);
        currentImageClick = 0;
        imageView.setImageResource(R.drawable.help);
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
