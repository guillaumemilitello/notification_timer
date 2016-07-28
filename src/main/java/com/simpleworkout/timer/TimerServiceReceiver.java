package com.simpleworkout.timer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by guillaume on 13/07/16.
 */
public class TimerServiceReceiver extends BroadcastReceiver {

    private static final String TAG = "TimerServiceReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()) {
            case IntentAction.START:
                ((TimerService)context).setInteractiveNotificationAlert(false);
                ((TimerService)context).start();
                break;
            case IntentAction.PAUSE:
                ((TimerService)context).pause();
                break;
            case IntentAction.RESUME:
                ((TimerService)context).resume();
                break;
            case IntentAction.EXTRA_SET:
                ((TimerService)context).setInteractiveNotificationAlert(false);
                ((TimerService)context).extraSet();
                break;
            case IntentAction.NEXT_SET:
                ((TimerService)context).nextSet();
                break;
            case IntentAction.NEXT_SET_START:
                ((TimerService)context).nextSetStart();
                break;
            case IntentAction.STOP:
                ((TimerService)context).setInteractiveNotificationAlert(false);
                ((TimerService)context).stop();
                break;
            case IntentAction.RESET:
                ((TimerService)context).setInteractiveNotificationAlert(false);
                ((TimerService)context).reset();
                break;
            case IntentAction.CLEAR:
                ((TimerService)context).clear();
                break;
            case IntentAction.TIMER_MINUS:
                ((TimerService)context).timerMinus();
                break;
            case IntentAction.TIMER_PLUS:
                ((TimerService)context).timerPlus();
                break;
            case IntentAction.SETS_MINUS:
                ((TimerService)context).setsMinus();
                break;
            case IntentAction.SETS_PLUS:
                ((TimerService)context).setsPlus();
                break;
            case IntentAction.NOTIFICATION_DISMISS:
                ((TimerService)context).updateNotificationVisibility(false);
                ((TimerService)context).setInteractiveNotificationAlert(false);
                break;
            case Intent.ACTION_SCREEN_ON:
                // Force rebuild the notification when the screen gets back on
                ((TimerService)context).updateNotificationVisibilityScreenLocked();
                break;
            case IntentAction.ACQUIRE_WAKELOCK:
                // The device needs to be awake for the last notifications with light and sound
                ((TimerService)context).acquireWakeLock();
                break;
            default:
                Log.e(TAG, "BroadcastReceiver: unsupported intent action=" + intent.getAction());
        }
    }
}
