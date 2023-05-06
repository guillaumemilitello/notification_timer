package com.notification.timer;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import android.util.Log;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class TimerService extends Service {

    private static final String TAG = "TimerService";

    // margin for not missing the notification
    private static final long WAKELOCK_TIME_APPROX = 2;

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
    private InteractiveNotification interactiveNotification;
    private boolean interactiveNotificationAlertDone = false;
    private boolean interactiveNotificationDone = false;

    public void setInteractiveNotificationAlertDone() {
        interactiveNotificationAlertDone = true;
    }

    // Running values
    private long timerCurrent = 0;
    private long timerUser = 0;
    private int setsCurrent = 0;
    private int setsUser = 1;
    private String nameUser = "";
    private int displayMode = Preset.DISPLAY_MODE_TIMER;
    private State state = State.WAITING;

    public long getTimerCurrent() {
        return timerCurrent;
    }

    public long getTimerUser() {
        return timerUser;
    }

    public int getSetsCurrent() {
        return setsCurrent;
    }

    public int getSetsUser() {
        return setsUser;
    }

    public String getNameUser() {
        return nameUser;
    }

    public int getDisplayMode() {
        return displayMode;
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
    private boolean setsNumberReset;
    private boolean timerGetReadyEnable;
    private int timerGetReady;
    private long stepTime;
    private long timerMinus;
    private long timerPlus;

    // context preferences save
    private long timerCurrentSaving = 0;
    private static final int TIMER_CURRENT_SAVING_INTERVAL = 5;
    private static final int CONTEXT_PREFERENCE_TIMER_CURRENT = 0x01;
    private static final int CONTEXT_PREFERENCE_TIMER_USER = 0x02;
    private static final int CONTEXT_PREFERENCE_SETS_CURRENT = 0x04;
    private static final int CONTEXT_PREFERENCE_SETS_USER = 0x08;
    private static final int CONTEXT_PREFERENCE_NAME_USER = 0x10;
    private static final int CONTEXT_PREFERENCE_DISPLAY_MODE = 0x20;
    private static final int CONTEXT_PREFERENCE_STATE = 0x40;
    private static final int CONTEXT_PREFERENCE_ALL = 0x7F;

    private static final int CONTEXT_PREFERENCE_MAIN_ACTIVITY_VISIBLE = 0x80;

    public enum State {

        WAITING("waiting"),
        READY("ready"),
        RUNNING("running"),
        PAUSED("paused");

        private final String state;

        State(String state) {
            this.state = state;
        }

        @NonNull
        @Override
        public String toString() {
            return state;
        }
    }

    @SuppressLint("InvalidWakeLockTag")
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");
        PowerManager powerManager;

        interactiveNotification = new InteractiveNotification(this);

        alarmManager = (AlarmManager) getBaseContext().getSystemService(Context.ALARM_SERVICE);
        powerManager = (PowerManager) getBaseContext().getSystemService(Context.POWER_SERVICE);
        pendingIntentAlarm = PendingIntent.getBroadcast(getBaseContext(), 0, new Intent(IntentAction.ACQUIRE_WAKELOCK), PendingIntent.FLAG_IMMUTABLE);

        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SimpleWorkoutTimerWakeLock");
        }

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
        filter.addAction(IntentAction.STEP_TIME);
        filter.addAction(IntentAction.TIMER_MINUS);
        filter.addAction(IntentAction.TIMER_PLUS);
        filter.addAction(IntentAction.SETS_MINUS);
        filter.addAction(IntentAction.SETS_PLUS);
        filter.addAction(IntentAction.NOTIFICATION_DISMISS);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(IntentAction.ACQUIRE_WAKELOCK);
        registerReceiver(timerServiceReceiver, filter);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
        updateAllPreferences();

        loadContextPreferences();

        running = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: running=" + running);
        return START_STICKY;
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
            if (timerCurrent == 0 && setsCurrent == 0) {
                Log.e(TAG, "updateNotificationVisibility: wrong layout state=" + state + ", timerCurrent=" + timerCurrent + ", setsCurrent=" + setsCurrent);
                state = State.WAITING;
                return;
            }
            setMainActivityVisible(!visible);
            if (visible) {
                notificationUpdateTimerCurrent(timerCurrent);
                startNotificationForeground();
                interactiveNotificationDone = false;
            } else {
                Log.d(TAG, "updateNotificationVisibility: stopForeground");
                stopNotificationForeground();
                interactiveNotificationDone = true;
                if (!interactiveNotificationAlertDone) {
                    notificationDeleted();
                }
            }
        }
    }

    private void startNotificationForeground() {
        if (!mainActivityVisible) {
            Log.d(TAG, "startNotificationForeground");
            startForeground(interactiveNotification.getId(), interactiveNotification.getNotification());
            interactiveNotification.setVisible();
        }
    }

    private void stopNotificationForeground() {
        if (mainActivityVisible) {
            Log.d(TAG, "stopNotificationForeground");
            stopForeground(true);
            interactiveNotification.dismiss();
        }
    }

    private void updateStateIntent(State state) {
        this.state = state;
        if (mainActivityVisible) {
            getApplicationContext().sendBroadcast(new Intent(IntentAction.TIMER_STATE).putExtra("state", state.toString()));
        }
        saveContextPreferences(CONTEXT_PREFERENCE_STATE);
    }

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

    void start() {
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

    void resume() {
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
        doneInteractiveNotification(InteractiveNotification.NotificationMode.DONE);

        saveContextPreferences(CONTEXT_PREFERENCE_TIMER_CURRENT | CONTEXT_PREFERENCE_SETS_CURRENT);
    }

    void nextSet() {
        Log.d(TAG, "nextSet: setsCurrent=" + setsCurrent);

        stopCountDown();

        setsCurrent++;
        timerCurrent = timerUser;

        updateStateIntent(State.READY);
        updateTimerIntent(timerUser, setsCurrent);

        doneInteractiveNotification(InteractiveNotification.NotificationMode.UPDATE);

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

        updateStateIntent(State.RUNNING);
        startCountDown(timerUser);
        updateTimerIntent(timerUser, setsCurrent);

        interactiveNotification.update(setsCurrent, timerCurrent, InteractiveNotification.ButtonsLayout.RUNNING);

        saveContextPreferences(CONTEXT_PREFERENCE_TIMER_CURRENT | CONTEXT_PREFERENCE_SETS_CURRENT);
    }

    void extraSet() {
        Log.d(TAG, "extraSet: setsCurrent=" + setsCurrent);

        timerCurrent = timerUser;

        stopCountDown();
        startCountDown(timerUser);

        updateStateIntent(State.RUNNING);
        updateTimerIntent(timerUser, setsCurrent);

        interactiveNotification.update(setsCurrent, timerCurrent, InteractiveNotification.ButtonsLayout.RUNNING);

        saveContextPreferences(CONTEXT_PREFERENCE_TIMER_CURRENT);
    }

    void reset() {
        Log.d(TAG, "reset");

        if (setsNumberReset || setsUser != Integer.MAX_VALUE) {
            setsCurrent = 1;
        } else if (state == State.RUNNING || state == State.PAUSED) {
            setsCurrent += 1;
        }
        timerCurrent = timerUser;

        stopCountDown();

        interactiveNotification.update(setsCurrent, timerCurrent, InteractiveNotification.ButtonsLayout.READY);

        updateStateIntent(State.READY);
        updateTimerIntent(timerCurrent, setsCurrent);

        saveContextPreferences(CONTEXT_PREFERENCE_TIMER_CURRENT | CONTEXT_PREFERENCE_SETS_CURRENT);
    }

    void clear() {
        Log.d(TAG, "clear");

        stopCountDown();

        timerCurrent = 0;
        timerUser = 0;

        if (setsNumberReset || setsUser != Integer.MAX_VALUE) {
            setsCurrent = 0;
        } else if (state != State.WAITING) {
            setsCurrent -= 1;
        }
        setsUser = 0;

        nameUser = "";

        displayMode = Preset.DISPLAY_MODE_TIMER;

        updateStateIntent(State.WAITING);
        updateTimerIntent(timerCurrent, setsCurrent);

        // remove the notification and reset the timer to init state
        stopNotificationForeground();

        saveContextPreferences(CONTEXT_PREFERENCE_ALL);
    }

    void stepTime() {
        if (stepTime > 0) {
            timerPlus(stepTime);
        } else {
            timerMinus(-stepTime);
        }
    }

    void timerMinus() {
        timerMinus(timerMinus);
    }

    private void timerMinus(long timerMinus) {
        timerCurrent = Math.max(timerCurrent - timerMinus, 0);
        Log.d(TAG, "timerMinus: timerCurrent=" + timerCurrent);
        updateCountDown(TimeUnit.SECONDS.toMillis(timerCurrent));
        updateTimerIntent(timerCurrent);
        notificationUpdateTimerCurrent(timerCurrent);
        saveContextPreferences(CONTEXT_PREFERENCE_TIMER_CURRENT);
    }

    void timerPlus() {
        timerPlus(timerPlus);
    }

    private void timerPlus(long timerPlus) {
        timerCurrent += timerPlus;
        Log.d(TAG, "timerPlus: timerCurrent=" + timerCurrent);
        updateCountDown(TimeUnit.SECONDS.toMillis(timerCurrent));
        updateTimerIntent(timerCurrent);
        notificationUpdateTimerCurrent(timerCurrent);
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

    void setsPlus() {
        setsCurrent += 1;
        Log.d(TAG, "setsPlus: setsCurrent=" + setsCurrent);
        interactiveNotification.updateSetsCurrent(setsCurrent, InteractiveNotification.NotificationMode.UPDATE);
        saveContextPreferences(CONTEXT_PREFERENCE_SETS_CURRENT);
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
        interactiveNotification.updateSetsUser(setsUser);
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

    public void setNameUser(String name) {
        Log.d(TAG, "setNameUser: nameUser=" + name);
        nameUser = name;
        interactiveNotification.updateNameUser(name);
        saveContextPreferences(CONTEXT_PREFERENCE_NAME_USER);
    }

    public void setDisplayMode(int mode) {
        Log.d(TAG, "displayMode: displayMode=" + mode);
        displayMode = mode;
        saveContextPreferences(CONTEXT_PREFERENCE_DISPLAY_MODE);
    }

    private void notificationUpdateTimerCurrent(long time) {
        // Avoid the extra notification when the timerUser == timerGetReady and when not RUNNING
        if (time == timerGetReady && timerUser > timerGetReady && timerGetReadyEnable && state == State.RUNNING) {
            interactiveNotification.updateTimerCurrent(time, InteractiveNotification.NotificationMode.READY);
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
                setupAlarmManager();
            }
        }
    }

    private void notificationDeleted() {
        interactiveNotificationAlertDone = true;
        Log.d(TAG, "notificationDeleted: setsCurrent=" + setsCurrent + ", setsUser=" + setsUser + " state=" + state);
        interactiveNotification.dismiss();
    }

    public void stopCountDown(){
        if (state == State.RUNNING) {
            cancelCountDown();
        }
    }

    private void setupAlarmManager() {
        long timerNow = timerCurrent;
        if (timerNow == 0) {
            timerNow = timerUser;
        }

        long timeout = timerNow - WAKELOCK_TIME_APPROX;

        // wake up for the extra notification
        if (timerGetReadyEnable && timeout > timerGetReady) {
            timeout -= timerGetReady;
        }

        // still set an alarm within WAKELOCK_TIME_APPROX
        if (timeout <= 0) {
            timeout = timerNow;
        }

        cancelAlarmManager();
        Log.d(TAG, "setupAlarmManager: setting alarm to wakeup the device in=" + timeout + ", for timerCurrent=" + (timerNow - timeout));
        final long triggerAtMillis = TimeUnit.SECONDS.toMillis(timeout) + System.currentTimeMillis();
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntentAlarm);
    }

    private void cancelAlarmManager() {
        Log.d(TAG, "cancelAlarmManager: canceling alarm");
        alarmManager.cancel(pendingIntentAlarm);
    }

    void acquireWakeLock() {
        if (wakeLock != null) {
            Log.d(TAG, "acquireWakeLock: timerCurrent=" + timerCurrent);
            if (wakeLock.isHeld()) {
                Log.d(TAG, "acquireWakeLock: isHeld=true");
                wakeLock.release();
            }
            // making sure we don't miss the notification, wake lock will auto release
            wakeLock.acquire(TimeUnit.SECONDS.toMillis(2 * WAKELOCK_TIME_APPROX));
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
                timerUpdate(0);
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
        if ((flags & CONTEXT_PREFERENCE_TIMER_CURRENT) == CONTEXT_PREFERENCE_TIMER_CURRENT) {
            sharedPreferencesEditor.putLong(getString(R.string.pref_timer_service_timer_current), timerCurrent);
            sharedPreferencesEditor.putLong(getString(R.string.pref_timer_service_timer_end), System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timerCurrent));            Log.d(TAG, "saveContextPreferences: timerCurrent=" + timerCurrent);
        }
        if ((flags & CONTEXT_PREFERENCE_TIMER_USER) == CONTEXT_PREFERENCE_TIMER_USER) {
            sharedPreferencesEditor.putLong(getString(R.string.pref_timer_service_timer_user), timerUser);
            Log.d(TAG, "saveContextPreferences: timerUser=" + timerUser);
        }
        if ((flags & CONTEXT_PREFERENCE_SETS_CURRENT) == CONTEXT_PREFERENCE_SETS_CURRENT) {
            sharedPreferencesEditor.putInt(getString(R.string.pref_timer_service_sets_current), setsCurrent);
            Log.d(TAG, "saveContextPreferences: setsCurrent=" + setsCurrent);
        }
        if ((flags & CONTEXT_PREFERENCE_SETS_USER) == CONTEXT_PREFERENCE_SETS_USER) {
            sharedPreferencesEditor.putInt(getString(R.string.pref_timer_service_sets_user), setsUser);
            Log.d(TAG, "saveContextPreferences: setsUser=" + setsUser);
        }
        if ((flags & CONTEXT_PREFERENCE_NAME_USER) == CONTEXT_PREFERENCE_NAME_USER) {
            sharedPreferencesEditor.putString(getString(R.string.pref_timer_service_name_user), nameUser);
            Log.d(TAG, "saveContextPreferences: nameUser=" + nameUser);
        }
        if ((flags & CONTEXT_PREFERENCE_DISPLAY_MODE) == CONTEXT_PREFERENCE_DISPLAY_MODE) {
            sharedPreferencesEditor.putInt(getString(R.string.pref_timer_service_displayMode), displayMode);
            Log.d(TAG, "saveContextPreferences: displayMode=" + displayMode);
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
        setNameUser(sharedPreferences.getString(getString(R.string.pref_timer_service_name_user), nameUser));
        setDisplayMode(sharedPreferences.getInt(getString(R.string.pref_timer_service_displayMode), displayMode));
        state = State.valueOf(sharedPreferences.getString(getString(R.string.pref_timer_service_state), state.toString()).toUpperCase(Locale.US));
        mainActivityVisible = sharedPreferences.getBoolean(getString(R.string.pref_timer_service_main_activity_visible), mainActivityVisible);
        Log.d(TAG, "loadContextPreferences: timerCurrent=" + timerCurrent + ", timerUser=" + timerUser + ", setsCurrent=" + setsCurrent
        + ", setsUser=" + setsUser + ", nameUser=" + nameUser + ", displayMode=" + displayMode + ", state=" + state + ", mainActivityVisible=" + mainActivityVisible);

        if (state == State.RUNNING) {
            long remainingTime = TimeUnit.MILLISECONDS.toSeconds(timerEnd - System.currentTimeMillis());
            // TimerService have been killed after the notification, or notification has past
            Log.d(TAG, "loadContextPreferences: remainingTime=" + remainingTime);
            if (remainingTime > 0) {
                timerCurrent = remainingTime;
                startContextPreferences();
            } else if (timerCurrent > 0) {
                interactiveNotification.updateSetsCurrent(++setsCurrent);
                timerCurrent = 0;
            }
            notificationUpdateTimerCurrent(timerCurrent);
        } else if (state == State.PAUSED) {
            pauseContextPreference();
        }

        if (!mainActivityVisible) {
            updateNotificationLayout();
            interactiveNotification.updateNotificationBuilder();
            updateNotificationVisibility(!mainActivityVisible);
        }
    }

    private void updateNotificationLayout() {
        switch (state) {
            case RUNNING:
                if (timerCurrent > 0) {
                    interactiveNotification.updateButtonsLayout(InteractiveNotification.ButtonsLayout.RUNNING);
                } else  if (setsCurrent <= setsUser) {
                    interactiveNotification.updateButtonsLayout(InteractiveNotification.ButtonsLayout.SET_DONE);
                } else {
                    interactiveNotification.updateButtonsLayout(InteractiveNotification.ButtonsLayout.ALL_SETS_DONE);
                }
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
    }

    private void updateAllPreferences() {
        Log.d(TAG, "updateAllPreferences");
        updatePreference(getString(R.string.pref_timer_minus));
        updatePreference(getString(R.string.pref_timer_plus));
        updatePreference(getString(R.string.pref_sets_number_display_enable));
        updatePreference(getString(R.string.pref_sets_number_reset));
        updatePreference(getString(R.string.pref_step_time));
        updatePreference(getString(R.string.pref_vibrate));
        updatePreference(getString(R.string.pref_ringtone_uri));
        updatePreference(getString(R.string.pref_light_color_enable));
        updatePreference(getString(R.string.pref_light_color));
        updatePreference(getString(R.string.pref_light_flash_rate));
        updatePreference(getString(R.string.pref_timer_get_ready_enable));
        updatePreference(getString(R.string.pref_timer_get_ready));
        updatePreference(getString(R.string.pref_timer_get_ready_vibrate));
        updatePreference(getString(R.string.pref_timer_get_ready_ringtone_uri));
        updatePreference(getString(R.string.pref_background_theme_mode));
        updatePreference(getString(R.string.pref_dark_theme_mode));
        updatePreference(getString(R.string.pref_custom_color_enable));
        updatePreference(getString(R.string.pref_custom_color_running));
        updatePreference(getString(R.string.pref_custom_color_ready));
        updatePreference(getString(R.string.pref_custom_color_done));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (interactiveNotification != null){
                interactiveNotification.updateDoneChannel();
                interactiveNotification.updateReadyChannel();
            } else {
                Log.e(TAG, "updateAllPreferences: interactiveNotification is null");
            }
        }
    }

    private void updatePreference(String key) {
        Log.d(TAG, "updatePreference: key=" + key);

        if (key.equals(getString(R.string.pref_timer_minus))) {
            timerMinus = Long.parseLong(sharedPreferences.getString(key, getString(R.string.default_timer_minus)));
        } else if (key.equals(getString(R.string.pref_timer_plus))) {
            timerPlus = Long.parseLong(sharedPreferences.getString(key, getString(R.string.default_timer_plus)));
        } else if (key.equals(getString(R.string.pref_sets_number_display_enable))) {
            final boolean setsNumberDisplayEnable = sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.default_sets_number_display_enable));
            interactiveNotification.setSetsNumberDisplayEnable(setsNumberDisplayEnable);
        } else if (key.equals(getString(R.string.pref_sets_number_reset))) {
            setsNumberReset = sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.default_sets_number_reset));
            interactiveNotification.setSetsNumberReset(setsNumberReset);
        } else if (key.equals(getString(R.string.pref_step_time))) { // timerMinus must be set before in updateAllPreferences()
            String timerMinusString = Long.toString(-timerMinus);
            stepTime = Long.parseLong(sharedPreferences.getString(key, timerMinusString));
            interactiveNotification.setStepTime(stepTime);
            if (!sharedPreferences.contains(getString(R.string.pref_step_time))) { // keep the previous timerMinus preference when updating
                SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
                sharedPreferencesEditor.putString(getString(R.string.pref_step_time), timerMinusString);
                sharedPreferencesEditor.apply();
            }
        } else if (key.equals(getString(R.string.pref_vibrate))) {
            boolean vibrationEnable = sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.default_vibrate));
            interactiveNotification.setVibrationEnable(vibrationEnable);
        } else if (key.equals(getString(R.string.pref_ringtone_uri))) {
            Uri ringtoneUri = Uri.parse(sharedPreferences.getString(key, getString(R.string.default_ringtone_uri)));
            interactiveNotification.setRingtone(ringtoneUri);
        }  else if (key.equals(getString(R.string.pref_light_color_enable))) {
            boolean colorEnable = sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.default_light_color_enable));
            interactiveNotification.setLightColorEnable(colorEnable);
        } else if (key.equals(getString(R.string.pref_light_color))) {
            int light_color = sharedPreferences.getInt(key, ContextCompat.getColor(this, R.color.default_light_color));
            interactiveNotification.setLightColor(light_color);
        } else if (key.equals(getString(R.string.pref_light_flash_rate))) {
            int flashRate = Integer.parseInt(sharedPreferences.getString(key, getString(R.string.default_light_flash_rate)));
            interactiveNotification.setLightFlashRate(flashRate);
        } else if (key.equals(getString(R.string.pref_timer_get_ready_enable))) {
            timerGetReadyEnable = sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.default_timer_get_ready_enable));
            interactiveNotification.setTimerGetReadyEnable(timerGetReadyEnable);
            setupAlarmManager();
        } else if (key.equals(getString(R.string.pref_timer_get_ready))) {
            timerGetReady = Integer.parseInt(sharedPreferences.getString(key, getString(R.string.default_timer_get_ready)));
            interactiveNotification.setTimerGetReady(timerGetReady);
        } else if (key.equals(getString(R.string.pref_timer_get_ready_vibrate))) {
            boolean vibrationEnableReady = sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.default_timer_get_ready_vibrate));
            interactiveNotification.setVibrationReadyEnable(vibrationEnableReady);
        } else if (key.equals(getString(R.string.pref_timer_get_ready_ringtone_uri))) {
            Uri ringtoneUriReady = Uri.parse(sharedPreferences.getString(key, getString(R.string.default_timer_get_ready_ringtone_uri)));
            interactiveNotification.setRingtoneReady(ringtoneUriReady);
        } else if (key.equals(getString(R.string.pref_custom_color_enable))) {
            boolean colorEnable = sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.default_color_enable));
            interactiveNotification.setColorEnable(colorEnable);
        } else if (key.equals(getString(R.string.pref_dark_theme_mode)) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            int darkThemeMode = Integer.parseInt(sharedPreferences.getString(key, getString(R.string.default_dark_mode)));
            AppCompatDelegate.setDefaultNightMode(darkThemeMode);
        } else if (key.equals(getString(R.string.pref_custom_color_running))) {
            int colorRunning = sharedPreferences.getInt(key, ContextCompat.getColor(this, R.color.default_color_running));
            interactiveNotification.setColorRunning(colorRunning);
        } else if (key.equals(getString(R.string.pref_custom_color_ready))) {
            int colorReady = sharedPreferences.getInt(key, ContextCompat.getColor(this, R.color.default_color_ready));
            interactiveNotification.setColorReady(colorReady);
        } else if (key.equals(getString(R.string.pref_custom_color_done))) {
            int colorDone = sharedPreferences.getInt(key, ContextCompat.getColor(this, R.color.default_color_done));
            interactiveNotification.setColorDone(colorDone);
        } else {
            Log.e(TAG, "updatePreference: not supported preference key=" + key);
        }
    }

    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                    Log.d(TAG, "onSharedPreferenceChanged: key=" + key);
                    if (PreferencesActivity.isKeyPreference(getBaseContext(), key))
                    {
                        updatePreference(key);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            final List<String> doneChannelPrefList = Arrays.asList(getString(R.string.pref_ringtone_uri), getString(R.string.pref_vibrate),
                                    getString(R.string.pref_light_color_enable), getString(R.string.pref_light_color));
                            if (doneChannelPrefList.contains(key)) {
                                interactiveNotification.updateDoneChannel();
                            }
                            final List<String> readyChannelPrefList = Arrays.asList(getString(R.string.pref_timer_get_ready_ringtone_uri),
                                    getString(R.string.pref_timer_get_ready_vibrate));
                            if (readyChannelPrefList.contains(key)) {
                                interactiveNotification.updateReadyChannel();
                            }
                        }
                    }
                }
            };

    class TimerBinder extends Binder {
        TimerService getService() {
            Log.d(TAG, "TimerServiceBinder getService");
            return TimerService.this;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "onDestroy");

        stopNotificationForeground();
        unregisterReceiver(timerServiceReceiver);
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
        setsCurrent = 0; // Always clear the timer number when quitting the app.
        saveContextPreferences(CONTEXT_PREFERENCE_TIMER_CURRENT | CONTEXT_PREFERENCE_SETS_CURRENT);
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
