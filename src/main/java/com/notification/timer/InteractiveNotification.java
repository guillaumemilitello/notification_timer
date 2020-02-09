package com.notification.timer;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import java.util.List;
import java.util.Locale;

@SuppressLint("ParcelCreator")
class InteractiveNotification extends Notification {

    private final static String TAG = "InteractiveNotification";

    private final static int ID = 52;

    public int getId() {
        return ID;
    }

    private final Context context;

    private boolean visible;

    private final NotificationManager notificationManager;
    private Notification.Builder notificationBuilder;
    private final PendingIntent pendingIntent;
    private final PendingIntent pendingIntentDeleted;

    private static final String doneChannelId = "doneChannelId";
    private static final String readyChannelId = "readyChannelId";
    private static final String updateChannelId = "updateChannelId";
    private static int doneChannelNbId = 0;
    private static int readyChannelNbId = 0;

    private static boolean notificationBuilt = false;

    @SuppressLint("DefaultLocale")
    static String getDoneChannelId() {  return String.format("%s%d", doneChannelId, doneChannelNbId); }
    @SuppressLint("DefaultLocale")
    static String getReadyChannelId() {  return String.format("%s%d", readyChannelId, readyChannelNbId); }

    @SuppressLint("DefaultLocale")
    private static String getNewDoneChannelId() {  return String.format("%s%d", doneChannelId, ++doneChannelNbId); }
    @SuppressLint("DefaultLocale")
    private static String getNewReadyChannelId() { return String.format("%s%d", readyChannelId, ++readyChannelNbId); }

    public Notification getNotification() { return  notificationBuilder.build(); }

    private int headsUpUpdateCount;

    // Timer service related
    private long timerCurrent, timerUser;
    private int setsCurrent, setsUser;
    private String nameUser;
    private String timerString, setsString;

    private boolean setsNumberDisplayEnable;
    private boolean setsNumberReset;
    private boolean timerGetReadyEnable;
    private int timerGetReady;

    private ButtonsLayout buttonsLayout;
    private ButtonAction button0, button1, button2;
    private long stepTime;
    private int stepTimeResId;

    void setVibrationEnable(boolean vibrationEnable) {
        this.vibrationEnable = vibrationEnable;
    }

    void setVibrationReadyEnable(boolean vibrationReadyEnable) {
        this.vibrationReadyEnable = vibrationReadyEnable;
    }

    void setSetsNumberReset(boolean setsNumberReset) {
        this.setsNumberReset = setsNumberReset;
    }

    void setSetsNumberDisplayEnable(boolean setsNumberDisplayEnable) {
        this.setsNumberDisplayEnable = setsNumberDisplayEnable;
        updateSetsTextView();
    }

    void setTimerGetReadyEnable(boolean timerGetReadyEnable) {
        this.timerGetReadyEnable = timerGetReadyEnable;
    }

    void setTimerGetReady(int timerGetReady) {
        this.timerGetReady = timerGetReady;
    }

    void setLightColorEnable(boolean lightColorEnable) {
        this.lightColorEnable = lightColorEnable;
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

    void setColorEnable(boolean colorEnable) {
        this.colorEnable = colorEnable;
    }

    void setColorRunning(int colorRunning) {
        this.colorRunning = colorRunning;
    }

    void setColorReady(int colorReady) {
        this.colorReady = colorReady;
    }

    void setColorDone(int colorDone) {
        this.colorDone = colorDone;
    }

    void setStepTime(long stepTime) {
        this.stepTime = stepTime;
        this.stepTimeResId = stepTime > 0 ? MainActivity.getTimerPlusResId(stepTime) : MainActivity.getTimerMinusResId(-stepTime);
    }

    void updateNotificationChannels() {
        if (notificationManager != null) {
            updateDoneChannel();
            updateReadyChannel();
        }
    }

    // Settings options
    private boolean vibrationEnable;
    private boolean vibrationReadyEnable;
    private boolean lightColorEnable;
    private int lightColor;
    private int lightFlashRateOn, lightFlashRateOff;
    private Uri ringtone;
    private Uri ringtoneReady;
    private boolean colorEnable;
    private int colorRunning;
    private int colorReady;
    private int colorDone;

    private enum ButtonAction {

        NO_ACTION("no_action"),
        START("start"),
        PAUSE("pause"),
        RESUME("resume"),
        RESET("reset"),
        NEXT_SET("next_set"),
        EXTRA_SET("extra_set"),
        STEP_TIME("step_time"),
        DISMISS("dismiss");

        private final String action;

        ButtonAction(String action) {
            this.action = action;
        }

        @NonNull
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

        private final String layout;

        ButtonsLayout(String layout) {
            this.layout = layout;
        }

        @NonNull
        @Override
        public String toString() {
            return layout;
        }
    }

    enum NotificationMode {

        NONE,
        UPDATE,
        READY,
        DONE
    }


    InteractiveNotification(Context context) {

        this.context = context;

        timerString = "";
        setsString = "";
        timerCurrent = 0;
        setsCurrent = 0;
        setsUser = 0;

        timerGetReadyEnable = false;
        timerGetReady = 15;

        button2 = ButtonAction.NO_ACTION;
        button1 = ButtonAction.NO_ACTION;
        button0 = ButtonAction.NO_ACTION;
        buttonsLayout = ButtonsLayout.NO_LAYOUT;

        headsUpUpdateCount = 0;

        // pending intent to go back to the main activity from the notification
        Intent notificationIntent = new Intent(context, MainActivity.class);
        pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

        // pending intent when the notification is deleted
        pendingIntentDeleted = PendingIntent.getBroadcast(context, 0, new Intent().setAction(IntentAction.NOTIFICATION_DISMISS), PendingIntent.FLAG_UPDATE_CURRENT);

        // attributes used to setup notification channels with sound
        audioAttributes = (new AudioAttributes.Builder()).setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN).setUsage(AudioAttributes.USAGE_NOTIFICATION).build();

        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager != null) {
            createNotificationChannels();
        }

        notificationBuilder = createDefaultNotificationBuilder();

        updateButtonsLayout(ButtonsLayout.READY);
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void createNotificationChannels() {
        boolean doneChannelCreated = false;
        boolean readyChannelCreated = false;
        boolean updateChannelCreated = false;
        List<NotificationChannel> notificationChannelList = notificationManager.getNotificationChannels();
        for (NotificationChannel notificationChannel : notificationChannelList) {
            if (notificationChannel.getId().contains(doneChannelId)) {
                doneChannelNbId = Integer.parseInt(notificationChannel.getId().replaceAll("[^0-9]", ""));
                doneChannelCreated = true;
                Log.d(TAG, "createNotificationChannels: " + notificationChannel.getId() + ", doneChannelNbId=" + doneChannelNbId);
            }
            else if (notificationChannel.getId().contains(readyChannelId)) {
                readyChannelNbId = Integer.parseInt(notificationChannel.getId().replaceAll("[^0-9]", ""));
                readyChannelCreated = true;
                Log.d(TAG, "createNotificationChannels: " + notificationChannel.getId() + ", readyChannelNbId=" + readyChannelNbId);
            }
            else if (notificationChannel.getId().equals(updateChannelId)) {
                updateChannelCreated = true;
            }
        }
        Log.d(TAG, "createNotificationChannels: doneChannelCreated=" + doneChannelCreated + ", readyChannelCreated=" + readyChannelCreated + ", updateChannelCreated=" + updateChannelCreated);
        if (!doneChannelCreated) {
            createDoneChannel();
        }
        if (!readyChannelCreated) {
            createReadyChannel();
        }
        if (!updateChannelCreated) {
            createUpdateChannel();
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void updateDoneChannel() {
        NotificationChannel currentNotificationChannel = notificationManager.getNotificationChannel(getDoneChannelId());
        Log.d(TAG, "updateDoneChannel: currentNotificationChannelId=" + currentNotificationChannel.getId());
        Uri uri = currentNotificationChannel.getSound();
        if (ringtone != null && uri != null && !ringtone.equals(uri)) {
            Log.d(TAG, "updateDoneChannel: ringtone=" + ringtone + ", uri=" + uri);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ringtone = uri;
            }
            createDoneChannel();
        } else if (ringtone == null && uri != null) {
            Log.d(TAG, "updateDoneChannel: ringtone=null, uri=" + uri);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ringtone = uri;
            }
            createDoneChannel();
        } else if (ringtone != null && uri == null) {
            Log.d(TAG, "updateDoneChannel: ringtone=" + ringtone + ", uri=null");
            createDoneChannel();
        } else if (currentNotificationChannel.shouldVibrate() != vibrationEnable) {
            Log.d(TAG, "updateDoneChannel: vibrationEnable=" + vibrationEnable);
            createDoneChannel();
        } else if (currentNotificationChannel.shouldShowLights() != lightColorEnable) {
            Log.d(TAG, "updateDoneChannel: lightColorEnable=" + lightColorEnable);
            createDoneChannel();
        } else if (currentNotificationChannel.getLightColor() != lightColor) {
            Log.d(TAG, "updateDoneChannel: color=" + color + ", lightColor=" + lightColor);
            createDoneChannel();
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void createDoneChannel() {
        notificationManager.deleteNotificationChannel(getDoneChannelId());
        NotificationChannel notificationChannel = new NotificationChannel(getNewDoneChannelId(), context.getString(R.string.notif_channel_done), NotificationManager.IMPORTANCE_HIGH);
        notificationChannel.setShowBadge(false);
        notificationChannel.setSound(ringtone, audioAttributes);
        notificationChannel.setVibrationPattern(MainActivity.vibrationPattern);
        notificationChannel.enableVibration(vibrationEnable);
        notificationChannel.enableLights(lightColorEnable);
        notificationChannel.setLightColor(lightColor);
        notificationManager.createNotificationChannel(notificationChannel);
        Log.d(TAG, "createDoneChannel: notificationChannel=" + notificationChannel);
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void updateReadyChannel() {
        NotificationChannel currentNotificationChannel = notificationManager.getNotificationChannel(getReadyChannelId());
        Log.d(TAG, "updateReadyChannel: currentNotificationChannelId=" + currentNotificationChannel.getId());
        Uri uri = currentNotificationChannel.getSound();
        if (ringtoneReady != null && uri != null && !ringtoneReady.equals(uri)) {
            Log.d(TAG, "updateReadyChannel: ringtoneReady=" + ringtoneReady + ", uri=" + uri);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ringtoneReady = uri;
            }
            createReadyChannel();
        } else if (ringtoneReady == null && uri != null) {
            Log.d(TAG, "updateReadyChannel: ringtoneReady=null, uri=" + uri);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ringtoneReady = uri;
            }
            createReadyChannel();
        } else if (ringtoneReady != null && uri == null) {
            Log.d(TAG, "updateReadyChannel: ringtoneReady=" + ringtoneReady + ", uri=null");
            createReadyChannel();
        } else if (currentNotificationChannel.shouldVibrate() != vibrationReadyEnable) {
            Log.d(TAG, "updateReadyChannel: vibrationReadyEnable=" + vibrationReadyEnable);
            createReadyChannel();
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void createReadyChannel() {
        notificationManager.deleteNotificationChannel(getReadyChannelId());
        NotificationChannel notificationChannel = new NotificationChannel(getNewReadyChannelId(), context.getString(R.string.notif_channel_ready), NotificationManager.IMPORTANCE_HIGH);
        notificationChannel.setShowBadge(false);
        notificationChannel.setSound(ringtoneReady, audioAttributes);
        notificationChannel.setVibrationPattern(MainActivity.vibrationPattern);
        notificationChannel.enableVibration(vibrationReadyEnable);
        notificationChannel.enableLights(false);
        notificationManager.createNotificationChannel(notificationChannel);
        Log.d(TAG, "createReadyChannel: notificationChannel=" + notificationChannel);
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void createUpdateChannel() {
        NotificationChannel notificationChannel = new NotificationChannel(updateChannelId, context.getString(R.string.notif_channel_update), NotificationManager.IMPORTANCE_LOW);
        notificationChannel.setShowBadge(false);
        notificationChannel.enableVibration(false);
        notificationChannel.enableLights(false);
        notificationManager.createNotificationChannel(notificationChannel);
        Log.d(TAG, "createUpdateChannel: notificationChannel=" + notificationChannel);
    }

    void update(int setsCurrent, long timerCurrent, ButtonsLayout layout) {
        updateTimerCurrent(timerCurrent);
        updateSetsCurrent(setsCurrent);
        updateButtonsLayout(layout, NotificationMode.UPDATE);
    }

    void updateButtonsLayout(ButtonsLayout layout) {
        updateButtonsLayout(layout, NotificationMode.NONE);
    }

    void updateButtonsLayout(ButtonsLayout layout, NotificationMode notificationMode) {
        Log.d(TAG, "updateButtonsLayout: layout=" + layout.toString() + ", buttonsLayout=" + buttonsLayout.toString() + ", timerCurrent=" + timerCurrent + ", setsCurrent=" + setsCurrent);
        if (buttonsLayout == ButtonsLayout.READY || buttonsLayout != layout) {
            switch (layout) {
                case READY:
                    // Do not display the RESET button for the first set
                    if ((!setsNumberReset && setsUser == Integer.MAX_VALUE) || setsCurrent <= 1) {
                        button2 = ButtonAction.NO_ACTION;
                        button1 = ButtonAction.DISMISS;
                    } else {
                        button2 = ButtonAction.DISMISS;
                        button1 = ButtonAction.RESET;
                    }
                    button0 = ButtonAction.START;
                    break;
                case RUNNING:
                    button2 = ButtonAction.STEP_TIME;
                    button1 = ButtonAction.NEXT_SET;
                    button0 = ButtonAction.PAUSE;
                    break;
                case PAUSED:
                    button2 = ButtonAction.DISMISS;
                    button1 = ButtonAction.NEXT_SET;
                    button0 = ButtonAction.RESUME;
                    break;
                case SET_DONE:
                    if (!setsNumberReset && setsUser == Integer.MAX_VALUE) {
                        button2 = ButtonAction.NO_ACTION;
                        button1 = ButtonAction.DISMISS;
                    } else {
                        button2 = ButtonAction.DISMISS;
                        button1 = ButtonAction.RESET;
                    }
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

    private void updateButton(RemoteViews remoteView, int id, ButtonAction action) {
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
                iconId = R.drawable.ic_play;
                intent = new Intent().setAction(IntentAction.START);
                break;
            case PAUSE:
                iconId = R.drawable.ic_pause;
                intent = new Intent().setAction(IntentAction.PAUSE);
                break;
            case RESUME:
                iconId = R.drawable.ic_play;
                intent = new Intent().setAction(IntentAction.RESUME);
                break;
            case NEXT_SET:
                iconId = R.drawable.ic_chevron_right;
                intent = new Intent().setAction(IntentAction.NEXT_SET);
                break;
            case EXTRA_SET:
                iconId = R.drawable.ic_chevrons_right;
                intent = new Intent().setAction(IntentAction.EXTRA_SET);
                break;
            case STEP_TIME:
                iconId = stepTimeResId;
                if (stepTime > 0 && timerCurrent + stepTime > MainActivity.TIMER_MAX) {
                    intent = new Intent().setAction(IntentAction.NO_ACTION);
                } else {
                    intent = new Intent().setAction(IntentAction.STEP_TIME);
                }
                break;
            case RESET:
                iconId = R.drawable.ic_chevrons_left;
                intent = new Intent().setAction(IntentAction.RESET);
                break;
            case DISMISS:
                iconId = R.drawable.ic_preset_delete;
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
        visible = true;
        build(NotificationMode.UPDATE);
    }

    private boolean isColorEnable() {
        return colorEnable && buttonsLayout != ButtonsLayout.READY;
    }

    private RemoteViews createRemoteView() {
        RemoteViews remoteView;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isColorEnable()) {
            if (MainActivity.isColorDark(context, getColor())) {
                remoteView = new RemoteViews(context.getPackageName(), R.layout.notification_white_text);
            } else {
                remoteView = new RemoteViews(context.getPackageName(), R.layout.notification_black_text);
            }
        } else {
            if (isColorGetReady()) {
                remoteView = new RemoteViews(context.getPackageName(), R.layout.notification_red_text);
            } else {
                remoteView = new RemoteViews(context.getPackageName(), R.layout.notification);
            }
        }

        updateButton(remoteView, R.id.button0, button0);
        updateButton(remoteView, R.id.button1, button1);
        updateButton(remoteView, R.id.button2, button2);

        if (drawProgressBar()) {
            remoteView.setTextViewText(R.id.textViewSets, "");
            remoteView.setTextViewText(R.id.textViewSets_short, setsString);
            remoteView.setViewVisibility(R.id.progressBarTimer, View.VISIBLE);
            remoteView.setProgressBar(R.id.progressBarTimer, (int) timerUser, (int) (timerUser - timerCurrent), false);
        } else {
            remoteView.setTextViewText(R.id.textViewSets, setsString);
            remoteView.setTextViewText(R.id.textViewSets_short, "");
            remoteView.setViewVisibility(R.id.progressBarTimer, View.INVISIBLE);
        }
        remoteView.setTextViewText(R.id.textViewTimer, timerString);

        return remoteView;
    }

    private boolean drawProgressBar() {
        switch (buttonsLayout) {
            default: case NO_LAYOUT: case PAUSED: case RUNNING: return true;
            case READY: case SET_DONE: case ALL_SETS_DONE: return false;
        }
    }

    private Notification.Builder createDefaultNotificationBuilder() {
        return createNotificationBuilder(NotificationMode.UPDATE);
    }

    private Notification.Builder createNotificationBuilder(NotificationMode notificationMode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            switch (notificationMode) {
                case DONE:
                    return new Builder(context, getDoneChannelId())
                            .setSmallIcon(R.drawable.ic_notification)
                            .setContentIntent(pendingIntent)
                            .setDeleteIntent(pendingIntentDeleted)
                            .setColorized(isColorEnable())
                            .setColor(getColor());
                case READY:
                    return new Builder(context, getReadyChannelId())
                            .setSmallIcon(R.drawable.ic_notification)
                            .setContentIntent(pendingIntent)
                            .setDeleteIntent(pendingIntentDeleted)
                            .setColorized(isColorEnable())
                            .setColor(getColor());
                default:
                    return new Builder(context, updateChannelId)
                            .setSmallIcon(R.drawable.ic_notification)
                            .setContentIntent(pendingIntent)
                            .setDeleteIntent(pendingIntentDeleted)
                            .setColorized(isColorEnable())
                            .setColor(getColor());
            }
        } else {
            switch (notificationMode) {
                case DONE:
                    return new Builder(context)
                            .setSmallIcon(R.drawable.ic_notification)
                            .setContentIntent(pendingIntent)
                            .setDeleteIntent(pendingIntentDeleted)
                            .setPriority(PRIORITY_MAX)
                            .setLights(lightColorEnable ? lightColor : COLOR_DEFAULT, lightFlashRateOn, lightFlashRateOff)
                            .setVibrate(vibrationEnable ? MainActivity.vibrationPattern : null)
                            .setSound(ringtone)
                            .setColor(getColor());
                case READY:
                    return new Builder(context)
                            .setSmallIcon(R.drawable.ic_notification)
                            .setContentIntent(pendingIntent)
                            .setDeleteIntent(pendingIntentDeleted)
                            .setPriority(PRIORITY_MAX)
                            .setLights(COLOR_DEFAULT, 0, 0)
                            .setVibrate(vibrationReadyEnable ? MainActivity.vibrationPattern : null)
                            .setSound(ringtoneReady)
                            .setColor(getColor());
                default:
                    return new Builder(context)
                            .setSmallIcon(R.drawable.ic_notification)
                            .setContentIntent(pendingIntent)
                            .setDeleteIntent(pendingIntentDeleted)
                            .setPriority(PRIORITY_MAX)
                            .setLights(COLOR_DEFAULT, 0, 0)
                            .setColor(getColor());
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    private void setCustomContent(RemoteViews remoteView, NotificationMode notificationMode, boolean layoutSetDone) {
        if (notificationMode == NotificationMode.UPDATE && headsUpUpdateCount > 0) {
            // Avoid updating the headsUpContentView when it won't be visible
            notificationBuilder.setCustomHeadsUpContentView(remoteView);
            headsUpUpdateCount--;
        }
        else if (notificationMode == NotificationMode.READY) {
            notificationBuilder.setCustomHeadsUpContentView(remoteView);
            // Update the next 2 seconds of the headsUpContentView when it's running
            headsUpUpdateCount = 2;
        }
        else {
            notificationBuilder.setCustomHeadsUpContentView(remoteView);
        }
        notificationBuilder.setCustomContentView(remoteView);
        notificationBuilder.setUsesChronometer(layoutSetDone);
    }

    private int getColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isColorEnable()) {
            switch (buttonsLayout) {
                default:
                case NO_LAYOUT:
                case PAUSED:
                case RUNNING:
                    if (isColorGetReady()) {
                        return colorReady;
                    } else {
                        return colorRunning;
                    }
                case SET_DONE:
                case ALL_SETS_DONE:
                    return colorDone;
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            switch (buttonsLayout) {
                case NO_LAYOUT:
                case READY:
                    return ContextCompat.getColor(context, R.color.progress_bar_current);
                case PAUSED:
                case RUNNING:
                    if (isColorGetReady()) {
                        return ContextCompat.getColor(context, R.color.progress_bar_get_ready);
                    } else {
                        return ContextCompat.getColor(context, R.color.progress_bar_current);
                    }
                case SET_DONE:
                case ALL_SETS_DONE:
                    return ContextCompat.getColor(context, R.color.progress_bar_get_ready);
            }
        }
        return colorRunning;
    }

    private boolean isColorGetReady() {
        return timerCurrent == 0 || (timerGetReadyEnable && timerCurrent <= timerGetReady && timerUser > timerGetReady);
    }

    void updateNotificationBuilder() {
        Log.d(TAG, "updateNotificationBuilder");
        // Force update the notification when the service has crashed regardless of the layout
        notificationBuilder = createNotificationBuilder(NotificationMode.UPDATE);
    }

    private void build(NotificationMode notificationMode) {
        if (notificationMode != NotificationMode.NONE) {
            final boolean layoutSetDone = buttonsLayout == ButtonsLayout.ALL_SETS_DONE || buttonsLayout == ButtonsLayout.SET_DONE;
            if (visible) {
                // Do not recreate the notification when updating a DONE notification to preserve the chronometer counter
                if (notificationMode != NotificationMode.UPDATE || !layoutSetDone || !notificationBuilt) {
                    notificationBuilder = createNotificationBuilder(notificationMode);
                    notificationBuilt = true;
                }

                RemoteViews remoteView = createRemoteView();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    notificationBuilder.setStyle(new DecoratedCustomViewStyle());
                    setCustomContent(remoteView, notificationMode, layoutSetDone);
                } else {
                    notificationBuilder.setContent(remoteView);
                }

                notificationManager.notify(ID, notificationBuilder.build());

                if (layoutSetDone) {
                    if (notificationMode == NotificationMode.DONE) {
                        notificationBuilt = true;
                    }
                } else {
                    notificationBuilt = false;
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    notificationBuilder.setChannelId(updateChannelId);
                } else {
                    notificationBuilder.setSound(null);
                    notificationBuilder.setVibrate(null);
                }
            } else if(layoutSetDone) {
                // layoutSetDone should be recreated on the next update if visible
                notificationBuilt = false;
            }
        }
    }

    private void updateTimerCurrent(long timer) {
        updateTimerCurrent(timer, NotificationMode.NONE);
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
        updateSetsCurrent(sets, NotificationMode.NONE);
    }

    void updateNameUser(String name) {
        Log.d(TAG, "updateNameUser: nameUser=" + nameUser);
        nameUser = name;
        updateSetsTextView();
        build(NotificationMode.UPDATE);
    }

    void updateSetsCurrent(int sets, NotificationMode notificationMode) {
        Log.d(TAG, "updateSetsCurrent: setsCurrent=" + sets);
        setsCurrent = sets;
        updateSetsTextView();
        build(notificationMode);
    }

    void updateSetsUser(int sets) {
        Log.d(TAG, "updateSetsUser: setsUser=" + sets);
        setsUser = sets;
        updateSetsTextView();
        build(NotificationMode.UPDATE);
    }

    private void updateTimerTextView() {
        switch (buttonsLayout) {
            case NO_LAYOUT:
            case READY:
            case PAUSED:
            case RUNNING:
                if (timerCurrent >= 3600) {
                    timerString = String.format(Locale.US, "%d:%02d:%02d", timerCurrent / 3600, timerCurrent % 3600 / 60, timerCurrent % 60);
                } else {
                    timerString = String.format(Locale.US, "%d:%02d", timerCurrent / 60, timerCurrent % 60);
                }
                break;
            case SET_DONE:
            case ALL_SETS_DONE:
                timerString = context.getString(R.string.notif_timer_done);
                break;
        }
    }

    @SuppressLint("DefaultLocale")
    private void updateSetsTextView() {
        final boolean setsUserDisplayEnabled = setsUser != Integer.MAX_VALUE;
        if (!setsNumberDisplayEnable && !setsUserDisplayEnabled) {
            setsString = nameUser;
        } else {
            final int setsCurrentDisplay = (buttonsLayout == ButtonsLayout.ALL_SETS_DONE || buttonsLayout == ButtonsLayout.SET_DONE) ? setsCurrent - 1 : setsCurrent;
            if (setsUserDisplayEnabled) {
                setsString = String.format("%s %d/%d", nameUser, setsCurrentDisplay, setsUser);
            } else {
                setsString = String.format("%s %d", nameUser, setsCurrentDisplay);
            }
        }
        Log.d(TAG, "updateSetsTextView: nameUser=" + nameUser + "setsString='" + setsString + "'");
    }

    void dismiss() {
        if (visible) {
            notificationManager.cancel(ID);
            visible = false;
            Log.d(TAG, "dismissed");
        }
    }
}
