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

    private int currentImageId;

    HelpOverlay(MainActivity mainActivity) {
        layout = mainActivity.findViewById(R.id.layoutHelp);
        imageView = mainActivity.findViewById(R.id.imageViewHelp);
        TextView textViewInfo = mainActivity.findViewById(R.id.textViewHelpButtonInfo);
        TextView textViewDone = mainActivity.findViewById(R.id.textViewHelpButtonDone);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                next();
            }
        });
        textViewInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                next();
            }
        });
        textViewDone.setOnClickListener(new View.OnClickListener() {
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
        currentImageId = 0;
        showImage(currentImageId);
    }

    private void next() {
        Log.d(TAG, "next");
        showImage(++currentImageId);
    }

    private void hide() {
        Log.d(TAG, "hide");
        setVisible(false);
    }

    private void showImage(int imageId) {
        Log.d(TAG, "showImage: imageId=" + imageId);
        switch (imageId) {
            case 0: imageView.setImageResource(R.drawable.help_add); return;
            case 1: imageView.setImageResource(R.drawable.help_ready); return;
            case 2: imageView.setImageResource(R.drawable.help_running); return;
        }
        hide();
    }
}
