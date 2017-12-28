package com.notification.timer;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.util.Log;

class AlertBuilderAllSetsDone extends AlertDialog.Builder {

    private final static String TAG = "AlertBuilderAllSetsDone";

    AlertBuilderAllSetsDone(final Context context) {
        super(context);
        setTitle(context.getString(R.string.alert_time_out));
        setIcon(context.getDrawable(R.drawable.ic_chronometer_done));
        setMessage(context.getString(R.string.alert_all_set_done));
        setNeutralButton(context.getString(R.string.alert_extra_set),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int which) {
                        Log.d(TAG, "sending intent EXTRA_SET");
                        context.sendBroadcast(new Intent(IntentAction.EXTRA_SET));
                    }
                });
        setPositiveButton(context.getString(R.string.alert_reset),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int which) {
                        Log.d(TAG, "sending intent RESET");
                        context.sendBroadcast(new Intent(IntentAction.RESET));
                    }
                });
        setNegativeButton(context.getString(R.string.alert_clear),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int which) {
                        Log.d(TAG, "sending intent CLEAR");
                        context.sendBroadcast(new Intent(IntentAction.CLEAR));
                    }
                });
        setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                Log.d(TAG, " dismissed : sending intent RESET");
                context.sendBroadcast(new Intent(IntentAction.RESET));
            }
        });
    }
}
