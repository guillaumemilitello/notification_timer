package com.simpleworkout.timer;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
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

    private ButtonsLayout buttonsLayout;
    private ButtonAction button0, button1, button2;

    void setVibrationEnable(boolean vibrationEnable) {
        this.vibrationEnable = vibrationEnable;
    }

    void setVibrationReadyEnable(boolean vibrationReadyEnable) {
        this.vibrationReadyEnable = vibrationReadyEnable;
    }

    void setLightColor(int lightColor) {
        this.lightColor = lightColor;
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
    private Uri ringtone;
    private Uri ringtoneReady;

    final static int COLOR_NONE = -1;

    private Context context;

    private static int [][] remoteViewLayouts = new int[][]
    {{ R.layout.notification_1b, R.layout.notification_2b, R.layout.notification_3b },
     { R.layout.notification_1b_progress, R.layout.notification_2b_progress, R.layout.notification_3b_progress }};

    private static int[][][] remoteViewResources = new int[][][]
    {{{ R.id.textViewTimer_1b, R.id.textViewSets_1b, R.id.button0_1b, R.id.unused_button, R.id.unused_button},
      { R.id.textViewTimer_2b, R.id.textViewSets_2b, R.id.button0_2b, R.id.button1_2b,    R.id.unused_button},
      { R.id.textViewTimer_3b, R.id.textViewSets_3b, R.id.button0_3b, R.id.button1_3b,    R.id.button2_3b}},
     {{ R.id.textViewTimer_1b_p, R.id.textViewSets_1b_p, R.id.button0_1b_p, R.id.unused_button, R.id.unused_button, R.id.progressBarTimer_1b_p},
      { R.id.textViewTimer_2b_p, R.id.textViewSets_2b_p, R.id.button0_2b_p, R.id.button1_2b_p,  R.id.unused_button, R.id.progressBarTimer_2b_p},
      { R.id.textViewTimer_3b_p, R.id.textViewSets_3b_p, R.id.button0_3b_p, R.id.button1_3b_p,  R.id.button2_3b_p,  R.id.progressBarTimer_3b_p}}};

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
                .setPriority(PRIORITY_MAX);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notificationBuilder.setStyle(new Notification.DecoratedCustomViewStyle());
            notificationBuilder.setCustomContentView(new RemoteViews(context.getPackageName(), R.layout.notification_1b));
            notificationBuilder.setColor(context.getColor(R.color.colorPrimary));
        }
        else {
            //noinspection deprecation
            notificationBuilder.setContent(new RemoteViews(context.getPackageName(), R.layout.notification_1b));
        }

        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        timerString = "";
        setsString = "";
        timerCurrent = 0;
        setsCurrent = 0;
        setsUser = 0;
        setsInit = 0;

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
                    button2 = (setsCurrent > setsInit)? ButtonAction.RESET : ButtonAction.NO_ACTION;
                    button1 = ButtonAction.START;
                    button0 = ButtonAction.DISMISS;
                    break;
                case RUNNING:
                    button2 = ButtonAction.NEXT_SET;
                    button1 = ButtonAction.PAUSE;
                    button0 = ButtonAction.TIMER_MINUS;
                    break;
                case PAUSED:
                    button2 = ButtonAction.NEXT_SET;
                    button1 = ButtonAction.RESUME;
                    button0 = ButtonAction.DISMISS;
                    break;
                case SET_DONE:
                    button2 = ButtonAction.NO_ACTION;
                    button1 = ButtonAction.START;
                    button0 = ButtonAction.DISMISS;
                    break;
                case ALL_SETS_DONE:
                    button2 = ButtonAction.RESET;
                    button1 = ButtonAction.EXTRA_SET;
                    button0 = ButtonAction.DISMISS;
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

    private void updateButton(RemoteViews remoteView, int id, ButtonAction action) {
        if (id == R.id.unused_button) {
            return;
        }

        int iconId;
        Intent intent;

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

    private RemoteViews createRemoteView() {
        int remoteViewId = (button2 != ButtonAction.NO_ACTION)? 2 : (button1 != ButtonAction.NO_ACTION)? 1 : 0;
        int progressBarId = drawProgressBar()? 1 : 0;

        RemoteViews remoteView = new RemoteViews(context.getPackageName(), remoteViewLayouts[progressBarId][remoteViewId]);

        remoteView.setTextViewText(remoteViewResources[progressBarId][remoteViewId][0], timerString);
        remoteView.setTextViewText(remoteViewResources[progressBarId][remoteViewId][1], setsString);
        updateButton(remoteView, remoteViewResources[progressBarId][remoteViewId][2], button0);
        updateButton(remoteView, remoteViewResources[progressBarId][remoteViewId][3], button1);
        updateButton(remoteView, remoteViewResources[progressBarId][remoteViewId][4], button2);

        if (progressBarId == 1) {
            remoteView.setProgressBar(remoteViewResources[1][remoteViewId][5], (int) timerUser, (int) (timerUser - timerCurrent), false);
        }

        return remoteView;
    }

    private boolean drawProgressBar() {
        switch (buttonsLayout) {
            default: case NO_LAYOUT: case PAUSED: case RUNNING: return true;
            case READY: case SET_DONE: case ALL_SETS_DONE: return false;
        }
    }

    private void build(NotificationMode notificationMode) {
        if (restTimerNotificationVisible && notificationMode != NotificationMode.NO_NOTIFICATION) {

            RemoteViews remoteView = createRemoteView();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                notificationBuilder.setCustomContentView(remoteView);
            } else {
                //noinspection deprecation
                notificationBuilder.setContent(remoteView);
            }

            switch (notificationMode) {
                case UPDATE:
                    notificationBuilder.setVibrate(null);
                    notificationBuilder.setSound(null);
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
                        notificationBuilder.setLights(lightColor, 1000, 1000);
                    }
                    break;
                default:
                case NO_NOTIFICATION:
                    Log.e(TAG, "build: notificationMode=" + notificationMode + " not specified");
                    break;
            }
            notificationManager.notify(ID, notificationBuilder.build());

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
                    if (setsUser == MainActivity.SETS_INFINITY) {
                        setsString = String.format(context.getString(R.string.notif_timer_info_infinity), getSetString(setsCurrent), setsInit);
                    } else {
                        setsString = String.format(context.getString(R.string.notif_timer_info), getSetString(setsCurrent), setsInit, setsUser);
                    }
                } else {
                    setsString = String.format(context.getString(R.string.notif_timer_number), getSetString(setsCurrent));
                }
                break;
            case READY:
                if (setsUser == MainActivity.SETS_INFINITY) {
                    setsString = String.format(context.getString(R.string.notif_timer_info_infinity), getSetString(setsCurrent), setsInit);
                } else {
                    setsString = String.format(context.getString(R.string.notif_timer_info), getSetString(setsCurrent), setsInit, setsUser);
                }
                break;
            case ALL_SETS_DONE:
            case SET_DONE:
                if (setsUser == MainActivity.SETS_INFINITY) {
                    setsString = String.format(context.getString(R.string.notif_timer_info_infinity), getSetString(setsCurrent - 1), setsInit);
                } else {
                    setsString = String.format(context.getString(R.string.notif_timer_info), getSetString(setsCurrent - 1), setsInit, setsUser);
                }
                break;
        }
        Log.d(TAG, "updateSetsTextView: setsString='" + setsString + "'");
    }

    private String getSetString(int sets) {
        String string = String.format(Locale.US, "%d", sets);
        if (sets >= 11 && sets <= 30) {
            return string + "th";
        }
        switch (sets % 10) {
            case 1:  return string + "st";
            case 2:  return string + "nd";
            case 3:  return string + "rd";
            default: return string + "th";
        }
    }

    void dismiss() {
        if (restTimerNotificationVisible) {
            notificationManager.cancel(ID);
            restTimerNotificationVisible = false;
            Log.d(TAG, "dismissed");
        }
    }
}
