package com.notification.timer;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.util.Log;

class AlertBuilderSetDone extends AlertDialog.Builder {

    private final static String TAG = "AlertBuilderSetDone";

    AlertBuilderSetDone(final Context context) {
        super(context);
        setTitle(context.getString(R.string.alert_time_out));
        setIcon(context.getDrawable(R.drawable.ic_chronometer_done));
        setMessage(context.getString(R.string.alert_set_done));
        setNegativeButton(context.getString(R.string.alert_no),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int which) {
                        Log.d(TAG, "sending intent STOP");
                        context.sendBroadcast(new Intent(IntentAction.STOP));
                    }
                });
        setPositiveButton(context.getString(R.string.alert_yes),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int which) {
                        Log.d(TAG, "sending intent START");
                        context.sendBroadcast(new Intent(IntentAction.START));
                    }
                });
        setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                Log.d(TAG, " dismissed : sending intent STOP");
                context.sendBroadcast(new Intent(IntentAction.STOP));
            }
        });
    }
}