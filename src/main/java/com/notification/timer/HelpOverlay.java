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

    private int currentImageId;

    HelpOverlay(MainActivity mainActivity) {
        layout = mainActivity.findViewById(R.id.layoutHelp);
        imageViewBackground = mainActivity.findViewById(R.id.imageViewHelpBackground);
        imageViewForeground = mainActivity.findViewById(R.id.imageViewHelpForeground);
        TextView textViewInfo = mainActivity.findViewById(R.id.textViewHelpButtonInfo);
        TextView textViewDone = mainActivity.findViewById(R.id.textViewHelpButtonDone);
        imageViewForeground.setOnClickListener(new View.OnClickListener() {
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
        imageViewBackground.setVisibility(visibility);
        imageViewForeground.setVisibility(visibility);
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
            case 0:
                imageViewForeground.setImageResource(R.drawable.help_0_add_text);
                imageViewBackground.setImageResource(R.drawable.help_0_add_background);
                return;
            case 1:
                imageViewForeground.setImageResource(R.drawable.help_1_ready_text);
                imageViewBackground.setImageResource(R.drawable.help_1_ready_background);
                return;
            case 2:
                imageViewForeground.setImageResource(R.drawable.help_2_running_text);
                imageViewBackground.setImageResource(R.drawable.help_2_running_background);
                return;
            case 3:
                imageViewForeground.setImageResource(R.drawable.help_3_running_text);
                imageViewBackground.setImageResource(R.drawable.help_2_running_background); // same background as 2
                return;
            case 4:
                imageViewForeground.setImageResource(R.drawable.help_4_ready_text);
                imageViewBackground.setImageResource(R.drawable.help_4_ready_background);
                return;
            case 5:
                imageViewForeground.setImageResource(R.drawable.help_5_running_text);
                imageViewBackground.setImageResource(R.drawable.help_5_running_background);
                return;
        }
        hide();
    }
}
