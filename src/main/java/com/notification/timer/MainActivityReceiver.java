
package com.notification.timer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Locale;

public class MainActivityReceiver extends BroadcastReceiver {

    private static final String TAG = "MainActivityReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null) {
            switch (action) {
                case IntentAction.START:
                    ((MainActivity) context).start();
                    break;
                case IntentAction.PAUSE:
                    ((MainActivity) context).pause();
                    break;
                case IntentAction.RESUME:
                    ((MainActivity) context).resume();
                    break;
                case IntentAction.STOP:
                    ((MainActivity) context).stop();
                    break;
                case IntentAction.CLEAR:
                    ((MainActivity) context).clear();
                    break;
                case IntentAction.NEXT_SET:
                    ((MainActivity) context).nextSet();
                    break;
                case IntentAction.NEXT_SET_START:
                    ((MainActivity) context).nextSetStart();
                    break;
                case IntentAction.RESET:
                    ((MainActivity) context).reset();
                    break;
                case IntentAction.EXTRA_SET:
                    ((MainActivity) context).extraSet();
                    break;
                case IntentAction.TIMER_MINUS:
                    ((MainActivity) context).timerMinus();
                    break;
                case IntentAction.TIMER_PLUS:
                    ((MainActivity) context).timerPlus();
                    break;
                case IntentAction.SETS_MINUS:
                    ((MainActivity) context).setsMinus();
                    break;
                case IntentAction.SETS_PLUS:
                    ((MainActivity) context).setsPlus();
                    break;
                case IntentAction.TIMER_STATE:
                    String state = intent.getStringExtra("state");
                    if (state != null) {
                        ((MainActivity) context).updateTimerState(TimerService.State.valueOf(state.toUpperCase(Locale.US)));
                    }
                    break;
                case IntentAction.TIMER_UPDATE:
                    long time = intent.getLongExtra("time", -1);
                    if (time != -1) {
                        ((MainActivity) context).timerUpdate(time);
                    }
                    int sets = intent.getIntExtra("sets", -1);
                    if (sets != -1) {
                        ((MainActivity) context).setsUpdate(sets);
                    }
                    break;
                case IntentAction.TIMER_DONE:
                    ((MainActivity) context).done();
                    break;
                case IntentAction.TIMER_REBIND:
                    ((MainActivity) context).timerServiceRebind();
                    break;
                case IntentAction.NOTIFICATION_DISMISS:
                    ((MainActivity) context).clear();
                    break;
                default:
                    Log.e(TAG, "wrong intent event=" + intent.getAction());
            }
        }
    }
}
