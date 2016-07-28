package com.simpleworkout.timer;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.util.Log;

/**
 * Created by guillaume on 09/06/16.
 */
public class AlertBuilderSetDone extends AlertDialog.Builder {

    private final static String TAG = "AlertBuilderSetDone";

    public AlertBuilderSetDone(final Context context) {
        super(context);
        setMessage("Time out ! It's time to do your set then select the action to do next");
        setNegativeButton("STOP",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int which) {
                        Log.d(TAG, "sending intent STOP");
                        context.sendBroadcast(new Intent(IntentAction.STOP));
                    }
                });
        setPositiveButton("CONTINUE",
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