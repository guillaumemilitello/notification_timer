package com.simpleworkout.timer;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.util.Log;

public class AlertBuilderAllSetsDone extends AlertDialog.Builder {

    private final static String TAG = "AlertBuilderAllSetsDone";

    public AlertBuilderAllSetsDone(final Context context) {
        super(context);
        setMessage(context.getString(R.string.alert_all_sets_done));
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
        setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                Log.d(TAG, " dismissed : sending intent RESET");
                context.sendBroadcast(new Intent(IntentAction.RESET));
            }
        });
    }
}
