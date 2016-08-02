package com.simpleworkout.timer;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.Locale;

@SuppressLint("ParcelCreator")
public class InteractiveNotification extends Notification {

    private final static String TAG = "InteractiveNotification";

    private final static int ID = 52;
    public static int getID() { return ID; }

    private boolean restTimerNotificationVisible;

    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;

    // Timer service related
    private long timerCurrent;
    private int setsCurrent;
    private String timerString, setsString;

    private ButtonsLayout buttonsLayout;
    private ButtonAction button0, button1, button2;

    public void setTimerMinus(long timerMinus) { this.timerMinus = timerMinus; }
    public void setVibrationEnable(boolean vibrationEnable) { this.vibrationEnable = vibrationEnable; }
    public void setVibrationReadyEnable(boolean vibrationReadyEnable) { this.vibrationReadyEnable = vibrationReadyEnable; }
    public void setLightColor(int lightColor) { this.lightColor = lightColor; }
    public void setLightReadyColor(int lightReadyColor) { this.lightReadyColor = lightReadyColor; }
    public void setRingtone(Uri ringtone) { this.ringtone = ringtone; }
    public void setRingtoneReady(Uri ringtoneReady) { this.ringtoneReady = ringtoneReady; }

    // Settings options
    private long timerMinus = 30;
    private boolean vibrationEnable = true;
    private boolean vibrationReadyEnable = true;
    private int lightColor = Color.GREEN;
    private int lightReadyColor = Color.YELLOW;
    private Uri ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    private Uri ringtoneReady = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

    public final static int COLOR_NONE = -1;

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

    protected enum ButtonsLayout {

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

    public InteractiveNotification(Context context) {

        this.context = context;

        // pending intent to go back to the main activity from the notification
        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

        // pending intent when the notification is deleted
        PendingIntent pendingIntentDeleted = PendingIntent.getBroadcast(context, 0, new Intent().setAction(IntentAction.NOTIFICATION_DISMISS), PendingIntent.FLAG_UPDATE_CURRENT);

        // Default configurations
        RemoteViews contentView = new RemoteViews(context.getPackageName(), R.layout.notification_1_button);
        contentView.setImageViewResource(R.id.notification1Button0, R.drawable.ic_play_arrow_black_48dp);

        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // prepare Notification Builder
        notificationBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setDeleteIntent(pendingIntentDeleted)
                .setContent(contentView)
                .setPriority(PRIORITY_MAX);

        timerString = "";
        setsString = "";
        timerCurrent = 0;
        setsCurrent = 1;

        button2 = ButtonAction.NO_ACTION;
        button1 = ButtonAction.NO_ACTION;
        button0 = ButtonAction.NO_ACTION;
        buttonsLayout = ButtonsLayout.NO_LAYOUT;

        updateButtonsLayout(ButtonsLayout.READY);
    }

    protected void updateButtonsLayout(ButtonsLayout layout) {
        Log.d(TAG, "updateButtonsLayout: layout=" + layout.toString() + ", buttonsLayout=" + buttonsLayout.toString() +", timerCurrent=" + timerCurrent + ", setsCurrent=" + setsCurrent);
        if(buttonsLayout == ButtonsLayout.RUNNING || buttonsLayout != layout) {
            switch (layout) {
                case READY:
                    button2 = ButtonAction.NO_ACTION;
                    button1 = ButtonAction.DISMISS;
                    button0 = ButtonAction.START;
                    notificationBuilder.setOngoing(false);
                    break;
                case RUNNING:
                    if(timerCurrent > timerMinus) {
                        button2 = ButtonAction.NEXT_SET;
                        button1 = ButtonAction.TIMER_MINUS;
                    }
                    else {
                        button2 = ButtonAction.NO_ACTION;
                        button1 = ButtonAction.NEXT_SET;
                    }
                    if(setsCurrent > 1)
                        button2 = ButtonAction.NEXT_SET_START;
                    button0 = ButtonAction.PAUSE;
                    notificationBuilder.setOngoing(true);
                    break;
                case PAUSED:
                    button2 = ButtonAction.DISMISS;
                    button1 = ButtonAction.NEXT_SET;
                    button0 = ButtonAction.RESUME;
                    notificationBuilder.setOngoing(false);
                    break;
                case SET_DONE:
                    button2 = ButtonAction.NO_ACTION;
                    button1 = ButtonAction.DISMISS;
                    button0 = ButtonAction.START;
                    notificationBuilder.setOngoing(false);
                    break;
                case ALL_SETS_DONE:
                    button2 = ButtonAction.EXTRA_SET;
                    button1 = ButtonAction.DISMISS;
                    button0 = ButtonAction.RESET;
                    notificationBuilder.setOngoing(false);
                    break;
                default:
                    Log.e(TAG, "updateButtonsLayout: layout=" + layout.toString());
            }
            buttonsLayout = layout;
            Log.d(TAG, "updateButtonsLayout: buttonsLayout=" + buttonsLayout.toString());
        }
    }

    private void updateButton(RemoteViews remoteView, int id, ButtonAction action) {

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

    protected void setVisible() {
        restTimerNotificationVisible = true;
        build(0);
    }

    private RemoteViews createRemoteView() {

        RemoteViews remoteView;

        if(button2 != ButtonAction.NO_ACTION) {
            remoteView = new RemoteViews(context.getPackageName(), R.layout.notification_3_buttons);
            updateButton(remoteView, R.id.notification3Buttons2, button2);
            updateButton(remoteView, R.id.notification3Buttons1, button1);
            updateButton(remoteView, R.id.notification3Buttons0, button0);
            remoteView.setTextViewText(R.id.textView3NotificationTimer, timerString);
            remoteView.setTextViewText(R.id.textView3NotificationSets, setsString);
        }
        else if(button1 != ButtonAction.NO_ACTION) {
            remoteView = new RemoteViews(context.getPackageName(), R.layout.notification_2_buttons);
            updateButton(remoteView, R.id.notification2Buttons1, button1);
            updateButton(remoteView, R.id.notification2Buttons0, button0);
            remoteView.setTextViewText(R.id.textView2NotificationTimer, timerString);
            remoteView.setTextViewText(R.id.textView2NotificationSets, setsString);
        }
        else {
            remoteView = new RemoteViews(context.getPackageName(), R.layout.notification_1_button);
            updateButton(remoteView, R.id.notification1Button0, button0);
            remoteView.setTextViewText(R.id.textView1NotificationTimer, timerString);
            remoteView.setTextViewText(R.id.textView1NotificationSets, setsString);
        }

        return remoteView;
    }

    protected void build(int alertLevel) {

        if(restTimerNotificationVisible) {

            RemoteViews remoteView = createRemoteView();
            notificationBuilder.setContent(remoteView);

            // pending intent to go back to the main activity from the notification
            Intent notificationIntent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

            // pending intent when the notification is deleted
            PendingIntent pendingIntentDeleted = PendingIntent.getBroadcast(context, 0, new Intent().setAction(IntentAction.NOTIFICATION_DISMISS), PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Builder notificationBuilder;
            // prepare Notification Builder
            notificationBuilder = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentIntent(pendingIntent)
                    .setDeleteIntent(pendingIntentDeleted)
                    .setContent(remoteView)
                    .setPriority(PRIORITY_MAX);

            switch (alertLevel) {
                default:
                case 0:
                    notificationBuilder.setVibrate(null);
                    notificationBuilder.setSound(null);
                    notificationBuilder.setLights(COLOR_DEFAULT, 0, 0);
                    notificationBuilder.setPriority(PRIORITY_MAX);
                    break;
                // Light, sound and short vibration
                case 1:
                    if(vibrationReadyEnable)
                        notificationBuilder.setVibrate(MainActivity.vibrationPattern);
                    else
                        notificationBuilder.setVibrate(null);
                    notificationBuilder.setSound(ringtoneReady);
                    if(lightReadyColor != COLOR_NONE)
                        notificationBuilder.setLights(lightReadyColor, 500, 600);
                    notificationBuilder.setPriority(PRIORITY_MAX);
                    break;
                // Light, sound and long vibration
                case 2:
                    if(vibrationEnable)
                        notificationBuilder.setVibrate(MainActivity.vibrationPattern);
                    notificationBuilder.setSound(ringtone);
                    if(lightColor != COLOR_NONE)
                        notificationBuilder.setLights(lightColor, 1000, 1000);
                    notificationBuilder.setPriority(PRIORITY_MAX);
                    break;
                // Light only
                case 3:
                    notificationBuilder.setVibrate(null);
                    notificationBuilder.setSound(null);
                    if(lightReadyColor != -1)
                        notificationBuilder.setLights(lightReadyColor, 500, 600);
                    notificationBuilder.setPriority(PRIORITY_MAX);
                    break;
            }
            notificationManager.notify(ID, notificationBuilder.build());
            Log.d(TAG, "build : alertLevel=" + alertLevel);
        }
    }

    protected void dismiss() {

        if(restTimerNotificationVisible) {
            notificationManager.cancel(ID);
            restTimerNotificationVisible = false;
            Log.d(TAG, "dismissed");
        }
    }

    protected void updateTimerTextView(long time) {
        timerCurrent = time;
        timerString = String.format(Locale.US, "%d:%02d", timerCurrent / 60, timerCurrent % 60);
        Log.d(TAG, "updateTimerTextView: timerString='" + timerString + "'");
    }

    protected void updateSetsTextView(int sets) {
        setsCurrent = sets;
        if (sets > 1)
            setsString = String.format("%d " + context.getString(com.simpleworkout.timer.R.string.more_timers), sets);
        // Keep "Extra set" displayed
        else if (setsCurrent == 1 && !setsString.equals(context.getString(R.string.extra_timer)))
            setsString = "Last timer";
        Log.d(TAG, "updateSetsTextView: setsString='" + setsString + "'");
    }

    protected void updateOneSetTextViews() {
        setsCurrent = 1;
        setsString = context.getString(R.string.one_timer);
        Log.d(TAG, "updateOneSetTextViews: setsString='" + setsString + "'");
    }

    protected void updateExtraSetTextViews() {
        setsCurrent = 1;
        setsString = context.getString(R.string.extra_timer);
        Log.d(TAG, "updateExtraSetTextViews: setsString='" + setsString + "'");
    }

    protected void updateAllSetsDoneTextView() {
        setsString = context.getString(R.string.all_timers_done);
        Log.d(TAG, "updateAllSetsDoneTextView: setsString='" + setsString + "'");
    }

    protected void alertSetDone() {
        Log.d(TAG, "alertSetDone: setting alert set done");
        timerString = context.getString(R.string.time_is_up);
        updateButtonsLayout(ButtonsLayout.SET_DONE);
    }

    protected void alertAllSetsDone() {
        Log.d(TAG, "setting alert all sets done");
        timerString = context.getString(R.string.time_is_up);
        setsString = context.getString(R.string.all_timers_done);
        updateButtonsLayout(ButtonsLayout.ALL_SETS_DONE);
    }
}
