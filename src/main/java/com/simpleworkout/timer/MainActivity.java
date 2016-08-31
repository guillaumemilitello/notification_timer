package com.simpleworkout.timer;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.codetroopers.betterpickers.mspicker.MsPickerBuilder;
import com.codetroopers.betterpickers.mspicker.MsPickerDialogFragment;
import com.codetroopers.betterpickers.numberpicker.NumberPickerBuilder;
import com.codetroopers.betterpickers.numberpicker.NumberPickerDialogFragment;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.simpleworkout.timer.TimerService.TimerBinder;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Locale;
import java.util.Map;

/**
 * TODO:  --> UI update
 * TODO: remove errors logs
 * TODO: clean code -> first github release : get a  LICENSE
 * TODO: add memorized setup + UI icons on top
 * TODO: finalize UI : main activity
 * TODO: finalize UI : text messages
 * TODO: finalize UI : status bar color at launch
 *
 * TODO: option screen on
 * TODO: about screen
 * TODO: merge request on pickers
 */

/**
 * Icons list:
 * add alarm
 * thicker chevrons
 * delete pickers ?
 * APPLICATION ICON
 */

/**
 * Bug list:
 * random crashes
 * bluetooth out on mainactivity
 */

public class MainActivity extends AppCompatActivity implements MsPickerDialogFragment.MsPickerDialogHandlerV2,
NumberPickerDialogFragment.NumberPickerDialogHandlerV2 {

    private static final String TAG = "MainActivity";

    private boolean mainActivityVisible;
    
    private MainActivityReceiver mainActivityReceiver;

    private TimerService timerService;
    private boolean timerServiceBound;

    // Timer done alerts
    private AlertDialog alertSetDone, alertAllSetsDone;

    // MainActivity user interface
    private MsPickerBuilder timerPickerBuilder;
    private NumberPickerBuilder setsPickerBuilder;
    private boolean timerPickerDone, setsPickerDone;
    private TextView timerTextView,setsNumbersTextView;
    private ProgressBar timerProgressBar;
    private ButtonsLayout buttonsLayout;
    private ButtonAction buttonLeftAction, buttonCenterAction, buttonRightAction;
    private ImageButton imageButtonLeft, imageButtonCenter, imageButtonRight;
    private ImageButton imageButtonTimerMinus, imageButtonTimerPlus, imageButtonSetsMinus, imageButtonSetsPlus;
    private ImageButton imageButtonPresetLeft, imageButtonPresetCenter, imageButtonPresetRight;
    private TextView presetLeftTextView, presetCenterTextView, presetRightTextView;
    private final float alphaEnabled = (float) 1.0;
    private final float alphaDisabled = (float) 0.3;

    // Timer service related
    private TimerService.State timerState;
    private long timerCurrent;
    private long timerUser;
    private int setsCurrent;
    private int setsUser;

    // Settings
    protected static final long[] vibrationPattern = { 0, 400, 200, 400, };

    // User preferences
    private boolean timerGetReadyEnable = true;
    private int timerGetReady = 15;
    private long timerMinus = 30;
    private long timerPlus = 30;
    private boolean vibrationEnable = false;
    private boolean vibrationReadyEnable = false;
    private Uri ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    private Uri ringtoneReady = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

    public SharedPreferences sharedPreferences;

    // Button actions available for this activity
    private enum ButtonAction {

        NO_ACTION("no_action"),
        START("start"),
        INPUT("input"),
        PAUSE("pause"),
        RESUME("resume"),
        CLEAR("clear"),
        CLEAR_DISABLED("clear_disabled"),
        RESET("reset"),
        RESET_DISABLED("reset_disabled"),
        NEXT_SET_START("next_set_start"),
        NEXT_SET_START_DISABLED("next_set_start_disabled");

        private String action;
        ButtonAction(String action) {
            this.action = action;
        }

        @Override
        public String toString() {
            return action;
        }
    }

    // Predefined layout for this activity
    private enum ButtonsLayout {

        NO_LAYOUT("no_layout"),
        WAITING("waiting"),
        WAITING_SETS("waiting_sets"),
        READY("ready"),
        RUNNING("running"),
        PAUSED("paused"),
        STOPPED("stopped");

        private String layout;
        ButtonsLayout(String layout) {
            this.layout = layout;
        }

        @Override
        public String toString() {
            return layout;
        }
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    @SuppressWarnings("deprecation")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Update system color bar and icon for the system
        setSupportActionBar((Toolbar) findViewById(R.id.actionBar));
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().setStatusBarColor(getColor(R.color.colorPrimaryDark));
            setTaskDescription(new ActivityManager.TaskDescription(getApplicationInfo().name,
                    BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher),
                    getColor(R.color.colorPrimary)));
        } else {
            getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
            setTaskDescription(new ActivityManager.TaskDescription(getApplicationInfo().name,
                    BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher),
                    getResources().getColor(R.color.colorPrimary)));
        }

        timerTextView = (TextView) findViewById(R.id.textViewTimer);
        setsNumbersTextView = (TextView) findViewById(R.id.textViewSetsNumber);

        AlertBuilderSetDone alertBuilderSetDone = new AlertBuilderSetDone(this);
        AlertBuilderAllSetsDone alertBuilderAllSetsDone = new AlertBuilderAllSetsDone(this);
        alertSetDone = alertBuilderSetDone.create();
        alertAllSetsDone = alertBuilderAllSetsDone.create();

        timerPickerBuilder = new MsPickerBuilder();
        timerPickerBuilder.setFragmentManager(getSupportFragmentManager());
        timerPickerBuilder.setStyleResId(R.style.BetterPickersDialogFragment_Light);
        timerPickerBuilder.setTimeInSeconds(0);

        setsPickerBuilder = new NumberPickerBuilder();
        setsPickerBuilder.setFragmentManager(getSupportFragmentManager());
        setsPickerBuilder.setStyleResId(R.style.BetterPickersDialogFragment_Light);
        setsPickerBuilder.setDecimalVisibility(View.INVISIBLE);
        setsPickerBuilder.setPlusMinusVisibility(View.INVISIBLE);

        timerPickerDone = false;
        setsPickerDone = false;

        timerProgressBar = (ProgressBar) findViewById(R.id.timerProgressBar);

        imageButtonLeft = (ImageButton) findViewById(R.id.imageButtonLeft);
        imageButtonCenter = (ImageButton) findViewById(R.id.imageButtonCenter);
        imageButtonRight = (ImageButton) findViewById(R.id.imageButtonRight);

        imageButtonPresetLeft = (ImageButton) findViewById(R.id.imageButtonPresetLeft);
        imageButtonPresetCenter = (ImageButton) findViewById(R.id.imageButtonPresetCenter);
        imageButtonPresetRight = (ImageButton) findViewById(R.id.imageButtonPresetRight);
        imageButtonPresetLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { presetInput(0); }
        });
        imageButtonPresetCenter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { presetInput(1); }
        });
        imageButtonPresetRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { presetInput(2); }
        });

        presetLeftTextView = (TextView) findViewById(R.id.textViewPresetLeft);
        presetCenterTextView = (TextView) findViewById(R.id.textViewPresetCenter);
        presetRightTextView= (TextView) findViewById(R.id.textViewPresetRight);

        imageButtonTimerMinus = (ImageButton) findViewById(R.id.imageButtonTimerMinus);
        imageButtonTimerPlus = (ImageButton) findViewById(R.id.imageButtonTimerPlus);
        imageButtonSetsMinus = (ImageButton) findViewById(R.id.imageButtonSetsMinus);
        imageButtonSetsPlus = (ImageButton) findViewById(R.id.imageButtonSetsPlus);
        imageButtonTimerMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { sendBroadcast(new Intent(IntentAction.TIMER_MINUS)); }
        });
        imageButtonTimerPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { sendBroadcast(new Intent(IntentAction.TIMER_PLUS)); }
        });
        imageButtonSetsMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { sendBroadcast(new Intent(IntentAction.SETS_MINUS)); }
        });
        imageButtonSetsPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { sendBroadcast(new Intent(IntentAction.SETS_PLUS)); }
        });

        buttonLeftAction = ButtonAction.NO_ACTION;
        buttonCenterAction = ButtonAction.NO_ACTION;
        buttonRightAction = ButtonAction.NO_ACTION;
        buttonsLayout = ButtonsLayout.NO_LAYOUT;

        // Catching button actions through intent from the notification
        mainActivityReceiver = new MainActivityReceiver();
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
        filter.addAction(IntentAction.SET_DONE);
        filter.addAction(IntentAction.ALL_SETS_DONE);
        filter.addAction(IntentAction.TIMER_STATE);
        filter.addAction(IntentAction.TIMER_UPDATE);
        filter.addAction(IntentAction.TIMER_DONE);
        registerReceiver(mainActivityReceiver, filter);

        Intent intent = new Intent(this, TimerService.class);
        startService(intent);
        bindService(intent, serviceConnection, Context.BIND_ABOVE_CLIENT);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        if(!timerServiceIsRunning()) {
            Log.d(TAG, "onCreate: starting service TimerService");
            startService(new Intent(getBaseContext(), TimerService.class));
        }

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    public void onStart() {
        super.onStart();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.simpleworkout.timer/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);

        mainActivityVisible = true;

        timerCurrent = 0;
        timerUser = 0;
        setsCurrent = 1;
        setsUser = 1;
        timerState = TimerService.State.WAITING;

        if(timerServiceBound) {
            getTimerServiceContext();
            updateUserInterface();
        }
    }

    private void getTimerServiceContext() {
        timerCurrent = timerService.getTimerCurrent();
        timerUser = timerService.getTimerUser();
        setsCurrent = timerService.getSetsCurrent();
        setsUser = timerService.getSetsUser();
        timerState = timerService.getState();
        Log.d(TAG, "updateUserInterface: timerCurrent=" + timerCurrent + ", timerUser=" + timerUser +
                ", setsCurrent=" + setsCurrent + ", setsUser=" + setsUser + ", timerState=" + timerState);
    }

    private void updateUserInterface() {
        timerProgressBar.setMax((int)timerUser);
        timerProgressBar.setProgress((int)timerCurrent);
        updateSetsDisplay();
        updateTimerDisplay();
        updateButtonsLayout();
    }

    private boolean timerServiceIsRunning(){
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        // Loop through the running services
        for(ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (TimerService.class.getName().equals(service.service.getClassName()))
                return true;
        }
        return false;
    }

    @Override
    public void onDialogMsSet(int reference, boolean isNegative, int minutes, int seconds) {
        long time = minutes * 60 + seconds;
        //getBaseContext().sendBroadcast(new Intent(IntentAction.SET_TIMER).putExtra("time", time));
        if(timerServiceBound)
            timerService.setTimer(time);
        timerCurrent = time;
        timerUser = time;
        Log.d(TAG, "onDialogMsSet: timerUser=" + time);
        timerProgressBar.setMax((int)timerUser);
        updateTimerDisplay();
        timerPickerDone = true;
        input();
    }

    @Override
    public void onDialogNumberSet(int reference, BigInteger number, double decimal, boolean isNegative, BigDecimal fullNumber) {
        int sets = number.intValue();
        //getBaseContext().sendBroadcast(new Intent(IntentAction.SET_SETS).putExtra("sets", sets));
        if(timerServiceBound)
            timerService.setSets(sets);
        setsCurrent = sets;
        setsUser = sets;
        Log.d(TAG, "onDialogNumberSet: setsUser=" + sets);
        updateSetsDisplay();
        setsPickerDone = true;
        input();
    }

    /**
     * Update the timer and sets TextView and Progress Bar
     * Use the timerCurrent and setsCurrent variables
     */
    @SuppressWarnings("deprecation")
    private void updateTimerDisplay() {
        String timeString = String.format(Locale.US, "%d:%02d", timerCurrent / 60, timerCurrent % 60);
        Log.d(TAG, "updateTimerDisplay: timeString='" + timeString + "'");
        timerTextView.setText(timeString);
        timerProgressBar.setMax((int)timerUser);
        timerProgressBar.setProgress((int)timerCurrent);
        int color;
        if(timerCurrent == 0)
            color = Color.GRAY;
        else if(timerCurrent <= timerGetReady && timerGetReadyEnable)
            color = Color.RED;
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            color = getColor(R.color.colorPrimary);
        else
            color = getResources().getColor(R.color.colorPrimary);
        timerTextView.setTextColor(color);
        timerProgressBar.setProgressTintList(ColorStateList.valueOf(color));
    }

    private void updateSetsDisplay() {
        String setsString = String.format(Locale.US, "x%d", setsCurrent);
        Log.d(TAG, "updateSetsDisplay: setsString='" + setsString + "'");
        setsNumbersTextView.setText(setsString);
    }

    /**
     * Handle the input timer and sets picker
     * Update the layout to READY when the input is complete (timer and sets)
     */
    protected void input() {
        Log.d(TAG, "input: timerPickerDone=" + timerPickerDone + ", setPickerDone=" + setsPickerDone);
        if(!timerPickerDone)
            timerPickerBuilder.show();
        else if(!setsPickerDone) {
            updateButtonsLayout(ButtonsLayout.WAITING_SETS);
            setsPickerBuilder.show();
        } else {
            timerState = TimerService.State.READY;
            if(timerServiceBound)
                timerService.setState(timerState);
            updateButtonsLayout();
        }
    }

    private void inputPreset(int position) {
        long timer = sharedPreferences.getLong(String.format(Locale.US, "presetArray_%d_timer", position), -1);
        int sets = sharedPreferences.getInt(String.format(Locale.US, "presetArray_%d_sets", position), -1);

        if(timer < 0 || timer < 0) {
            Log.e(TAG, "inputPreset : error in the saved values position=" + position + ", time=" + timer + ", sets=" + sets);
            return;
        }
        Log.d(TAG, "inputPreset: position=" + position + ", timerUser=" + timer + ", setsUser=" + sets);

        timerCurrent = timer;
        timerUser = timer;
        setsCurrent = sets;
        setsUser = sets;

        // Update READY UI
        timerProgressBar.setMax((int)timerUser);
        updateTimerDisplay();
        timerPickerDone = true;
        setsPickerDone = true;

        if(timerServiceBound) {
            timerService.setTimer(timer);
            timerService.setSets(sets);
            timerService.setState(timerState);
        }

        timerState = TimerService.State.READY;
        updateButtonsLayout();
    }

    private void updatePresetTextView(int position) {
        Log.d(TAG, "updatePresetTextView position=" + position);
        String presetString = "-";
        long time = sharedPreferences.getLong(String.format(Locale.US, "presetArray_%d_timer", position), -1);
        int sets = sharedPreferences.getInt(String.format(Locale.US, "presetArray_%d_sets", position), -1);
        if (time > 0 && sets > 0)
            presetString = String.format(Locale.US, "%d:%02d x%d", time / 60, time % 60, sets);
        switch (position) {
            case 0:
                presetLeftTextView.setText(presetString);
                break;
            case 1:
                presetCenterTextView.setText(presetString);
                break;
            case 2:
                presetRightTextView.setText(presetString);
                break;
        }
    }

    private void presetInput(int position) {
        Log.d(TAG, "presetInput position=" + position);
        if (buttonsLayout == ButtonsLayout.WAITING || buttonsLayout == ButtonsLayout.WAITING_SETS) {
            inputPreset(position);
        }
        else if(buttonsLayout == ButtonsLayout.READY || buttonsLayout == ButtonsLayout.STOPPED) {
            addPreset(position);
        }
        updatePresetButtons();
    }

    private void addPreset(int position) {
        Log.d(TAG, "addPreset: position=" + position + ", timerCurrent=" + timerCurrent + ", setsCurrent=" + setsCurrent);
        SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
        sharedPreferencesEditor.putInt(String.format(Locale.US, "presetArray_%d_sets", position), setsCurrent);
        sharedPreferencesEditor.putLong(String.format(Locale.US, "presetArray_%d_timer", position), timerCurrent);
        sharedPreferencesEditor.commit();
        updatePresetTextView(position);
    }

    private boolean presetAvailable(int position) {
        Log.d(TAG, "presetAvailable position=" + position);
        long time = sharedPreferences.getLong(String.format(Locale.US, "presetArray_%d_timer", position), -1);
        int sets = sharedPreferences.getInt(String.format(Locale.US, "presetArray_%d_sets", position), -1);
        return (time > 0 && sets > 0);
    }

    protected void start() {
        timerState = TimerService.State.RUNNING;
        Log.d(TAG, "start: timerState=" + timerState);
        updateButtonsLayout();
    }

    protected void pause() {
        timerState = TimerService.State.PAUSED;
        Log.d(TAG, "pause: timerState=" + timerState);
        updateButtonsLayout();
    }

    protected void resume() {
        timerState = TimerService.State.RUNNING;
        Log.d(TAG, "resume: timerState=" + timerState);
        updateButtonsLayout();
    }

    protected void stop() {
        timerState = TimerService.State.STOPPED;
        Log.d(TAG, "stop: timerState=" + timerState + ", setsCurrent=" + setsCurrent);
        if(setsCurrent > 1)
            updateButtonsLayout();
        else
            updateButtonsLayout(ButtonsLayout.READY);
    }

    protected void nextSet() {
        stop();
        // Going to nextSet on the last set is allowed from the notification
        if(setsCurrent-- > 1) {
            Log.d(TAG, "nextSet: setsCurrent=" + setsCurrent);
        }
        updateSetsDisplay();
    }

    protected void nextSetStart() {
        stop();
        // Going to nextSet on the last set is allowed from the notification
        if(setsCurrent-- > 1) {
            Log.d(TAG, "nextSetStart: setsCurrent=" + setsCurrent);
        }
        else {
            Log.e(TAG, "nextSetStart: setsCurrent=" + setsCurrent);
        }
        updateSetsDisplay();
        timerState = TimerService.State.RUNNING;
        Log.d(TAG, "nextSetStart: timerState=" + timerState);
        updateButtonsLayout();
    }

    protected void clear() {
        timerState = TimerService.State.WAITING;
        Log.d(TAG, "clear: timerState=" + timerState);
        updateButtonsLayout();
        timerUser = 0;
        setsUser = 1;
        timerPickerDone = false;
        setsPickerDone = false;
        updateTimerDisplay();
        updateSetsDisplay();
    }

    protected void reset() {
        timerState = TimerService.State.READY;
        Log.d(TAG, "reset: timerState=" + timerState);
        updateButtonsLayout();
    }

    protected void extraSet() {
        setsCurrent += 1;
        Log.d(TAG, "extraSet: setsCurrent=" + setsCurrent);
        updateSetsDisplay();
        updateButtonsLayout(ButtonsLayout.RUNNING);
    }

    protected void timerMinus() {
        if(timerCurrent < 0) {
            Log.e(TAG, "timerMinus: impossible timerCurrent=" + timerCurrent);
            return;
        }
        timerCurrent -= timerMinus;
        Log.d(TAG, "timerMinus: timerCurrent=" + timerCurrent);
        updateTimerDisplay();
        updateTimerButtons();
    }

    protected void timerPlus() {
        timerCurrent += timerPlus;
        Log.d(TAG, "timerPlus: timerCurrent=" + timerCurrent);
        updateTimerDisplay();
        updateTimerButtons();
    }

    protected void setsMinus() {
        if(setsCurrent <= 1) {
            Log.e(TAG, "sets minus setsCurrent=" + setsCurrent);
            return;
        }
        setsCurrent -= 1;
        Log.d(TAG, "setsMinus: setsCurrent=" + setsCurrent);
        updateSetsDisplay();
        updateSetsButtons();
    }

    protected void setsPlus() {
        setsCurrent += 1;
        Log.d(TAG, "setsPlus: setsCurrent=" + setsCurrent);
        updateSetsDisplay();
        updateSetsButtons();
    }

    protected void updateTimerState(TimerService.State state) {
        if(timerState != state) {
            timerState = state;
            Log.d(TAG, "updateTimerState: synchronising timerState=" + timerState);
            updateButtonsLayout();
        }
    }

    protected void setsUpdate(int sets) {
        Log.d(TAG, "setsUpdate: sets=" + sets + ", setsCurrent=" + setsCurrent);
        if(setsCurrent != sets) {
            setsCurrent = sets;
            updateSetsDisplay();
            updateSetsButtons();
        }
    }

    protected void timerUpdate(long time) {
        Log.d(TAG, "timerUpdate: time=" + time + ", timerCurrent=" + timerCurrent);
        if(timerCurrent != time) {
            timerCurrent = time;
            updateTimerDisplay();
            updateTimerButtons();
            if(time == timerGetReady && timerGetReadyEnable && mainActivityVisible) {
                if(vibrationReadyEnable)
                    vibrate();
                ring(ringtoneReady);
            }
        }
    }

    protected void done() {
        // The timer will be stopped from the alerts
        if(setsCurrent-- > 1) {
            Log.d(TAG, "done: setsCurrent=" + setsCurrent);
            if (mainActivityVisible) {
                vibrate();
                ring(ringtone);
                alertSetDone.show();

            }
            updateButtonsLayout(ButtonsLayout.STOPPED);
            updateSetsDisplay();
        }
        else {
            Log.d(TAG, "done: all sets done, setsCurrent=" + setsCurrent);
            if (mainActivityVisible) {
                vibrate();
                ring(ringtone);
                alertAllSetsDone.show();
            }
            updateButtonsLayout(ButtonsLayout.READY);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main_favorites, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        int requestCode = 1;

        if(id == R.id.preferences) {
            Log.d(TAG, "onOptionsItemSelected: item.id=settings");
            startActivityForResult(new Intent(this, PreferencesActivity.class), requestCode);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            return true;
        }
        else if(id == R.id.feedback) {
            Intent sendEmail = new Intent(Intent.ACTION_SEND);
            sendEmail.setType("text/email");
            sendEmail.putExtra(Intent.EXTRA_EMAIL, new String[] { "guillaume.militello@gmail.com" });
            sendEmail.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.app_name) + " feedback");
            startActivity(Intent.createChooser(sendEmail, "Send Feedback:"));
            return true;
        }
        else if(id == R.id.about) {
            Log.d(TAG, "onOptionsItemSelected: item.id=about");
            startActivityForResult(new Intent(this, AboutActivity.class), requestCode);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            return true;
        }
        else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 1){
            Log.d(TAG, "onActivityResult: resultCode=" + resultCode);
        }
    }

    private void vibrate() {
        if(vibrationEnable) {
            Log.i(TAG, "vibrate");
            Vibrator vibrator;
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(vibrationPattern, -1);
        }
    }

    private void ring(Uri notification) {
        Ringtone ringtone = RingtoneManager.getRingtone(getApplicationContext(), notification);
        ringtone.play();
        Log.i(TAG, "ring: notification=" + notification.toString());
    }

    private void updateSetsButtons() {
        // Also update the next set button
        if ((buttonsLayout == ButtonsLayout.RUNNING || buttonsLayout == ButtonsLayout.PAUSED) && setsCurrent > 1) {
            imageButtonSetsMinus.setEnabled(true);
            imageButtonSetsMinus.setAlpha(alphaEnabled);
            updateButton(imageButtonRight, ButtonAction.NEXT_SET_START);
        } else {
            imageButtonSetsMinus.setEnabled(false);
            imageButtonSetsMinus.setAlpha(alphaDisabled);
            updateButton(imageButtonRight, ButtonAction.NEXT_SET_START_DISABLED);
        }
        if(buttonsLayout == ButtonsLayout.RUNNING || buttonsLayout == ButtonsLayout.PAUSED) {
            imageButtonSetsPlus.setEnabled(true);
            imageButtonSetsPlus.setAlpha(alphaEnabled);
        } else {
            imageButtonSetsPlus.setEnabled(false);
            imageButtonSetsPlus.setAlpha(alphaDisabled);
        }
    }

    private void updateTimerButtons() {
        if((buttonsLayout == ButtonsLayout.RUNNING || buttonsLayout == ButtonsLayout.PAUSED) && timerCurrent >= timerMinus){
            imageButtonTimerMinus.setEnabled(true);
            imageButtonTimerMinus.setAlpha(alphaEnabled);
        } else {
            imageButtonTimerMinus.setEnabled(false);
            imageButtonTimerMinus.setAlpha(alphaDisabled);
        }
        if(buttonsLayout == ButtonsLayout.RUNNING || buttonsLayout == ButtonsLayout.PAUSED) {
            imageButtonTimerPlus.setEnabled(true);
            imageButtonTimerPlus.setAlpha(alphaEnabled);
        } else {
            imageButtonTimerPlus.setEnabled(false);
            imageButtonTimerPlus.setAlpha(alphaDisabled);
        }
    }

    private void updatePresetButtons() {
        int ic_add = R.drawable.ic_add_circle_black_48dp;
        int ic_play = R.drawable.ic_play_circle_filled_black_48dp;
        imageButtonPresetLeft.setImageResource(ic_add);
        imageButtonPresetCenter.setImageResource(ic_add);
        imageButtonPresetRight.setImageResource(ic_add);
        if(buttonsLayout == ButtonsLayout.READY) {
            imageButtonPresetLeft.setEnabled(true);
            imageButtonPresetLeft.setAlpha(alphaEnabled);
            imageButtonPresetCenter.setEnabled(true);
            imageButtonPresetCenter.setAlpha(alphaEnabled);
            imageButtonPresetRight.setEnabled(true);
            imageButtonPresetRight.setAlpha(alphaEnabled);
        }
        else if(buttonsLayout == ButtonsLayout.WAITING || buttonsLayout == ButtonsLayout.WAITING_SETS) {
            boolean enable = presetAvailable(0);
            imageButtonPresetLeft.setImageResource(enable? ic_play : ic_add);
            imageButtonPresetLeft.setEnabled(enable);
            imageButtonPresetLeft.setAlpha(enable? alphaEnabled : alphaDisabled);
            enable = presetAvailable(1);
            imageButtonPresetCenter.setImageResource(enable? ic_play : ic_add);
            imageButtonPresetCenter.setEnabled(enable);
            imageButtonPresetCenter.setAlpha(enable? alphaEnabled : alphaDisabled);
            enable = presetAvailable(2);
            imageButtonPresetRight.setImageResource(enable? ic_play : ic_add);
            imageButtonPresetRight.setEnabled(enable);
            imageButtonPresetRight.setAlpha(enable? alphaEnabled : alphaDisabled);
        }
        else {
            imageButtonPresetLeft.setEnabled(false);
            imageButtonPresetLeft.setAlpha(alphaDisabled);
            imageButtonPresetCenter.setEnabled(false);
            imageButtonPresetCenter.setAlpha(alphaDisabled);
            imageButtonPresetRight.setEnabled(false);
            imageButtonPresetRight.setAlpha(alphaDisabled);
        }
        updatePresetTextView(0);
        updatePresetTextView(1);
        updatePresetTextView(2);
    }

    private void updateButtonsLayout() {
        ButtonsLayout layout = ButtonsLayout.valueOf(timerState.toString().toUpperCase(Locale.US));
        updateButtonsLayout(layout);
    }

    private void updateButtonsLayout(ButtonsLayout layout) {
        if(buttonsLayout != layout) {
            ButtonAction nextStep;
            switch (layout) {
                case WAITING:
                    updateButtons(ButtonAction.CLEAR_DISABLED, ButtonAction.INPUT, ButtonAction.NEXT_SET_START_DISABLED);
                    break;
                case WAITING_SETS:
                    updateButtons(ButtonAction.CLEAR, ButtonAction.INPUT, ButtonAction.NEXT_SET_START_DISABLED);
                    break;
                case READY:
                    updateButtons(ButtonAction.CLEAR, ButtonAction.START, ButtonAction.NEXT_SET_START_DISABLED);
                    break;
                case RUNNING:
                    nextStep = (setsCurrent > 1)? ButtonAction.NEXT_SET_START : ButtonAction.NEXT_SET_START_DISABLED;
                    updateButtons(ButtonAction.RESET, ButtonAction.PAUSE, nextStep);
                    break;
                case PAUSED:
                    nextStep = (setsCurrent > 1)? ButtonAction.NEXT_SET_START : ButtonAction.NEXT_SET_START_DISABLED;
                    updateButtons(ButtonAction.RESET, ButtonAction.RESUME, nextStep);
                    break;
                case STOPPED:
                    updateButtons(ButtonAction.RESET, ButtonAction.START, ButtonAction.NEXT_SET_START_DISABLED);
                    break;
                default:
                    Log.e(TAG, "updateButtonsLayout: impossible layout=" + layout.toString());
            }
            buttonsLayout = layout;
            Log.d(TAG, "updateButtonsLayout: buttonsLayout=" + buttonsLayout.toString());
        }
        updateSetsButtons();
        updateTimerButtons();
        updatePresetButtons();
    }

    private void updateButtons(ButtonAction left, ButtonAction center, ButtonAction right) {

        if(left != buttonLeftAction && updateButton(imageButtonLeft, left))
            buttonLeftAction = left;
        if(center != buttonCenterAction && updateButton(imageButtonCenter, center))
            buttonCenterAction = center;
        if(right != buttonRightAction && updateButton(imageButtonRight, right))
            buttonRightAction = right;
        Log.d(TAG, "updateButtons: left=" + buttonLeftAction.toString() +
                ", center=" + buttonCenterAction.toString() + ", right=" + buttonRightAction.toString());
    }

    private boolean updateButton(ImageButton button, ButtonAction action) {
        switch (action) {
            case NO_ACTION:
                button.setEnabled(false);
                return true;
            case INPUT:
                button.setEnabled(true);
                button.setImageResource(R.drawable.ic_add_circle_black_48dp);
                button.setAlpha(alphaEnabled);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) { input(); }
                });
                return true;
            case START:
                button.setEnabled(true);
                button.setImageResource(R.drawable.ic_play_circle_filled_black_48dp);
                button.setAlpha(alphaEnabled);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) { sendBroadcast(new Intent(IntentAction.START)); }
                });
                return true;
            case PAUSE:
                button.setEnabled(true);
                button.setImageResource(R.drawable.ic_pause_circle_filled_black_48dp);
                button.setAlpha(alphaEnabled);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) { sendBroadcast(new Intent(IntentAction.PAUSE)); }
                });
                return true;
            case RESUME:
                button.setEnabled(true);
                button.setImageResource(R.drawable.ic_play_circle_filled_black_48dp);
                button.setAlpha(alphaEnabled);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) { sendBroadcast(new Intent(IntentAction.RESUME)); }
                });
                return true;
            case CLEAR:
                button.setEnabled(true);
                button.setImageResource(R.drawable.ic_delete_black_48dp);
                button.setAlpha(alphaEnabled);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) { sendBroadcast(new Intent(IntentAction.CLEAR)); }
                });
                return true;
            case CLEAR_DISABLED:
                button.setEnabled(false);
                button.setImageResource(R.drawable.ic_delete_black_48dp);
                button.setAlpha(alphaDisabled);
                return true;
            case NEXT_SET_START:
                button.setEnabled(true);
                button.setImageResource(R.drawable.ic_chevron_right_double_black_48dp);
                button.setAlpha(alphaEnabled);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) { sendBroadcast(new Intent(IntentAction.NEXT_SET_START)); }
                });
                return true;
            case NEXT_SET_START_DISABLED:
                button.setEnabled(false);
                button.setImageResource(R.drawable.ic_chevron_right_double_black_48dp);
                button.setAlpha(alphaDisabled);
                return true;
            case RESET:
                button.setEnabled(true);
                button.setImageResource(R.drawable.ic_refresh_black_48dp);
                button.setAlpha(alphaEnabled);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) { sendBroadcast(new Intent(IntentAction.RESET)); }
                });
                return true;
            case RESET_DISABLED:
                button.setEnabled(false);
                button.setImageResource(R.drawable.ic_refresh_black_48dp);
                button.setAlpha(alphaDisabled);
                return true;
            default:
                Log.e(TAG, "updateButton: impossible with action=" + action);
        }
        return false;
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.simpleworkout.timer/http/host/path")
                );
        AppIndex.AppIndexApi.end(client, viewAction);
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.disconnect();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        mainActivityVisible = false;

        if(timerServiceBound) {
            timerService.setMainActivityVisible(false);
            timerService.updateNotificationVisibility(true);
        }

        // Complete ongoing pop-up action
        if(alertSetDone.isShowing()) {
            sendBroadcast(new Intent(new Intent(IntentAction.STOP)));
            alertSetDone.dismiss();
        }
        else if(alertAllSetsDone.isShowing()) {
            sendBroadcast(new Intent(new Intent(IntentAction.RESET)));
            alertAllSetsDone.dismiss();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        mainActivityVisible = true;
        updateUserInterface();
        if(timerServiceBound) {
            timerService.setMainActivityVisible(true);
            timerService.updateNotificationVisibility(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        if(timerServiceBound)
            timerService.updateNotificationVisibility(false);
        unregisterReceiver(mainActivityReceiver);

        if (timerServiceBound) {
            unbindService(serviceConnection);
            timerServiceBound = false;
        }
        // Don't stop the service on WIN DEATH
        //stopService(new Intent(getBaseContext(), TimerService.class));
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            timerServiceBound = false;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            TimerBinder binder = (TimerBinder) service;
            timerService = binder.getService();
            timerServiceBound = true;
            Log.d(TAG, "onServiceConnected");
            getTimerServiceContext();
            updateUserInterface();
            updateAllPreferences();
        }
    };

    private void updateAllPreferences() {
        Log.d(TAG, "updateSummaries");
        Map<String, ?> preferences = sharedPreferences.getAll();
        if (preferences != null) {
            for (Map.Entry<String, ?> preference : preferences.entrySet()) {
                updatePreference(preference.getKey());
            }
        }
    }

    private void updatePreference(String key) {
        Log.d(TAG, "updatePreference: key=" + key);
        String color, uri, title;

        switch(key) {
            case "timerMinus":
                timerMinus = Long.parseLong(sharedPreferences.getString("timerMinus", "30"));
                if(timerServiceBound)
                    timerService.setTimerMinus(timerMinus);
                Log.d(TAG, "updatePreference: timerMinus=" + timerMinus);
                break;
            case "timerPlus":
                timerPlus = Long.parseLong(sharedPreferences.getString("timerPlus", "30"));
                if(timerServiceBound)
                    timerService.setTimerPlus(timerPlus);
                Log.d(TAG, "updatePreference: timerPlus=" + timerPlus);
                break;
            case "vibrationEnable":
                vibrationEnable = sharedPreferences.getBoolean("vibrationEnable", true);
                if(timerServiceBound)
                    timerService.interactiveNotification.setVibrationEnable(vibrationEnable);
                Log.d(TAG, "updatePreference: vibrationEnable=" + vibrationEnable);
                break;
            case "vibrationReadyEnable":
                vibrationReadyEnable = sharedPreferences.getBoolean("vibrationReadyEnable", true);
                if(timerServiceBound)
                    timerService.interactiveNotification.setVibrationReadyEnable(vibrationReadyEnable);
                Log.d(TAG, "updatePreference: vibrationReadyEnable=" + vibrationReadyEnable);
                break;
            case "timerGetReadyEnable":
                timerGetReadyEnable = sharedPreferences.getBoolean("timerGetReadyEnable", true);
                if(timerServiceBound)
                    timerService.setTimerGetReadyEnable(timerGetReadyEnable);
                Log.d(TAG, "updatePreference: timerGetReadyEnable=" + timerGetReadyEnable);
                break;
            case "timerGetReady":
                if(timerGetReadyEnable) {
                    timerGetReady = Integer.parseInt(sharedPreferences.getString("timerGetReady", "15"));
                    if(timerServiceBound)
                        timerService.setTimerGetReady(timerGetReady);
                    Log.d(TAG, "updatePreference: timerGetReady=" + timerGetReady);
                }
                else {
                    timerGetReady = -1;
                    if(timerServiceBound)
                        timerService.setTimerGetReady(timerGetReady);
                    Log.d(TAG, "updatePreference: timerGetReady=" + timerGetReady);
                }
                break;
            case "lightColor":
                int lightColor;
                color = sharedPreferences.getString("lightColor", "green");
                Log.d(TAG, "updatePreference: color=" + color);
                switch (color) {
                    case "none":
                        lightColor = InteractiveNotification.COLOR_NONE;
                        break;
                    case "default":
                        lightColor = InteractiveNotification.COLOR_DEFAULT;
                        break;
                    default:
                        lightColor = Color.parseColor(color);
                        break;
                }
                if(timerServiceBound)
                    timerService.interactiveNotification.setLightColor(lightColor);
                Log.d(TAG, "updatePreference: lightColor=" + color);
                break;
            case "lightReadyColor":
                int lightReadyColor;
                color = sharedPreferences.getString("lightReadyColor", "yellow");
                Log.d(TAG, "updatePreference: color=" + color);
                switch (color) {
                    case "none":
                        lightReadyColor = InteractiveNotification.COLOR_NONE;
                        break;
                    case "default":
                        lightReadyColor = InteractiveNotification.COLOR_DEFAULT;
                        break;
                    default:
                        lightReadyColor = Color.parseColor(color);
                        break;
                }
                if(timerServiceBound)
                    timerService.interactiveNotification.setLightReadyColor(lightReadyColor);
                Log.d(TAG, "updatePreference: lightReadyColor=" + color);
                break;
            case "ringtoneUri":
                uri = sharedPreferences.getString("ringtoneUri", "default");
                ringtone = Uri.parse(uri);
                if(timerServiceBound)
                    timerService.interactiveNotification.setRingtone(ringtone);
                title = RingtoneManager.getRingtone(this, ringtone).getTitle(this);
                Log.d(TAG, "updatePreference: ringtone=" + title);
                break;
            case "ringtoneUriReady":
                uri = sharedPreferences.getString("ringtoneUriReady", "default");
                ringtoneReady = Uri.parse(uri);
                if(timerServiceBound)
                    timerService.interactiveNotification.setRingtoneReady(ringtoneReady);
                title = RingtoneManager.getRingtone(this, ringtoneReady).getTitle(this);
                Log.d(TAG, "updatePreference: ringtone=" + title);
                break;
            default:
                Log.e(TAG, "updateSetting: not supported preference key=" + key);
        }
    }

    SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                    // avoid checking for preset timer keys
                    if(!key.contains("presetArray_")) {
                        Log.d(TAG, "SharedPreferenceChanged: key=" + key);
                        updatePreference(key);
                    }
                }
            };
}
