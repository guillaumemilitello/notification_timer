package com.notification.timer;

import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

class HelpOverlay {

    private static final String TAG = "HelpOverlay";

    private LinearLayout layout;
    private ImageView imageView;
    private TextView textViewInfo, textViewDone;

    private boolean showFirstRun;
    private int currentImageId;

    HelpOverlay(MainActivity mainActivity) {
        layout = mainActivity.findViewById(R.id.layoutHelp);
        imageView = mainActivity.findViewById(R.id.imageViewHelp);
        textViewInfo = mainActivity.findViewById(R.id.textViewHelpButtonInfo);
        textViewDone = mainActivity.findViewById(R.id.textViewHelpButtonDone);
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

        showFirstRun = false;
    }

    private void setVisible(boolean visible) {
        Log.d(TAG, "setVisible: visible=" + visible);
        int visibility = visible? View.VISIBLE : View.INVISIBLE;
        layout.setVisibility(visibility);
        imageView.setVisibility(visibility);
    }

    void showFirstRun() {
        Log.d(TAG, "showFirstRun");
        showFirstRun = true;
        setVisible(true);
        layout.setWeightSum(9);
        textViewInfo.setVisibility(View.GONE);
        textViewDone.setVisibility(View.VISIBLE);
        textViewDone.setText(R.string.help_ok);
        imageView.setImageResource(R.drawable.help_first_run);
        imageView.setScaleType(ImageView.ScaleType.FIT_END);
    }

    void show() {
        Log.d(TAG, "show");
        showFirstRun = false;
        setVisible(true);
        layout.setWeightSum(10);
        textViewInfo.setVisibility(View.VISIBLE);
        textViewDone.setText(R.string.help_quit);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        currentImageId = 0;
        showImage(currentImageId);
    }

    private void next() {
        Log.d(TAG, "next");
        if (showFirstRun) {
            showFirstRun = false;
            hide();
        } else {
            showImage(++currentImageId);
        }
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
