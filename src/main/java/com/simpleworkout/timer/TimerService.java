package com.simpleworkout.timer;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class TimerService extends Service {

    public static final String TAG = "TimerService";

    private boolean running = false;

    private final IBinder binder = new TimerBinder();
    private TimerServiceReceiver timerServiceReceiver;

    private CountDownPauseTimer countDownPauseTimer;

    private boolean mainActivityVisible = true;
    public void setMainActivityVisible(boolean mainActivityVisible) { this.mainActivityVisible = mainActivityVisible; }

    private AlarmManager alarmManager;
    private PendingIntent pendingIntentAlarm;
    private PowerManager.WakeLock wakeLock;
    private SharedPreferences sharedPreferences;

    // Notification related
    protected InteractiveNotification interactiveNotification;
    private boolean interactiveNotificationAlert = false;
    private boolean interactiveNotificationRebuild = true;

    public void setInteractiveNotificationAlert(boolean interactiveNotificationAlert) {
        this.interactiveNotificationAlert = interactiveNotificationAlert;
    }
    public void setInteractiveNotificationRebuild(boolean interactiveNotificationRebuild) {
        this.interactiveNotificationRebuild = interactiveNotificationRebuild;
    }

    // Running values
    private long timerCurrent = 0;
    private long timerUser = 0;
    private int setsCurrent = 1;
    private int setsUser = 1;
    private State state = State.WAITING;

    public long getTimerCurrent() { return timerCurrent; }
    public long getTimerUser() { return timerUser; }
    public int getSetsCurrent() { return setsCurrent; }
    public int getSetsUser() { return setsUser; }
    public State getState() { return state; }
    public void setState(State state) { this.state = state; saveContextPreferences(); }

    // Settings
    private boolean timerGetReadyEnable = true;
    private int timerGetReady = 15;
    private long timerMinus = 30;
    private long timerPlus = 30;

    public void setTimerGetReadyEnable(boolean timerGetReadyEnable) { this.timerGetReadyEnable = timerGetReadyEnable; }
    public void setTimerGetReady(int timerGetReady) { this.timerGetReady = timerGetReady; }
    public void setTimerMinus(long timerMinus) { this.timerMinus = timerMinus; interactiveNotification.setTimerMinus(timerMinus);}
    public void setTimerPlus(long timerPlus) { this.timerPlus = timerPlus; }

    public enum State {

        WAITING("waiting"),
        READY("ready"),
        RUNNING("running"),
        PAUSED("paused"),
        STOPPED("stopped");

        private String state;
        State(String state) {
            this.state = state;
        }

        @Override
        public String toString() {
            return state;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");
        PowerManager powerManager;

        interactiveNotification = new InteractiveNotification(this);

        alarmManager = (AlarmManager) getBaseContext().getSystemService(Context.ALARM_SERVICE);
        powerManager = (PowerManager) getBaseContext().getSystemService(Context.POWER_SERVICE);
        pendingIntentAlarm = PendingIntent.getBroadcast(getBaseContext(), 0, new Intent(IntentAction.ACQUIRE_WAKELOCK), 0);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SimpleWorkoutTimerWakeLock");

        timerServiceReceiver = new TimerServiceReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(IntentAction.START);
        filter.addAction(IntentAction.STOP);
        filter.addAction(IntentAction.PAUSE);
        filter.addAction(IntentAction.RESUME);
        filter.addAction(IntentAction.RESET);
        filter.addAction(IntentAction.CLEAR);
        filter.addAction(IntentAction.NEXT_SET);
        filter.addAction(IntentAction.NEXT_SET_START);
        filter.addAction(IntentAction.EXTRA_SET);
        filter.addAction(IntentAction.TIMER_MINUS);
        filter.addAction(IntentAction.TIMER_PLUS);
        filter.addAction(IntentAction.SETS_MINUS);
        filter.addAction(IntentAction.SETS_PLUS);
        filter.addAction(IntentAction.NOTIFICATION_DISMISS);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(IntentAction.ACQUIRE_WAKELOCK);
        registerReceiver(timerServiceReceiver, filter);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        loadContextPreferences();

        running = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: running=" + running);
        return START_NOT_STICKY;
    }

    protected void updateNotificationVisibilityScreenLocked() {
        Log.d(TAG, "updateNotificationVisibilityScreenLocked: interactiveNotificationAlert=" + interactiveNotificationAlert);
        if(interactiveNotificationRebuild && interactiveNotificationAlert)
            updateNotificationVisibility(true);
    }

    public void updateNotificationVisibility(boolean visible) {
        Log.d(TAG, "updateNotificationVisibility: visible=" + visible);
        if(!isWaiting()) {
            if (visible) {
                Log.d(TAG, "updateNotificationVisibility: startForeground");
                startForeground(interactiveNotification.getID(), interactiveNotification.getNotification());
                interactiveNotification.setVisible();
                interactiveNotificationRebuild = true;
            }
            else {
                Log.d(TAG, "updateNotificationVisibility: stopForeground");
                stopForeground(true);
                interactiveNotification.dismiss();
                if (interactiveNotificationAlert)
                    notificationDeleted();
            }
            saveContextPreferences();
        }
    }

    private void updateStateIntent(State state) {
        this.state = state;
        if(mainActivityVisible)
            getApplicationContext().sendBroadcast(new Intent(IntentAction.TIMER_STATE).putExtra("state", state.toString()));
    }

    /**
     * Send information to the MainActivity
     */
    private void updateTimerIntent(long time, int sets) {
        if(mainActivityVisible)
            getApplicationContext().sendBroadcast(new Intent(IntentAction.TIMER_UPDATE).putExtra("time", time).putExtra("sets", sets));
    }

    protected void start() {
        Log.d(TAG, "start: timerUser=" + timerUser + ", setsCurrent=" + setsCurrent);
        startCountDown(timerUser);
        updateStateIntent(State.RUNNING);
        interactiveNotification.updateButtonsLayout(InteractiveNotification.ButtonsLayout.RUNNING);
        interactiveNotification.build(0);
        saveContextPreferences();
    }

    private void startContextPreferences() {
        Log.d(TAG, "startContextPreferences: timerCurrent=" + timerCurrent + ", setsCurrent=" + setsCurrent);
        startCountDown(timerCurrent);
        interactiveNotification.updateButtonsLayout(InteractiveNotification.ButtonsLayout.RUNNING);
        interactiveNotification.build(0);
        saveContextPreferences();
    }

    protected void pause() {
        if(state == State.RUNNING) {
            Log.d(TAG, "pause");
            countDownPauseTimer.pause();
            updateStateIntent(State.PAUSED);
            interactiveNotification.updateButtonsLayout(InteractiveNotification.ButtonsLayout.PAUSED);
            interactiveNotification.build(0);
            saveContextPreferences();
        }
    }

    private void pauseContextPreference() {
        Log.d(TAG, "pauseContextPreference: timerCurrent=" + timerCurrent + ", setsCurrent=" + setsCurrent);
        startCountDown(timerCurrent);
        countDownPauseTimer.pause();
        interactiveNotification.updateButtonsLayout(InteractiveNotification.ButtonsLayout.PAUSED);
        interactiveNotification.build(0);
        saveContextPreferences();
    }

    protected void resume() {
        if(state == State.PAUSED) {
            Log.d(TAG,"resume");
            countDownPauseTimer.resume();
            updateStateIntent(State.RUNNING);
            interactiveNotification.updateButtonsLayout(InteractiveNotification.ButtonsLayout.RUNNING);
            interactiveNotification.build(0);
            saveContextPreferences();
        }
    }

    protected void stop() {
        Log.d(TAG,"stop: setCurrent=" + setsCurrent);
        timerCurrent = timerUser;
        if(state == State.RUNNING)
            cancelCountDown();
        updateStateIntent(State.STOPPED);
        updateTimerIntent(timerCurrent, setsCurrent);
        interactiveNotification.updateSetsTextView(setsCurrent);
        interactiveNotification.updateTimerTextView(timerCurrent);
        if(setsCurrent >= 1)
            interactiveNotification.updateButtonsLayout(InteractiveNotification.ButtonsLayout.READY);
        else
            Log.e(TAG, "stop: setCurrent cannot be 0");
        interactiveNotification.build(0);
        saveContextPreferences();
    }

    private void done() {
        // The timer will be stopped from the alerts
        if(mainActivityVisible)
            getApplicationContext().sendBroadcast(new Intent(IntentAction.TIMER_DONE));
        if (setsCurrent-- > 1) {
            Log.d(TAG, "done: set done setsCurrent=" + setsCurrent);
            interactiveNotification.alertSetDone();
            interactiveNotification.build(2);
            interactiveNotificationAlert = true;
            // Update for the next set
            interactiveNotification.updateSetsTextView(setsCurrent);
        } else {
            Log.d(TAG, "done: all sets done setsCurrent=" + setsCurrent);
            interactiveNotification.alertAllSetsDone();
            interactiveNotification.build(2);
            interactiveNotificationAlert = true;
        }
        if(wakeLock != null) {
            if(wakeLock.isHeld())
                wakeLock.release();
        }
        saveContextPreferences();
    }

    protected void nextSet() {
        if(state == State.RUNNING)
            cancelCountDown();
        timerCurrent = timerUser;
        Log.d(TAG, "nextSet: setsCurrent=" + (setsCurrent - 1));
        if(setsCurrent-- > 1) {
            interactiveNotification.updateSetsTextView(setsCurrent);
            interactiveNotification.updateButtonsLayout(InteractiveNotification.ButtonsLayout.SET_DONE);
            interactiveNotificationAlert = true;
        }
        else {
            interactiveNotification.updateAllSetsDoneTextView();
            interactiveNotification.updateButtonsLayout(InteractiveNotification.ButtonsLayout.ALL_SETS_DONE);
            interactiveNotificationAlert = true;
        }
        interactiveNotification.updateTimerTextView(timerUser);
        interactiveNotification.build(0);
        updateStateIntent(State.STOPPED);
        updateTimerIntent(timerUser, setsCurrent);
        if(wakeLock.isHeld())
            wakeLock.release();
        saveContextPreferences();
    }

    protected void nextSetStart() {
        if(state == State.RUNNING)
            cancelCountDown();
        timerCurrent = timerUser;
        Log.d(TAG, "nextSetStart: setsCurrent=" + (setsCurrent - 1));
        if(setsCurrent-- > 1)
            interactiveNotification.updateSetsTextView(setsCurrent);
        else
            Log.e(TAG, "nextSetStart: setsCurrent=" + setsCurrent);
        if(wakeLock.isHeld())
            wakeLock.release();
        Log.d(TAG, "nextSetStart: timerUser=" + timerUser + ", setsCurrent=" + setsCurrent);
        startCountDown(timerUser);
        updateTimerIntent(timerUser, setsCurrent);
        updateStateIntent(State.RUNNING);
        interactiveNotification.updateTimerTextView(timerUser);
        interactiveNotification.updateButtonsLayout(InteractiveNotification.ButtonsLayout.RUNNING);
        interactiveNotification.build(0);
        saveContextPreferences();
    }

    protected void extraSet() {
        timerCurrent = timerUser;
        if(state == State.RUNNING)
            cancelCountDown();
        else
            Log.e(TAG, "extraSet: timerState=" + state.toString() + ", setsCurrent=" + setsCurrent);
        setsCurrent += 1;
        interactiveNotification.updateTimerTextView(timerUser);
        interactiveNotification.updateExtraSetTextViews();
        updateTimerIntent(timerUser, setsCurrent);
        Log.d(TAG, "extraSet: timerUser=" + timerUser + ", setsCurrent=" + setsCurrent);
        startCountDown(timerUser);
        updateStateIntent(State.RUNNING);
        interactiveNotification.updateButtonsLayout(InteractiveNotification.ButtonsLayout.RUNNING);
        interactiveNotification.build(0);
        saveContextPreferences();
    }

    protected void reset() {
        Log.d(TAG, "reset");
        setsCurrent = setsUser;
        timerCurrent = timerUser;
        if(state == State.RUNNING)
            cancelCountDown();
        interactiveNotification.updateSetsTextView(setsUser);
        interactiveNotification.updateTimerTextView(timerUser);
        interactiveNotification.updateButtonsLayout(InteractiveNotification.ButtonsLayout.READY);
        interactiveNotification.build(0);
        updateTimerIntent(timerCurrent, setsCurrent);
        updateStateIntent(State.READY);
        if(wakeLock.isHeld())
            wakeLock.release();
        saveContextPreferences();
    }

    protected void clear() {
        Log.d(TAG, "clear");
        timerCurrent = 0;
        timerUser = 0;
        setsCurrent = 1;
        setsUser = 1;
        updateTimerIntent(timerCurrent, setsCurrent);
        if(state == State.RUNNING)
            cancelCountDown();
        updateStateIntent(State.WAITING);
        // remove the notification and reset the timer to init state
        stopForeground(true);
        interactiveNotification.dismiss();
        saveContextPreferences();
    }

    protected void timerMinus() {
        if(timerCurrent < timerMinus) {
            Log.e(TAG, "timerMinus: impossible timerCurrent=" + timerCurrent);
            return;
        }
        timerCurrent -= timerMinus;
        updateCountDown(TimeUnit.SECONDS.toMillis(timerCurrent));
        Log.d(TAG, "timerMinus: timerCurrent=" + timerCurrent);
        interactiveNotification.updateTimerTextView(timerCurrent);
        interactiveNotification.updateButtonsLayout(InteractiveNotification.ButtonsLayout.RUNNING);
        interactiveNotification.build(0);
    }

    protected void timerPlus() {
        timerCurrent += timerPlus;
        updateCountDown(TimeUnit.SECONDS.toMillis(timerCurrent));
        Log.d(TAG, "timerPlus: timerCurrent=" + timerCurrent);
        interactiveNotification.updateTimerTextView(timerCurrent);
        interactiveNotification.updateButtonsLayout(InteractiveNotification.ButtonsLayout.RUNNING);
        interactiveNotification.build(0);
    }

    protected void setsMinus() {
        if(setsCurrent <= 1) {
            Log.e(TAG, "sets minus setsCurrent=" + setsCurrent);
            return;
        }
        setsCurrent -= 1;
        Log.d(TAG, "setsMinus: setsCurrent=" + setsCurrent);
        interactiveNotification.updateSetsTextView(setsCurrent);
    }

    protected void setsPlus() {
        setsCurrent += 1;
        Log.d(TAG, "setsPlus: setsCurrent=" + setsCurrent);
        interactiveNotification.updateSetsTextView(setsCurrent);
    }

    public void setTimer(long time) {
        Log.d(TAG, "setTimer");
        if(time >= 0) {
            timerUpdate(time);
            if(!isRunning())
                timerUser = time;
            else
                updateCountDown(TimeUnit.SECONDS.toMillis(time));
            Log.d(TAG, "setTimer: timerUser=" + timerUser + ", timerCurrent=" + timerCurrent);
        }
        else
            Log.e(TAG, "setTimer with time=" + time);
    }

    public void setSets(int sets) {
        Log.d(TAG, "setSets");
        if(sets >= 1) {
            setsUpdate(sets);
            if(!isRunning())
                setsUser = sets;
            Log.d(TAG, "setSets: setsUser=" + setsUser + ", setsCurrent=" + setsCurrent);
        }
        else
            Log.e(TAG, "set_timer with sets=" + sets);
    }

    protected void timerUpdate(long time) {
        Log.d(TAG, "timerUpdate: time=" + time + ", timerCurrent=" + timerCurrent);
        int notification_alert_level = 0;
        if(timerCurrent != time) {
            timerCurrent = time;
            if (mainActivityVisible)
                getApplicationContext().sendBroadcast(new Intent(IntentAction.TIMER_UPDATE).putExtra("time", time));
            interactiveNotification.updateTimerTextView(timerCurrent);
            if(state == State.RUNNING)
                interactiveNotification.updateButtonsLayout(InteractiveNotification.ButtonsLayout.RUNNING);
            else if(state == State.WAITING)
                // Get ready for the next step
                interactiveNotification.updateButtonsLayout(InteractiveNotification.ButtonsLayout.READY);
        }
        // Get ready notification light, sound and vibration
        if(time == timerGetReady && timerGetReadyEnable)
            notification_alert_level = 1;
        // Only light for time < timerGetReady
        if(time < timerGetReady && timerGetReadyEnable)
            notification_alert_level = 3;
        interactiveNotification.build(notification_alert_level);
    }

    // Method only called from the MainActivity
    protected void setsUpdate(int sets) {
        Log.d(TAG, "setsUpdate: sets=" + sets + ", setsCurrent=" + setsCurrent + ", setsUser=" + setsUser);
        if(setsCurrent != sets) {
            setsCurrent = sets;
            interactiveNotification.updateSetsTextView(setsCurrent);
        }
        else if(setsCurrent == 1 && setsUser == 1)
            interactiveNotification.updateOneSetTextViews();
        interactiveNotification.build(0);
    }

    private void notificationDeleted() {
        interactiveNotificationAlert = false;
        if(isPaused()) {
            Log.d(TAG, "notificationDeleted");
            interactiveNotification.dismiss();
        }
        else if(setsCurrent >= 1) {
            Log.d(TAG, "notificationDeleted: setsCurrent=" + setsCurrent);
            interactiveNotification.dismiss();
            if(mainActivityVisible) {
                Log.d(TAG, "notificationDeleted: sending STOP action");
                getBaseContext().sendBroadcast(new Intent(IntentAction.STOP));
            }
            stop();
        }
        else {
            Log.d(TAG, "notificationDeleted: setsCurrent=" + setsCurrent);
            interactiveNotification.dismiss();
            if(mainActivityVisible) {
                Log.d(TAG, "notificationDeleted: sending CLEAR action");
                getBaseContext().sendBroadcast(new Intent(IntentAction.CLEAR));
            }
            reset();
        }
    }

    private boolean isRunning() { return (state == State.RUNNING || state == State.PAUSED) ; }

    private boolean isPaused() { return state == State.PAUSED; }

    private boolean isWaiting() { return state == State.WAITING; }

    private void setupAlarmManager() {
        alarmManager.cancel(pendingIntentAlarm);
        long time = System.currentTimeMillis();
        long timerApprox = 7;
        if(timerCurrent - timerGetReady > timerApprox)
            time += TimeUnit.SECONDS.toMillis(timerUser - timerGetReady - timerApprox);
        else if (timerCurrent > timerGetReady)
            time += TimeUnit.SECONDS.toMillis(timerUser - timerGetReady);
        Log.d(TAG, "setupAlarmManager: wakeup the device at time=" + (time - System.currentTimeMillis())/1000 + ", timerCurrent=" + timerCurrent);
        alarmManager.set(AlarmManager.RTC_WAKEUP, time, pendingIntentAlarm);
    }

    protected void acquireWakeLock() {
        if(wakeLock != null) {
            if (!wakeLock.isHeld()) {
                if (timerCurrent <= timerGetReady)
                    Log.e(TAG, "acquireWakeLock: timerGetReady=" + timerGetReady + " is passed timerCurrent=" + timerCurrent);
                else
                    Log.d(TAG, "acquireWakeLock: timerCurrent=" + timerCurrent);
                wakeLock.acquire();
            } else
                Log.e(TAG, "acquireWakeLock: wakeLock isHeld=true");
        }
    }

    private void startCountDown(long time) {
        setupAlarmManager();
        countDownPauseTimer = new CountDownPauseTimer(TimeUnit.SECONDS.toMillis(time), TimeUnit.SECONDS.toMillis(1)) {

            @Override
            public void onTick(long millisUntilFinished) {
                timerUpdate(TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished));
                Log.d("CountDownPauseTimer", "onTick: millisUntilFinished=" + millisUntilFinished);
            }

            @Override
            public void onFinish() {
                Log.d("CountDownPauseTimer", "onFinish");
                done();
            }
        };
        countDownPauseTimer.start();
    }

    private void updateCountDown(long time) {
        setupAlarmManager();
        countDownPauseTimer.update(time);
    }

    private void cancelCountDown() {
        countDownPauseTimer.cancel();
        Log.d(TAG, "cancelCountDown: canceling alarms");
        alarmManager.cancel(pendingIntentAlarm);
    }

    private void saveContextPreferences() {
        Log.d(TAG, "saveContextPreferences: timerCurrent=" + timerCurrent + ", timerUser=" + timerUser + ", setsCurrent=" + setsCurrent
                + ", setsUser=" + setsUser + ", state=" + state + ", mainActivityVisible=" + mainActivityVisible);
        SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
        sharedPreferencesEditor.putLong("timerService_timerEnd", System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timerCurrent));
        sharedPreferencesEditor.putLong("timerService_timerCurrent", timerCurrent);
        sharedPreferencesEditor.putLong("timerService_timerUser", timerUser);
        sharedPreferencesEditor.putInt("timerService_setsCurrent", setsCurrent);
        sharedPreferencesEditor.putInt("timerService_setsUser", setsUser);
        sharedPreferencesEditor.putString("timerService_state", state.toString());
        sharedPreferencesEditor.putBoolean("timerService_mainActivityVisible", mainActivityVisible);
        sharedPreferencesEditor.apply();
    }

    private void loadContextPreferences() {
        long timerEnd = sharedPreferences.getLong("timerService_timerEnd", System.currentTimeMillis());
        timerCurrent = sharedPreferences.getLong("timerService_timerCurrent", timerCurrent);
        timerUser = sharedPreferences.getLong("timerService_timerUser", timerUser);
        setsCurrent = sharedPreferences.getInt("timerService_setsCurrent", setsCurrent);
        setsUser = sharedPreferences.getInt("timerService_setsUser", setsUser);
        state = State.valueOf(sharedPreferences.getString("timerService_state", state.toString()).toUpperCase(Locale.US));
        mainActivityVisible = sharedPreferences.getBoolean("timerService_mainActivityVisible", mainActivityVisible);

        if(state == State.RUNNING){
            timerCurrent = TimeUnit.MILLISECONDS.toSeconds(timerEnd - System.currentTimeMillis());
            startContextPreferences();
        }
        else if(state == State.PAUSED) {
            pauseContextPreference();
        }

        if(!mainActivityVisible) {
            updateNotificationVisibility(true);
            interactiveNotification.updateSetsTextView(setsCurrent);
            interactiveNotification.updateTimerTextView(timerCurrent);
            switch (state) {
                case RUNNING:
                    interactiveNotification.updateButtonsLayout(InteractiveNotification.ButtonsLayout.RUNNING);
                    break;
                case PAUSED:
                    interactiveNotification.updateButtonsLayout(InteractiveNotification.ButtonsLayout.PAUSED);
                    break;
                case STOPPED:
                case READY:
                    interactiveNotification.updateButtonsLayout(InteractiveNotification.ButtonsLayout.READY);
                    break;
                case WAITING:
                    interactiveNotification.updateButtonsLayout(InteractiveNotification.ButtonsLayout.NO_LAYOUT);
                    Log.e(TAG, "loadContextPreferences: cannot show the interactiveNotification with state=" + state);
                    break;
            }
            interactiveNotification.build(0);
        }
        Log.d(TAG, "loadContextPreferences: timerCurrent=" + timerCurrent + ", timerUser=" + timerUser + ", setsCurrent=" + setsCurrent
            + ", setsUser=" + setsUser + ", state=" + state + ", mainActivityVisible=" + mainActivityVisible);
    }

    public class TimerBinder extends Binder {
        TimerService getService() {
            Log.d(TAG, "TimerServiceBinder getService");
            return TimerService.this;
        }
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy");
        super.onDestroy();
        interactiveNotification.dismiss();
        unregisterReceiver(timerServiceReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(TAG, "onBind");
        return binder;
    }

    @Override
    public void onRebind(Intent intent) {
        Log.v(TAG, "onRebind");
        getApplicationContext().sendBroadcast(new Intent(IntentAction.TIMER_REBIND));
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.v(TAG, "onUnbind");
        return true;
    }
}
