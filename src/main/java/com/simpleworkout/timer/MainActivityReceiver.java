
package com.simpleworkout.timer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Locale;

public class MainActivityReceiver extends BroadcastReceiver {

    private static final String TAG = "MainActivityReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "received a notification intent action=" + intent.getAction());

        switch (intent.getAction()) {
            case IntentAction.START:
                ((MainActivity)context).start();
                break;
            case IntentAction.PAUSE:
                ((MainActivity)context).pause();
                break;
            case IntentAction.RESUME:
                ((MainActivity)context).resume();
                break;
            case IntentAction.STOP:
                ((MainActivity)context).stop();
                break;
            case IntentAction.CLEAR:
                ((MainActivity)context).clear();
                break;
            case IntentAction.NEXT_SET:
                ((MainActivity)context).nextSet();
                break;
            case IntentAction.NEXT_SET_START:
                ((MainActivity)context).nextSetStart();
                break;
            case IntentAction.RESET:
                ((MainActivity)context).reset();
                break;
            case IntentAction.EXTRA_SET:
                ((MainActivity)context).extraSet();
                break;
            case IntentAction.TIMER_MINUS:
                ((MainActivity)context).timerMinus();
                break;
            case IntentAction.TIMER_PLUS:
                ((MainActivity)context).timerPlus();
                break;
            case IntentAction.SETS_MINUS:
                ((MainActivity)context).setsMinus();
                break;
            case IntentAction.SETS_PLUS:
                ((MainActivity)context).setsPlus();
                break;
            case IntentAction.TIMER_STATE:
                if (intent.hasExtra("state")) {
                    String state = intent.getExtras().getString("state");
                    if(state != null)
                        ((MainActivity)context).updateTimerState(TimerService.State.valueOf(state.toUpperCase(Locale.US)));
                }
                break;
            case IntentAction.TIMER_UPDATE:
                if (intent.hasExtra("time"))
                    ((MainActivity)context).timerUpdate(intent.getExtras().getLong("time"));
                if (intent.hasExtra("sets"))
                    ((MainActivity)context).setsUpdate(intent.getExtras().getInt("sets"));
                break;
            case IntentAction.TIMER_DONE:
                ((MainActivity)context).done();
                break;
            case IntentAction.TIMER_REBIND:
                ((MainActivity)context).timerServiceRebind();
                break;
            default:
                Log.e(TAG, "wrong intent event=" + intent.getAction());
        }
    }
}
