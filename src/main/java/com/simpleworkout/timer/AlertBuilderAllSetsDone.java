package com.simpleworkout.timer;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.util.Log;

/**
 * Created by guillaume on 09/06/16.
 */
public class AlertBuilderAllSetsDone extends AlertDialog.Builder {

    private final static String TAG = "AlertBuilderAllSetsDone";

    public AlertBuilderAllSetsDone(final Context context) {
        super(context);
        setMessage("Time out ! It's time to do your last set !");
        setNeutralButton("EXTRA SET",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int which) {
                        Log.d(TAG, "sending intent EXTRA_SET");
                        context.sendBroadcast(new Intent(IntentAction.EXTRA_SET));
                    }
                });
        setPositiveButton("RESET",
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
