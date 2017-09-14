package com.simpleworkout.timer;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import java.util.Locale;

@SuppressLint("ParcelCreator")
class InteractiveNotification extends Notification {

    private final static String TAG = "InteractiveNotification";

    private final static int ID = 52;

    public int getId() {
        return ID;
    }

    private boolean restTimerNotificationVisible;

    private NotificationManager notificationManager;
    private Notification.Builder notificationBuilder;

    public Notification getNotification() {
        return notificationBuilder.build();
    }

    // Timer service related
    private long timerCurrent, timerUser;
    private int setsCurrent, setsUser, setsInit;
    private String timerString, setsString;

    private boolean timerGetReadyEnable;
    private int timerGetReady;

    private ButtonsLayout buttonsLayout;
    private ButtonAction button0, button1, button2;
    private ButtonAction currentButton0, currentButton1, currentButton2;
    private RemoteViews remoteView;

    void setVibrationEnable(boolean vibrationEnable) {
        this.vibrationEnable = vibrationEnable;
    }

    void setVibrationReadyEnable(boolean vibrationReadyEnable) {
        this.vibrationReadyEnable = vibrationReadyEnable;
    }

    public void setTimerGetReadyEnable(boolean timerGetReadyEnable) {
        this.timerGetReadyEnable = timerGetReadyEnable;
    }

    public void setTimerGetReady(int timerGetReady) {
        this.timerGetReady = timerGetReady;
    }

    void setLightColor(int lightColor) {
        this.lightColor = lightColor;
    }

    void setLightFlashRate(int lightFlashRate) {
        // No light blinking
        this.lightFlashRateOn = (lightFlashRate == 0)? 1000 : lightFlashRate;
        this.lightFlashRateOff = lightFlashRate;
    }

    void setRingtone(Uri ringtone) {
        this.ringtone = ringtone;
    }

    void setRingtoneReady(Uri ringtoneReady) {
        this.ringtoneReady = ringtoneReady;
    }

    // Settings options
    private boolean vibrationEnable;
    private boolean vibrationReadyEnable;
    private int lightColor;
    private int lightFlashRateOn, lightFlashRateOff;
    private Uri ringtone;
    private Uri ringtoneReady;

    final static int COLOR_NONE = -1;

    private Context context;

    private enum ButtonAction {

        NO_ACTION("no_action"),
        START("start"),
        STOP("stop"),
        PAUSE("pause"),
        RESUME("resume"),
        RESET("reset"),
        NEXT_SET("next_set"),
        NEXT_SET_START("next_set_start"),
        EXTRA_SET("extra_set"),
        TIMER_MINUS("timer_minus"),
        TIMER_PLUS("timer_plus"),
        DISMISS("dismiss");

        private String action;

        ButtonAction(String action) {
            this.action = action;
        }

        @Override
        public String toString() {
            return action;
        }
    }

    enum ButtonsLayout {

        NO_LAYOUT("no_layout"),
        READY("ready"),
        RUNNING("running"),
        PAUSED("paused"),
        SET_DONE("set_done"),
        ALL_SETS_DONE("all_sets_done");

        private String layout;

        ButtonsLayout(String layout) {
            this.layout = layout;
        }

        @Override
        public String toString() {
            return layout;
        }
    }

    enum NotificationMode {

        NO_NOTIFICATION,
        UPDATE,
        SOUND_SHORT_VIBRATE,
        LIGHT_SOUND_LONG_VIBRATE
    }


    InteractiveNotification(Context context) {

        this.context = context;

        // pending intent to go back to the main activity from the notification
        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

        // pending intent when the notification is deleted
        PendingIntent pendingIntentDeleted = PendingIntent.getBroadcast(context, 0, new Intent().setAction(IntentAction.NOTIFICATION_DISMISS), PendingIntent.FLAG_UPDATE_CURRENT);

        // prepare Notification Builder
        notificationBuilder = new Notification.Builder(context)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setDeleteIntent(pendingIntentDeleted)
                .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                .setPriority(PRIORITY_MAX);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notificationBuilder.setStyle(new DecoratedMediaCustomViewStyle());
            notificationBuilder.setColor(context.getColor(R.color.colorPrimary));
        }

        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        timerString = "";
        setsString = "";
        timerCurrent = 0;
        setsCurrent = 0;
        setsUser = 0;
        setsInit = 0;

        timerGetReadyEnable = false;
        timerGetReady = 0;

        button2 = ButtonAction.NO_ACTION;
        button1 = ButtonAction.NO_ACTION;
        button0 = ButtonAction.NO_ACTION;
        buttonsLayout = ButtonsLayout.NO_LAYOUT;

        updateButtonsLayout(ButtonsLayout.READY);
    }

    protected void update(int setsCurrent, long timerCurrent, ButtonsLayout layout, NotificationMode notificationMode) {
        updateTimerCurrent(timerCurrent);
        updateSetsCurrent(setsCurrent);
        updateButtonsLayout(layout, notificationMode);
    }

    void updateButtonsLayout(ButtonsLayout layout) {
        updateButtonsLayout(layout, NotificationMode.NO_NOTIFICATION);
    }

    void updateButtonsLayout(ButtonsLayout layout, NotificationMode notificationMode) {
        Log.d(TAG, "updateButtonsLayout: layout=" + layout.toString() + ", buttonsLayout=" + buttonsLayout.toString() + ", timerCurrent=" + timerCurrent + ", setsCurrent=" + setsCurrent);
        if (buttonsLayout == ButtonsLayout.READY || buttonsLayout != layout) {
            switch (layout) {
                case READY:
                    button2 = (setsCurrent > setsInit)? ButtonAction.DISMISS : ButtonAction.NO_ACTION;
                    button1 = (setsCurrent > setsInit)? ButtonAction.RESET   : ButtonAction.DISMISS;
                    button0 = ButtonAction.START;
                    break;
                case RUNNING:
                    button2 = ButtonAction.TIMER_MINUS;
                    button1 = ButtonAction.NEXT_SET;
                    button0 = ButtonAction.PAUSE;
                    break;
                case PAUSED:
                    button2 = ButtonAction.DISMISS;
                    button1 = ButtonAction.NEXT_SET;
                    button0 = ButtonAction.RESUME;
                    break;
                case SET_DONE:
                    button2 = ButtonAction.DISMISS;
                    button1 = ButtonAction.RESET;
                    button0 = ButtonAction.START;
                    break;
                case ALL_SETS_DONE:
                    button2 = ButtonAction.DISMISS;
                    button1 = ButtonAction.RESET;
                    button0 = ButtonAction.EXTRA_SET;
                    break;
                default:
                    Log.e(TAG, "updateButtonsLayout: layout=" + layout.toString());
            }
            buttonsLayout = layout;
            Log.d(TAG, "updateButtonsLayout: buttonsLayout=" + buttonsLayout.toString());

            updateTimerTextView();
            updateSetsTextView();

            build(notificationMode);
        }
    }

    private void updateButton(int id, ButtonAction action) {
        if (id == R.id.unused_button) {
            return;
        }

        int iconId;
        Intent intent;

        if (action != ButtonAction.NO_ACTION)
        {
            remoteView.setBoolean(id, "setEnabled", true);
            remoteView.setViewVisibility(id, View.VISIBLE);
        }
        else
        {
            remoteView.setBoolean(id, "setEnabled", false);
            remoteView.setViewVisibility(id, View.INVISIBLE);
        }

        switch (action) {
            case START:
                iconId = R.drawable.ic_play_arrow_black_48dp;
                intent = new Intent().setAction(IntentAction.START);
                break;
            case PAUSE:
                iconId = R.drawable.ic_pause_black_48dp;
                intent = new Intent().setAction(IntentAction.PAUSE);
                break;
            case RESUME:
                iconId = R.drawable.ic_play_arrow_black_48dp;
                intent = new Intent().setAction(IntentAction.RESUME);
                break;
            case STOP:
                iconId = R.drawable.ic_stop_black_48dp;
                intent = new Intent().setAction(IntentAction.STOP);
                break;
            case NEXT_SET:
                iconId = R.drawable.ic_chevron_right_black_48dp;
                intent = new Intent().setAction(IntentAction.NEXT_SET);
                break;
            case NEXT_SET_START:
                iconId = R.drawable.ic_chevron_right_double_black_48dp;
                intent = new Intent().setAction(IntentAction.NEXT_SET_START);
                break;
            case EXTRA_SET:
                iconId = R.drawable.ic_chevron_right_double_black_48dp;
                intent = new Intent().setAction(IntentAction.EXTRA_SET);
                break;
            case TIMER_MINUS:
                iconId = R.drawable.ic_stopwatch_remove_black_48dp;
                intent = new Intent().setAction(IntentAction.TIMER_MINUS);
                break;
            case TIMER_PLUS:
                iconId = R.drawable.ic_stopwatch_add_black_48dp;
                intent = new Intent().setAction(IntentAction.TIMER_PLUS);
                break;
            case RESET:
                iconId = R.drawable.ic_refresh_black_48dp;
                intent = new Intent().setAction(IntentAction.RESET);
                break;
            case DISMISS:
                iconId = R.drawable.ic_close_black_48dp;
                intent = new Intent().setAction(IntentAction.NOTIFICATION_DISMISS);
                break;
            case NO_ACTION:
                return;
            default:
                Log.e(TAG, "updateButton: undefined action=" + action);
                return;
        }
        remoteView.setImageViewResource(id, iconId);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteView.setOnClickPendingIntent(id, pendingIntent);
    }

    void setVisible() {
        Log.d(TAG, "setVisible");
        restTimerNotificationVisible = true;
        build(NotificationMode.UPDATE);
    }

    private void updateRemoteView() {
        if (remoteView == null) {
            remoteView = new RemoteViews(context.getPackageName(), R.layout.notification);
        }

        if (button0 != currentButton0) {
            currentButton0 = button0;
            updateButton(R.id.button0, button0);
        }
        if (button1 != currentButton1) {
            currentButton1 = button1;
            updateButton(R.id.button1, button1);
        }
        if (button2 != currentButton2) {
            currentButton2 = button2;
            updateButton(R.id.button2, button2);
        }

        if (drawProgressBar()) {
            remoteView.setTextViewText(R.id.textViewSets, "");
            remoteView.setTextViewText(R.id.textViewSets_short, setsString);

            int timerVisibleId, timerGoneId, progressBarVisibleId, progressBarGoneId;

            if (timerGetReadyEnable && timerCurrent <= timerGetReady) {
                timerVisibleId = R.id.textViewTimerGetReady;
                timerGoneId = R.id.textViewTimer;
                progressBarVisibleId = R.id.progressBarTimerGetReady;
                progressBarGoneId = R.id.progressBarTimer;
            } else {
                timerVisibleId = R.id.textViewTimer;
                timerGoneId = R.id.textViewTimerGetReady;
                progressBarVisibleId = R.id.progressBarTimer;
                progressBarGoneId = R.id.progressBarTimerGetReady;
            }
            remoteView.setViewVisibility(timerVisibleId, View.VISIBLE);
            remoteView.setTextViewText(timerVisibleId, timerString);
            remoteView.setViewVisibility(progressBarVisibleId, View.VISIBLE);
            remoteView.setProgressBar(progressBarVisibleId, (int) timerUser, (int) (timerUser - timerCurrent), false);
            remoteView.setViewVisibility(timerGoneId, View.GONE);
            remoteView.setViewVisibility(progressBarGoneId, View.GONE);
        } else {
            remoteView.setTextViewText(R.id.textViewSets, setsString);
            remoteView.setTextViewText(R.id.textViewSets_short, "");

            remoteView.setViewVisibility(R.id.textViewTimer, View.VISIBLE);
            remoteView.setTextViewText(R.id.textViewTimer, timerString);
            remoteView.setViewVisibility(R.id.textViewTimerGetReady, View.GONE);

            remoteView.setViewVisibility(R.id.progressBarTimer, View.INVISIBLE);
            remoteView.setViewVisibility(R.id.progressBarTimerGetReady, View.INVISIBLE);
        }
    }

    private boolean drawProgressBar() {
        switch (buttonsLayout) {
            default: case NO_LAYOUT: case PAUSED: case RUNNING: return true;
            case READY: case SET_DONE: case ALL_SETS_DONE: return false;
        }
    }

    private void build(NotificationMode notificationMode) {
        if (restTimerNotificationVisible && notificationMode != NotificationMode.NO_NOTIFICATION) {

            updateRemoteView();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                if (notificationMode == NotificationMode.UPDATE) {
                    notificationBuilder.setCustomContentView(remoteView);
                }
                else {
                    notificationBuilder.setCustomHeadsUpContentView(remoteView);
                }
            } else {
                //noinspection deprecation
                notificationBuilder.setContent(remoteView);
            }

            switch (notificationMode) {
                case UPDATE:
                    notificationBuilder.setLights(COLOR_DEFAULT, 0, 0);
                    break;
                case SOUND_SHORT_VIBRATE:
                    notificationBuilder.setVibrate(vibrationReadyEnable ? MainActivity.vibrationPattern : null);
                    notificationBuilder.setSound(ringtoneReady);
                    break;
                case LIGHT_SOUND_LONG_VIBRATE:
                    notificationBuilder.setVibrate(vibrationEnable ? MainActivity.vibrationPattern : null);
                    notificationBuilder.setSound(ringtone);
                    if (lightColor != COLOR_NONE) {
                        notificationBuilder.setLights(lightColor, lightFlashRateOn, lightFlashRateOff);
                    }
                    break;
                case NO_NOTIFICATION:
                    break;
                default:
                    Log.e(TAG, "build: notificationMode=" + notificationMode + " not specified");
                    break;
            }
            notificationManager.notify(ID, notificationBuilder.build());

            notificationBuilder.setSound(null);
            notificationBuilder.setVibrate(null);

            if (notificationMode != NotificationMode.UPDATE) {
                Log.d(TAG, "build: notificationMode=" + notificationMode);
            }
        }
    }

    private void updateTimerCurrent(long timer) {
        updateTimerCurrent(timer, NotificationMode.NO_NOTIFICATION);
    }

    void updateTimerCurrent(long timer, NotificationMode notificationMode) {
        timerCurrent = timer;
        updateTimerTextView();
        build(notificationMode);
    }

    void updateTimerUser(long timer) {
        timerUser = timer;
    }

    void updateSetsCurrent(int sets) {
        updateSetsCurrent(sets, NotificationMode.NO_NOTIFICATION);
    }

    void updateSetsCurrent(int sets, NotificationMode notificationMode) {
        Log.d(TAG, "updateSetsCurrent: setsCurrent=" + sets);
        setsCurrent = sets;
        updateSetsTextView();
        build(notificationMode);
    }

    void updateSetsUser(int sets, NotificationMode notificationMode) {
        Log.d(TAG, "updateSetsUser: setsUser=" + sets);
        setsUser = sets;
        updateSetsTextView();
        build(notificationMode);
    }

    void updateSetsInit(int sets) {
        setsInit = sets;
    }

    private void updateTimerTextView() {
        switch (buttonsLayout) {
            case NO_LAYOUT:
            case READY:
            case PAUSED:
            case RUNNING:
                timerString = String.format(Locale.US, "%d:%02d", timerCurrent / 60, timerCurrent % 60);
                break;
            case SET_DONE:
            case ALL_SETS_DONE:
                timerString = context.getString(R.string.notif_timer_done);
                break;
        }
    }

    private void updateSetsTextView() {
        switch (buttonsLayout) {
            case NO_LAYOUT:
            case PAUSED:
            case RUNNING:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    if (setsUser == Integer.MAX_VALUE) {
                        setsString = String.format(context.getString(R.string.notif_timer_number), setsCurrent);
                    } else if (setsInit == 1) {
                        setsString = String.format(context.getString(R.string.notif_timer_info), setsCurrent, setsUser);
                    } else {
                        setsString = String.format(context.getString(R.string.notif_timer_info_start0), setsCurrent, setsUser);
                    }
                } else {
                    setsString = String.format(context.getString(R.string.notif_timer_number), setsCurrent);
                }
                break;
            case READY:
                if (setsUser == Integer.MAX_VALUE) {
                    setsString = String.format(context.getString(R.string.notif_timer_number), setsCurrent);
                } else if (setsInit == 1) {
                    setsString = String.format(context.getString(R.string.notif_timer_info), setsCurrent, setsUser);
                } else {
                    setsString = String.format(context.getString(R.string.notif_timer_info_start0), setsCurrent, setsUser);
                }
                break;
            case ALL_SETS_DONE:
            case SET_DONE:
                if (setsUser == Integer.MAX_VALUE) {
                    setsString = String.format(context.getString(R.string.notif_timer_number), setsCurrent - 1);
                } else if (setsInit == 1) {
                    setsString = String.format(context.getString(R.string.notif_timer_info), setsCurrent - 1, setsUser);
                } else {
                    setsString = String.format(context.getString(R.string.notif_timer_info_start0), setsCurrent - 1, setsUser);
                }
                break;
        }
        Log.d(TAG, "updateSetsTextView: setsString='" + setsString + "'");
    }

    void dismiss() {
        if (restTimerNotificationVisible) {
            notificationManager.cancel(ID);
            restTimerNotificationVisible = false;
            Log.d(TAG, "dismissed");
        }
    }
}
