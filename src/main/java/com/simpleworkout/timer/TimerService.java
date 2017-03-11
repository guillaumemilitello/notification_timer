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
    private int setsInit = 0;
    private int setsCurrent = 0;
    private int setsUser = 1;
    private State state = State.WAITING;

    public long getTimerCurrent() { return timerCurrent; }
    public long getTimerUser() { return timerUser; }
    public int getSetsInit() { return setsInit; }
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
    public void setTimerMinus(long timerMinus) { this.timerMinus = timerMinus;}
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
                startForeground(interactiveNotification.ID, interactiveNotification.getNotification());
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
        if(mainActivityVisible) {
            getApplicationContext().sendBroadcast(new Intent(IntentAction.TIMER_STATE).putExtra("state", state.toString()));
        }
    }

    /**
     * Send information to the MainActivity
     */
    private void updateTimerIntent(long time) {
        if(mainActivityVisible) {
            getApplicationContext().sendBroadcast(new Intent(IntentAction.TIMER_UPDATE).putExtra("time", time));
        }
    }

    private void updateTimerIntent(long time, int sets) {
        if(mainActivityVisible) {
            getApplicationContext().sendBroadcast(new Intent(IntentAction.TIMER_UPDATE).putExtra("time", time).putExtra("sets", sets));
        }
    }

    protected void start() {
        Log.d(TAG, "start: timerUser=" + timerUser + ", setsCurrent=" + setsCurrent);
        startCountDown(timerUser);
        updateStateIntent(State.RUNNING);

        interactiveNotification.updateButtonsLayout(InteractiveNotification.ButtonsLayout.RUNNING, InteractiveNotification.NotificationMode.UPDATE);

        saveContextPreferences();
    }

    private void startContextPreferences() {
        Log.d(TAG, "startContextPreferences: timerCurrent=" + timerCurrent + ", setsCurrent=" + setsCurrent);
        startCountDown(timerCurrent);

        interactiveNotification.updateButtonsLayout(InteractiveNotification.ButtonsLayout.RUNNING, InteractiveNotification.NotificationMode.UPDATE);

        saveContextPreferences();
    }

    protected void pause() {
        if(state == State.RUNNING) {
            Log.d(TAG, "pause");

            countDownPauseTimer.pause();

            updateStateIntent(State.PAUSED);

            interactiveNotification.updateButtonsLayout(InteractiveNotification.ButtonsLayout.PAUSED, InteractiveNotification.NotificationMode.UPDATE);

            saveContextPreferences();
        }
    }

    private void pauseContextPreference() {
        Log.d(TAG, "pauseContextPreference: timerCurrent=" + timerCurrent + ", setsCurrent=" + setsCurrent);

        startCountDown(timerCurrent);
        countDownPauseTimer.pause();

        interactiveNotification.updateButtonsLayout(InteractiveNotification.ButtonsLayout.PAUSED, InteractiveNotification.NotificationMode.UPDATE);

        saveContextPreferences();
    }

    protected void resume() {
        if(state == State.PAUSED) {
            Log.d(TAG,"resume");

            countDownPauseTimer.resume();
            updateStateIntent(State.RUNNING);

            interactiveNotification.updateButtonsLayout(InteractiveNotification.ButtonsLayout.RUNNING, InteractiveNotification.NotificationMode.UPDATE);

            saveContextPreferences();
        }
    }

    protected void stop() {
        Log.d(TAG,"stop: setCurrent=" + setsCurrent);

        timerCurrent = timerUser;

        if(state == State.RUNNING) {
            cancelCountDown();
        }
        updateStateIntent(State.STOPPED);
        updateTimerIntent(timerCurrent, setsCurrent);

        interactiveNotification.update(setsCurrent, timerCurrent, InteractiveNotification.ButtonsLayout.READY, InteractiveNotification.NotificationMode.UPDATE);

        saveContextPreferences();
    }

    private void done() {
        Log.d(TAG, "done: setsCurrent=" + setsCurrent );

        // The timer will be stopped from the alerts
        if(mainActivityVisible) {
            getApplicationContext().sendBroadcast(new Intent(IntentAction.TIMER_DONE));
        }

        setsCurrent++;
        doneInteractiveNotification(InteractiveNotification.NotificationMode.LIGHT_SOUND_LONG_VIBRATE);

        releaseWakeLock();

        saveContextPreferences();
    }

    protected void nextSet() {
        Log.d(TAG, "nextSet: setsCurrent=" + setsCurrent );

        if(state == State.RUNNING) {
            cancelCountDown();
        }

        setsCurrent++;
        timerCurrent = timerUser;
        doneInteractiveNotification(InteractiveNotification.NotificationMode.UPDATE);

        updateStateIntent(State.STOPPED);
        updateTimerIntent(timerUser, setsCurrent);

        releaseWakeLock();

        saveContextPreferences();
    }

    private void doneInteractiveNotification(InteractiveNotification.NotificationMode notificationMode) {
        if (setsCurrent < setsUser) {
            interactiveNotification.updateButtonsLayout(InteractiveNotification.ButtonsLayout.SET_DONE);
        } else {
            interactiveNotification.updateButtonsLayout(InteractiveNotification.ButtonsLayout.ALL_SETS_DONE);
        }
        interactiveNotification.updateTimerCurrent(timerCurrent);
        interactiveNotification.updateSetsCurrent(setsCurrent, notificationMode);
        interactiveNotificationAlert = true;
        Log.d(TAG, "goToNextSet: setsCurrent=" + setsCurrent + ", setsUser=" + setsUser);
    }

    protected void nextSetStart() {
        Log.d(TAG, "nextSetStart: setsCurrent=" + setsCurrent);

        setsCurrent++;

        timerCurrent = timerUser;

        if(state == State.RUNNING) {
            cancelCountDown();
        }
        startCountDown(timerUser);
        updateTimerIntent(timerUser, setsCurrent);
        updateStateIntent(State.RUNNING);

        interactiveNotification.update(setsCurrent, timerCurrent, InteractiveNotification.ButtonsLayout.RUNNING, InteractiveNotification.NotificationMode.UPDATE);

        releaseWakeLock();

        saveContextPreferences();
    }

    protected void extraSet() {
        Log.d(TAG, "extraSet: setsCurrent=" + setsCurrent);

        timerCurrent = timerUser;

        if(state == State.RUNNING) {
            cancelCountDown();
        }
        startCountDown(timerUser);
        updateTimerIntent(timerUser, setsCurrent);
        updateStateIntent(State.RUNNING);

        interactiveNotification.update(setsCurrent, timerCurrent, InteractiveNotification.ButtonsLayout.RUNNING, InteractiveNotification.NotificationMode.UPDATE);

        saveContextPreferences();
    }

    protected void reset() {
        Log.d(TAG, "reset");

        setsCurrent = setsInit;
        timerCurrent = timerUser;

        if(state == State.RUNNING) {
            cancelCountDown();
        }

        interactiveNotification.update(setsCurrent, timerCurrent, InteractiveNotification.ButtonsLayout.READY, InteractiveNotification.NotificationMode.UPDATE);

        updateTimerIntent(timerCurrent, setsCurrent);
        updateStateIntent(State.READY);

        releaseWakeLock();

        saveContextPreferences();
    }

    protected void clear() {
        Log.d(TAG, "clear");
        if(state == State.RUNNING) {
            cancelCountDown();
        }

        timerCurrent = 0;
        timerUser = 0;
        setsInit = 0;
        setsCurrent = 0;
        setsUser = 0;

        updateTimerIntent(timerCurrent, setsCurrent);
        updateStateIntent(State.WAITING);

        // remove the notification and reset the timer to init state
        stopForeground(true);
        interactiveNotification.dismiss();

        saveContextPreferences();
    }

    protected void timerMinus() {
        timerCurrent -= timerMinus;
        Log.d(TAG, "timerMinus: timerCurrent=" + timerCurrent);

        updateCountDown(TimeUnit.SECONDS.toMillis(timerCurrent));

        interactiveNotification.updateTimerCurrent(timerCurrent);
        interactiveNotification.updateButtonsLayout(InteractiveNotification.ButtonsLayout.RUNNING, InteractiveNotification.NotificationMode.UPDATE);
    }

    protected void timerPlus() {
        timerCurrent += timerPlus;
        Log.d(TAG, "timerPlus: timerCurrent=" + timerCurrent);

        updateCountDown(TimeUnit.SECONDS.toMillis(timerCurrent));

        interactiveNotification.updateTimerCurrent(timerCurrent);
        interactiveNotification.updateButtonsLayout(InteractiveNotification.ButtonsLayout.RUNNING, InteractiveNotification.NotificationMode.UPDATE);
    }

    protected void setsMinus() {
        if(setsCurrent <= 1) {
            Log.e(TAG, "sets minus setsCurrent=" + setsCurrent);
            return;
        }
        setsCurrent -= 1;
        Log.d(TAG, "setsMinus: setsCurrent=" + setsCurrent);

        interactiveNotification.updateSetsCurrent(setsCurrent, InteractiveNotification.NotificationMode.UPDATE);
    }

    protected void setsPlus() {
        setsCurrent += 1;
        Log.d(TAG, "setsPlus: setsCurrent=" + setsCurrent);

        interactiveNotification.updateSetsCurrent(setsCurrent, InteractiveNotification.NotificationMode.UPDATE);
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

    public void setSetsInit(int sets) {
        Log.d(TAG, "setSetsInit: setsInit=" + sets);
        setsInit = sets;
    }

    public void setSetsCurrent(int sets) {
        Log.d(TAG, "setSetsCurrent: setsCurrent=" + sets);
        setsCurrent = sets;
        interactiveNotification.updateSetsCurrent(setsCurrent, InteractiveNotification.NotificationMode.UPDATE);
    }

    public void setSetsUser(int sets) {
        Log.d(TAG, "setSetsUser: setsUser=" + sets);
        setsUser = sets;
        interactiveNotification.updateSetsUser(setsUser, InteractiveNotification.NotificationMode.UPDATE);
    }

    public void setTimerUser(long timer) {
        Log.d(TAG, "setTimerUser: timerUser=" + timer);
        timerUser = timer;
    }

    public void setTimerCurrent(long timer) {
        Log.d(TAG, "setTimerCurrent: timerCurrent=" + timer);
        timerCurrent = timer;
        interactiveNotification.updateTimerCurrent(timer, InteractiveNotification.NotificationMode.UPDATE);
    }

    protected void timerUpdate(long time) {
        Log.d(TAG, "timerUpdate: time=" + time + ", timerCurrent=" + timerCurrent);

        if(timerCurrent != time) {
            timerCurrent = time;
            updateTimerIntent(timerCurrent);
        }

        if(timerGetReadyEnable && timerCurrent == timerGetReady) {
            interactiveNotification.updateTimerCurrent(timerCurrent, InteractiveNotification.NotificationMode.LIGHT_SOUND_SHORT_VIBRATE);
        }
        else if(timerGetReadyEnable && timerCurrent < timerGetReady) {
            interactiveNotification.updateTimerCurrent(timerCurrent, InteractiveNotification.NotificationMode.LIGHT_ONLY);
        }
        else {
            interactiveNotification.updateTimerCurrent(timerCurrent, InteractiveNotification.NotificationMode.UPDATE);
        }
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

        if ((timerGetReadyEnable && (timerCurrent - timerGetReady) > timerApprox) || timerCurrent > timerApprox) {
            time += TimeUnit.SECONDS.toMillis(timerCurrent - timerGetReady - timerApprox);
        } else if ((timerGetReadyEnable && timerCurrent > timerApprox) || timerCurrent > 0) {
            time += TimeUnit.SECONDS.toMillis(timerUser - timerGetReady);
        }

        Log.d(TAG, "setupAlarmManager: wakeup the device at time=" + (time - System.currentTimeMillis())/1000 + ", timerCurrent=" + timerCurrent);
        alarmManager.set(AlarmManager.RTC_WAKEUP, time, pendingIntentAlarm);
    }

    protected void acquireWakeLock() {
        if(wakeLock != null) {
            if (!wakeLock.isHeld()) {
                if ((timerGetReadyEnable && timerCurrent <= timerGetReady) || timerCurrent <= 0)
                    Log.e(TAG, "acquireWakeLock: timerGetReadyEnable=" + timerGetReadyEnable + ", timerGetReady=" + timerGetReady + " is passed timerCurrent=" + timerCurrent);
                else
                    Log.d(TAG, "acquireWakeLock: timerCurrent=" + timerCurrent);
                wakeLock.acquire();
            } else
                Log.e(TAG, "acquireWakeLock: wakeLock isHeld=true");
        }
    }

    private void releaseWakeLock() {
        if(wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
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
                + ", setsInit=" + setsInit + ", setsUser=" + setsUser + ", state=" + state + ", mainActivityVisible=" + mainActivityVisible);
        SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
        sharedPreferencesEditor.putLong(getString(R.string.pref_timer_service_timer_end), System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timerCurrent));
        sharedPreferencesEditor.putLong(getString(R.string.pref_timer_service_timer_current), timerCurrent);
        sharedPreferencesEditor.putLong(getString(R.string.pref_timer_service_timer_user), timerUser);
        sharedPreferencesEditor.putInt(getString(R.string.pref_timer_service_sets_init), setsInit);
        sharedPreferencesEditor.putInt(getString(R.string.pref_timer_service_sets_current), setsCurrent);
        sharedPreferencesEditor.putInt(getString(R.string.pref_timer_service_sets_user), setsUser);
        sharedPreferencesEditor.putString(getString(R.string.pref_timer_service_state), state.toString());
        sharedPreferencesEditor.putBoolean(getString(R.string.pref_timer_service_main_activity_visible), mainActivityVisible);
        sharedPreferencesEditor.apply();
    }

    private void loadContextPreferences() {
        long timerEnd = sharedPreferences.getLong(getString(R.string.pref_timer_service_timer_end), System.currentTimeMillis());
        timerCurrent = sharedPreferences.getLong(getString(R.string.pref_timer_service_timer_current), timerCurrent);
        timerUser = sharedPreferences.getLong(getString(R.string.pref_timer_service_timer_user), timerUser);
        setsInit = sharedPreferences.getInt(getString(R.string.pref_timer_service_sets_init), setsInit);
        setSetsCurrent(sharedPreferences.getInt(getString(R.string.pref_timer_service_sets_current), setsCurrent));
        setSetsUser(sharedPreferences.getInt(getString(R.string.pref_timer_service_sets_user), setsUser));
        state = State.valueOf(sharedPreferences.getString(getString(R.string.pref_timer_service_state), state.toString()).toUpperCase(Locale.US));
        mainActivityVisible = sharedPreferences.getBoolean(getString(R.string.pref_timer_service_main_activity_visible), mainActivityVisible);

        if(state == State.RUNNING){
            timerCurrent = TimeUnit.MILLISECONDS.toSeconds(timerEnd - System.currentTimeMillis());
            startContextPreferences();
        }
        else if(state == State.PAUSED) {
            pauseContextPreference();
        }

        if(!mainActivityVisible) {
            updateNotificationVisibility(true);
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
                default:
                    interactiveNotification.updateButtonsLayout(InteractiveNotification.ButtonsLayout.NO_LAYOUT);
                    break;
            }
            interactiveNotification.updateSetsCurrent(setsCurrent);
            interactiveNotification.updateTimerCurrent(timerCurrent, InteractiveNotification.NotificationMode.UPDATE);
        }
        Log.d(TAG, "loadContextPreferences: timerCurrent=" + timerCurrent + ", timerUser=" + timerUser + ", setsCurrent=" + setsCurrent
            + ", setsInit=" + setsInit + ", setsUser=" + setsUser + ", state=" + state + ", mainActivityVisible=" + mainActivityVisible);
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
