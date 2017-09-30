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

    private static final String TAG = "TimerService";

    // margin for not missing the notification
    private static final long WAKELOCK_TIME_APPROX = 3;

    private boolean running = false;

    private final IBinder binder = new TimerBinder();
    private TimerServiceReceiver timerServiceReceiver;

    private CountDownPauseTimer countDownPauseTimer;

    private boolean mainActivityVisible = true;

    public void setMainActivityVisible(boolean mainActivityVisible) {
        this.mainActivityVisible = mainActivityVisible;
        saveContextPreferences(CONTEXT_PREFERENCE_MAIN_ACTIVITY_VISIBLE);
    }

    private AlarmManager alarmManager;
    private PendingIntent pendingIntentAlarm;
    private PowerManager.WakeLock wakeLock;
    private SharedPreferences sharedPreferences;

    // Notification related
    protected InteractiveNotification interactiveNotification;
    private boolean interactiveNotificationAlertDone = false;
    private boolean interactiveNotificationDone = false;

    public void setInteractiveNotificationAlertDone() {
        interactiveNotificationAlertDone = true;
    }

    public void setInteractiveNotificationDone(boolean interactiveNotificationDone) {
        this.interactiveNotificationDone = interactiveNotificationDone;
    }

    // Running values
    private long timerCurrent = 0;
    private long timerUser = 0;
    private int setsInit = 0;
    private int setsCurrent = 0;
    private int setsUser = 1;
    private State state = State.WAITING;

    public long getTimerCurrent() {
        return timerCurrent;
    }

    public long getTimerUser() {
        return timerUser;
    }

    public int getSetsInit() {
        return setsInit;
    }

    public int getSetsCurrent() {
        return setsCurrent;
    }

    public int getSetsUser() {
        return setsUser;
    }

    public State getState() {
        return state;
    }

    public void setReadyState() {
        this.state = State.READY;
        saveContextPreferences(CONTEXT_PREFERENCE_STATE);
        interactiveNotification.updateButtonsLayout(InteractiveNotification.ButtonsLayout.READY);
    }

    // Settings
    private boolean timerGetReadyEnable = true;
    private int timerGetReady = 15;
    private long timerMinus = 30;
    private long timerPlus = 30;

    public void setTimerGetReadyEnable(boolean timerGetReadyEnable) {
        this.timerGetReadyEnable = timerGetReadyEnable;
        setupAlarmManager();
    }

    public void setTimerGetReady(int timerGetReady) {
        this.timerGetReady = timerGetReady;
    }

    public void setTimerMinus(long timerMinus) {
        this.timerMinus = timerMinus;
    }

    public void setTimerPlus(long timerPlus) {
        this.timerPlus = timerPlus;
    }

    // context preferences save
    private long timerCurrentSaving = 0;
    private static final int TIMER_CURRENT_SAVING_INTERVAL = 15;
    private static final int CONTEXT_PREFERENCE_TIMER_END = 0x01;
    private static final int CONTEXT_PREFERENCE_TIMER_CURRENT = 0x02;
    private static final int CONTEXT_PREFERENCE_TIMER_USER = 0x04;
    private static final int CONTEXT_PREFERENCE_SETS_INIT = 0x08;
    private static final int CONTEXT_PREFERENCE_SETS_CURRENT = 0x0A;
    private static final int CONTEXT_PREFERENCE_SETS_USER = 0x10;
    private static final int CONTEXT_PREFERENCE_STATE = 0x20;
    private static final int CONTEXT_PREFERENCE_MAIN_ACTIVITY_VISIBLE = 0x40;

    public enum State {

        WAITING("waiting"),
        READY("ready"),
        RUNNING("running"),
        PAUSED("paused");

        private final String state;

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

    void updateNotificationVisibilityScreenLocked() {
        Log.d(TAG, "updateNotificationVisibilityScreenLocked: interactiveNotificationDone=" + interactiveNotificationDone);
        if (!interactiveNotificationDone) {
            updateNotificationVisibility(true);
        }
    }

    public void updateNotificationVisibility(boolean visible) {
        Log.d(TAG, "updateNotificationVisibility: visible=" + visible + ", state=" + state + ", mainActivityVisible=" + mainActivityVisible);
        if (state != State.WAITING) {
            setMainActivityVisible(!visible);
            if (visible) {
                startNotificationForeground();
                notificationUpdateTimerCurrent(timerCurrent);
                interactiveNotification.setVisible();
                interactiveNotificationDone = false;
            } else {
                Log.d(TAG, "updateNotificationVisibility: stopForeground");
                stopForeground(true);
                interactiveNotification.dismiss();
                interactiveNotificationDone = true;
                if (!interactiveNotificationAlertDone)
                    notificationDeleted();
            }
        }
    }

    private void startNotificationForeground() {
        if (!mainActivityVisible) {
            Log.d(TAG, "startNotificationForeground: startForeground");
            startForeground(interactiveNotification.getId(), interactiveNotification.getNotification());
        }
    }

    private void updateStateIntent(State state) {
        this.state = state;
        if (mainActivityVisible) {
            getApplicationContext().sendBroadcast(new Intent(IntentAction.TIMER_STATE).putExtra("state", state.toString()));
        }
        saveContextPreferences(CONTEXT_PREFERENCE_STATE);
    }

    /**
     * Send information to the MainActivity
     */
    private void updateTimerIntent(long time) {
        if (mainActivityVisible) {
            getApplicationContext().sendBroadcast(new Intent(IntentAction.TIMER_UPDATE).putExtra("time", time));
        }
    }

    private void updateTimerIntent(long time, int sets) {
        if (mainActivityVisible) {
            getApplicationContext().sendBroadcast(new Intent(IntentAction.TIMER_UPDATE).putExtra("time", time).putExtra("sets", sets));
        }
    }

    protected void start() {
        Log.d(TAG, "start: timerUser=" + timerUser + ", setsCurrent=" + setsCurrent);
        startCountDown(timerUser);
        updateStateIntent(State.RUNNING);

        interactiveNotification.update(setsCurrent, timerUser, InteractiveNotification.ButtonsLayout.RUNNING);
    }

    private void startContextPreferences() {
        Log.d(TAG, "startContextPreferences: timerCurrent=" + timerCurrent + ", setsCurrent=" + setsCurrent);
        startCountDown(timerCurrent);
    }

    void pause() {
        if (state == State.RUNNING) {
            Log.d(TAG, "pause");

            pauseCountDown();
            updateStateIntent(State.PAUSED);

            interactiveNotification.updateButtonsLayout(InteractiveNotification.ButtonsLayout.PAUSED, InteractiveNotification.NotificationMode.UPDATE);

            saveContextPreferences(CONTEXT_PREFERENCE_TIMER_CURRENT);
        }
    }

    private void pauseContextPreference() {
        Log.d(TAG, "pauseContextPreference: timerCurrent=" + timerCurrent + ", setsCurrent=" + setsCurrent);

        startCountDown(timerCurrent);
        pauseCountDown();
    }

    protected void resume() {
        if (state == State.PAUSED) {
            Log.d(TAG, "resume");

            resumeCountDown();
            updateStateIntent(State.RUNNING);

            interactiveNotification.updateButtonsLayout(InteractiveNotification.ButtonsLayout.RUNNING, InteractiveNotification.NotificationMode.UPDATE);
        }
    }

    void stop() {
        Log.d(TAG, "stop: setCurrent=" + setsCurrent);

        timerCurrent = timerUser;

        stopCountDown();

        updateStateIntent(State.READY);
        updateTimerIntent(timerCurrent, setsCurrent);

        interactiveNotification.update(setsCurrent, timerCurrent, InteractiveNotification.ButtonsLayout.READY);

        saveContextPreferences(CONTEXT_PREFERENCE_TIMER_CURRENT);
    }

    private void done() {
        Log.d(TAG, "done: setsCurrent=" + setsCurrent + ", state=" + state);

        // The timer will be stopped from the alerts
        if (mainActivityVisible) {
            getApplicationContext().sendBroadcast(new Intent(IntentAction.TIMER_DONE));
        }

        setsCurrent++;
        doneInteractiveNotification(InteractiveNotification.NotificationMode.LIGHT_SOUND_LONG_VIBRATE);

        releaseWakeLock();

        saveContextPreferences(CONTEXT_PREFERENCE_SETS_CURRENT);
    }

    protected void nextSet() {
        Log.d(TAG, "nextSet: setsCurrent=" + setsCurrent);

        stopCountDown();

        setsCurrent++;
        timerCurrent = timerUser;

        updateStateIntent(State.READY);
        updateTimerIntent(timerUser, setsCurrent);

        doneInteractiveNotification(InteractiveNotification.NotificationMode.UPDATE);

        releaseWakeLock();

        saveContextPreferences(CONTEXT_PREFERENCE_TIMER_CURRENT | CONTEXT_PREFERENCE_SETS_CURRENT);
    }

    private void doneInteractiveNotification(InteractiveNotification.NotificationMode notificationMode) {
        notificationUpdateTimerCurrent(timerCurrent);
        interactiveNotification.updateSetsCurrent(setsCurrent);
        interactiveNotificationAlertDone = false;
        if (state == State.READY) {
            interactiveNotification.updateButtonsLayout(InteractiveNotification.ButtonsLayout.READY, notificationMode);
        } else if (setsCurrent <= setsUser) {
            interactiveNotification.updateButtonsLayout(InteractiveNotification.ButtonsLayout.SET_DONE, notificationMode);
        } else {
            interactiveNotification.updateButtonsLayout(InteractiveNotification.ButtonsLayout.ALL_SETS_DONE, notificationMode);
        }
        Log.d(TAG, "doneInteractiveNotification: setsCurrent=" + setsCurrent + ", setsUser=" + setsUser + ", state=" + state);
    }

    void nextSetStart() {
        Log.d(TAG, "nextSetStart: setsCurrent=" + setsCurrent);

        setsCurrent++;

        timerCurrent = timerUser;

        stopCountDown();

        startCountDown(timerUser);
        updateTimerIntent(timerUser, setsCurrent);
        updateStateIntent(State.RUNNING);

        interactiveNotification.update(setsCurrent, timerCurrent, InteractiveNotification.ButtonsLayout.RUNNING);

        releaseWakeLock();

        saveContextPreferences(CONTEXT_PREFERENCE_TIMER_CURRENT | CONTEXT_PREFERENCE_SETS_CURRENT);
    }

    protected void extraSet() {
        Log.d(TAG, "extraSet: setsCurrent=" + setsCurrent);

        timerCurrent = timerUser;

        stopCountDown();

        startCountDown(timerUser);
        updateTimerIntent(timerUser, setsCurrent);
        updateStateIntent(State.RUNNING);

        interactiveNotification.update(setsCurrent, timerCurrent, InteractiveNotification.ButtonsLayout.RUNNING);

        saveContextPreferences(CONTEXT_PREFERENCE_TIMER_CURRENT);
    }

    void reset() {
        Log.d(TAG, "reset");

        setsCurrent = setsInit;
        timerCurrent = timerUser;

        stopCountDown();

        interactiveNotification.update(setsCurrent, timerCurrent, InteractiveNotification.ButtonsLayout.READY);

        updateTimerIntent(timerCurrent, setsCurrent);
        updateStateIntent(State.READY);

        releaseWakeLock();

        saveContextPreferences(CONTEXT_PREFERENCE_TIMER_CURRENT | CONTEXT_PREFERENCE_SETS_CURRENT);
    }

    protected void clear() {
        Log.d(TAG, "clear");

        stopCountDown();

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
        releaseWakeLock();

        saveContextPreferences(CONTEXT_PREFERENCE_TIMER_CURRENT | CONTEXT_PREFERENCE_TIMER_USER | CONTEXT_PREFERENCE_SETS_INIT |
                CONTEXT_PREFERENCE_SETS_CURRENT | CONTEXT_PREFERENCE_SETS_USER);
    }

    void timerMinus() {
        timerCurrent = Math.max(timerCurrent - timerMinus, 0);
        Log.d(TAG, "timerMinus: timerCurrent=" + timerCurrent);
        updateCountDown(TimeUnit.SECONDS.toMillis(timerCurrent));
        notificationUpdateTimerCurrent(timerCurrent);
        interactiveNotification.updateButtonsLayout(InteractiveNotification.ButtonsLayout.RUNNING, InteractiveNotification.NotificationMode.UPDATE);
        saveContextPreferences(CONTEXT_PREFERENCE_TIMER_CURRENT);
    }

    protected void timerPlus() {
        timerCurrent += timerPlus;
        Log.d(TAG, "timerPlus: timerCurrent=" + timerCurrent);
        updateCountDown(TimeUnit.SECONDS.toMillis(timerCurrent));
        notificationUpdateTimerCurrent(timerCurrent);
        interactiveNotification.updateButtonsLayout(InteractiveNotification.ButtonsLayout.RUNNING, InteractiveNotification.NotificationMode.UPDATE);
        saveContextPreferences(CONTEXT_PREFERENCE_TIMER_CURRENT);
    }

    void setsMinus() {
        if (setsCurrent <= 1) {
            Log.e(TAG, "sets minus setsCurrent=" + setsCurrent);
            return;
        }
        setsCurrent -= 1;
        Log.d(TAG, "setsMinus: setsCurrent=" + setsCurrent);
        interactiveNotification.updateSetsCurrent(setsCurrent, InteractiveNotification.NotificationMode.UPDATE);
        saveContextPreferences(CONTEXT_PREFERENCE_SETS_CURRENT);
    }

    protected void setsPlus() {
        setsCurrent += 1;
        Log.d(TAG, "setsPlus: setsCurrent=" + setsCurrent);
        interactiveNotification.updateSetsCurrent(setsCurrent, InteractiveNotification.NotificationMode.UPDATE);
        saveContextPreferences(CONTEXT_PREFERENCE_SETS_CURRENT);
    }

    public void setSetsInit(int sets) {
        Log.d(TAG, "setSetsInit: setsInit=" + sets);
        setsInit = sets;
        interactiveNotification.updateSetsInit(setsInit);
        saveContextPreferences(CONTEXT_PREFERENCE_SETS_INIT);
    }

    public void setSetsCurrent(int sets) {
        Log.d(TAG, "setSetsCurrent: setsCurrent=" + sets);
        setsCurrent = sets;
        interactiveNotification.updateSetsCurrent(setsCurrent, InteractiveNotification.NotificationMode.UPDATE);
        saveContextPreferences(CONTEXT_PREFERENCE_SETS_CURRENT);
    }

    public void setSetsUser(int sets) {
        Log.d(TAG, "setSetsUser: setsUser=" + sets);
        setsUser = sets;
        interactiveNotification.updateSetsUser(setsUser, InteractiveNotification.NotificationMode.UPDATE);
        saveContextPreferences(CONTEXT_PREFERENCE_SETS_USER);
    }

    public void setTimerUser(long timer) {
        Log.d(TAG, "setTimerUser: timerUser=" + timer);
        timerUser = timer;
        interactiveNotification.updateTimerUser(timerUser);
        saveContextPreferences(CONTEXT_PREFERENCE_TIMER_USER);
    }

    public void setTimerCurrent(long timer) {
        Log.d(TAG, "setTimerCurrent: timerCurrent=" + timer);
        timerCurrent = timer;
        notificationUpdateTimerCurrent(timerCurrent);
        saveContextPreferences(CONTEXT_PREFERENCE_TIMER_CURRENT);
    }

    private void notificationUpdateTimerCurrent(long time) {
        // Avoid the extra notification when the timerUser == timerGetReady and when not RUNNING
        if (time == timerGetReady && timerUser > timerGetReady && timerGetReadyEnable && state == State.RUNNING) {
            interactiveNotification.updateTimerCurrent(time, InteractiveNotification.NotificationMode.SOUND_SHORT_VIBRATE);
        } else {
            interactiveNotification.updateTimerCurrent(time, InteractiveNotification.NotificationMode.UPDATE);
        }
    }

    private void timerUpdate(long time) {
        if (timerCurrent != time) {
            timerCurrent = time;
            if (mainActivityVisible) {
                updateTimerIntent(timerCurrent);
            } else {
                notificationUpdateTimerCurrent(timerCurrent);
            }
            if (time / TIMER_CURRENT_SAVING_INTERVAL != timerCurrentSaving) {
                timerCurrentSaving = time / TIMER_CURRENT_SAVING_INTERVAL;
                saveContextPreferences(CONTEXT_PREFERENCE_TIMER_CURRENT);
            }
            if (timerGetReadyEnable && timerCurrent == timerGetReady) {
                releaseWakeLock();
                setupAlarmManager();
            }
        }
    }

    private void notificationDeleted() {
        interactiveNotificationAlertDone = true;
        Log.d(TAG, "notificationDeleted: setsCurrent=" + setsCurrent + ", setsUser=" + setsUser + " state=" + state);
        if (state == State.PAUSED) {
            interactiveNotification.dismiss();
        } else if (setsCurrent <= setsUser) {
            interactiveNotification.dismiss();
            if (mainActivityVisible) {
                Log.d(TAG, "notificationDeleted: sending STOP action");
                getBaseContext().sendBroadcast(new Intent(IntentAction.STOP));
            }
            stop();
        } else {
            interactiveNotification.dismiss();
            if (mainActivityVisible) {
                Log.d(TAG, "notificationDeleted: sending CLEAR action");
                getBaseContext().sendBroadcast(new Intent(IntentAction.CLEAR));
            }
            reset();
        }
    }

    public void stopCountDown(){
        if (state == State.RUNNING) {
            cancelCountDown();
        }
    }

    private void setupAlarmManager() {
        long time = 0;

        if (timerGetReadyEnable && timerCurrent - WAKELOCK_TIME_APPROX > timerGetReady) {
            time = TimeUnit.SECONDS.toMillis(timerCurrent - WAKELOCK_TIME_APPROX - timerGetReady);
        } else if (timerGetReadyEnable && timerCurrent > timerGetReady) {
            time = TimeUnit.SECONDS.toMillis(timerCurrent - timerGetReady);
        } else if (timerCurrent - WAKELOCK_TIME_APPROX > 0) {
            time = TimeUnit.SECONDS.toMillis(timerCurrent - WAKELOCK_TIME_APPROX);
        } else if (timerCurrent > 0){
            time = TimeUnit.SECONDS.toMillis(timerCurrent);
        }

        if (time > 0){
            cancelAlarmManager();
            Log.d(TAG, "setupAlarmManager: wakeup the device in=" + (time / 1000) + ", at=" + (timerCurrent - (time / 1000)));
            time += System.currentTimeMillis();
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, time, pendingIntentAlarm);
        }
    }

    private void cancelAlarmManager() {
        alarmManager.cancel(pendingIntentAlarm);
        releaseWakeLock();
        Log.d(TAG, "cancelAlarmManager: canceling alarm");
    }

    protected void acquireWakeLock() {
        if (wakeLock != null) {
            if (!wakeLock.isHeld()) {
                Log.d(TAG, "acquireWakeLock: timerCurrent=" + timerCurrent);
                wakeLock.acquire();
            } else {
                Log.e(TAG, "acquireWakeLock: wakeLock isHeld=true");
            }
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null) {
          if (wakeLock.isHeld()) {
              Log.d(TAG, "releaseWakeLock: timerCurrent=" + timerCurrent);
              wakeLock.release();
          }
        }
    }

    private void startCountDown(long time) {
        Log.d(TAG, "startCountDown: time=" + time);
        setupAlarmManager();
        countDownPauseTimer = new CountDownPauseTimer(TimeUnit.SECONDS.toMillis(time), TimeUnit.SECONDS.toMillis(1)) {

            @Override
            public void onTick(long millisUntilFinished) {
                timerUpdate(TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished));
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
        if (countDownPauseTimer != null) {
            countDownPauseTimer.update(time);
        }
    }

    private void pauseCountDown() {
        cancelAlarmManager();
        if (countDownPauseTimer != null) {
            countDownPauseTimer.pause();
        }
    }

    private void resumeCountDown() {
        setupAlarmManager();
        if (countDownPauseTimer != null) {
            countDownPauseTimer.resume();
        }
    }

    private void cancelCountDown() {
        cancelAlarmManager();
        if (countDownPauseTimer != null) {
            countDownPauseTimer.cancel();
        }
    }

    private void saveContextPreferences(int flags) {
        SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
        if ((flags & CONTEXT_PREFERENCE_TIMER_END) == CONTEXT_PREFERENCE_TIMER_END) {
            sharedPreferencesEditor.putLong(getString(R.string.pref_timer_service_timer_end), System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timerCurrent));
            Log.d(TAG, "saveContextPreferences: timerEnd");
        }
        if ((flags & CONTEXT_PREFERENCE_TIMER_CURRENT) == CONTEXT_PREFERENCE_TIMER_CURRENT) {
            sharedPreferencesEditor.putLong(getString(R.string.pref_timer_service_timer_current), timerCurrent);
            Log.d(TAG, "saveContextPreferences: timerCurrent=" + timerCurrent);
        }
        if ((flags & CONTEXT_PREFERENCE_TIMER_USER) == CONTEXT_PREFERENCE_TIMER_USER) {
            sharedPreferencesEditor.putLong(getString(R.string.pref_timer_service_timer_user), timerUser);
            Log.d(TAG, "saveContextPreferences: timerUser=" + timerUser);
        }
        if ((flags & CONTEXT_PREFERENCE_SETS_INIT) == CONTEXT_PREFERENCE_SETS_INIT) {
            sharedPreferencesEditor.putInt(getString(R.string.pref_timer_service_sets_init), setsInit);
            Log.d(TAG, "saveContextPreferences: setsInit=" + setsInit);
        }
        if ((flags & CONTEXT_PREFERENCE_SETS_CURRENT) == CONTEXT_PREFERENCE_SETS_CURRENT) {
            sharedPreferencesEditor.putInt(getString(R.string.pref_timer_service_sets_current), setsCurrent);
            Log.d(TAG, "saveContextPreferences: setsCurrent=" + setsCurrent);
        }
        if ((flags & CONTEXT_PREFERENCE_SETS_USER) == CONTEXT_PREFERENCE_SETS_USER) {
            sharedPreferencesEditor.putInt(getString(R.string.pref_timer_service_sets_user), setsUser);
            Log.d(TAG, "saveContextPreferences: setsUser=" + setsUser);
        }
        if ((flags & CONTEXT_PREFERENCE_STATE) == CONTEXT_PREFERENCE_STATE) {
            sharedPreferencesEditor.putString(getString(R.string.pref_timer_service_state), state.toString());
            Log.d(TAG, "saveContextPreferences: state=" + state);
        }
        if ((flags & CONTEXT_PREFERENCE_MAIN_ACTIVITY_VISIBLE) == CONTEXT_PREFERENCE_MAIN_ACTIVITY_VISIBLE) {
            sharedPreferencesEditor.putBoolean(getString(R.string.pref_timer_service_main_activity_visible), mainActivityVisible);
            Log.d(TAG, "saveContextPreferences: mainActivityVisible=" + mainActivityVisible);
        }
        sharedPreferencesEditor.apply();
    }

    private void loadContextPreferences() {
        long timerEnd = sharedPreferences.getLong(getString(R.string.pref_timer_service_timer_end), System.currentTimeMillis());
        setTimerCurrent(sharedPreferences.getLong(getString(R.string.pref_timer_service_timer_current), timerCurrent));
        setTimerUser(sharedPreferences.getLong(getString(R.string.pref_timer_service_timer_user), timerUser));
        setSetsCurrent(sharedPreferences.getInt(getString(R.string.pref_timer_service_sets_current), setsCurrent));
        setSetsUser(sharedPreferences.getInt(getString(R.string.pref_timer_service_sets_user), setsUser));
        setSetsInit(sharedPreferences.getInt(getString(R.string.pref_timer_service_sets_init), setsInit));
        state = State.valueOf(sharedPreferences.getString(getString(R.string.pref_timer_service_state), state.toString()).toUpperCase(Locale.US));
        mainActivityVisible = sharedPreferences.getBoolean(getString(R.string.pref_timer_service_main_activity_visible), mainActivityVisible);

        if (state == State.RUNNING) {
            timerCurrent = TimeUnit.MILLISECONDS.toSeconds(timerEnd - System.currentTimeMillis());
            // TimerService have been killed after the notification, or notification has past
            if (timerCurrent <= 0) {
                reset();
                return;
            }
            startContextPreferences();
        } else if (state == State.PAUSED) {
            pauseContextPreference();
        }

        if (!mainActivityVisible) {
            updateNotificationVisibility(true);
            switch (state) {
                case RUNNING:
                    interactiveNotification.updateButtonsLayout(InteractiveNotification.ButtonsLayout.RUNNING);
                    break;
                case PAUSED:
                    interactiveNotification.updateButtonsLayout(InteractiveNotification.ButtonsLayout.PAUSED);
                    break;
                case READY:
                    interactiveNotification.updateButtonsLayout(InteractiveNotification.ButtonsLayout.READY);
                    break;
                default:
                    interactiveNotification.updateButtonsLayout(InteractiveNotification.ButtonsLayout.NO_LAYOUT);
                    break;
            }
            interactiveNotification.updateSetsCurrent(setsCurrent);
            interactiveNotification.updateTimerUser(timerUser);
            notificationUpdateTimerCurrent(timerCurrent);
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
        super.onDestroy();
        Log.v(TAG, "onDestroy");

        stopForeground(true);
        interactiveNotification.dismiss();
        releaseWakeLock();
        unregisterReceiver(timerServiceReceiver);
        saveContextPreferences(CONTEXT_PREFERENCE_TIMER_CURRENT);
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

    @Override
    public void onTaskRemoved(Intent rootIntent){
        super.onTaskRemoved(rootIntent);
        Log.v(TAG, "onTaskRemoved: state=" + state);
        if (state == State.WAITING) {
            clear();
        }
    }
}
