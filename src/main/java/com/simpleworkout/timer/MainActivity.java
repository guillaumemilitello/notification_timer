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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
import java.util.ArrayList;
import java.util.List;
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
    private static final float ALPHA_ENABLED = (float) 1.0;
    private static final float ALPHA_DISABLED = (float) 0.3;

    // Sets number for infinity
    public static final int SETS_INFINITY = 999999;

    // Main user interface
    private TextView timerTextView, setsTextView, timerUserTextView, setsUserTextView;
    private ProgressBar timerProgressBar, timerReadyProgressBar;
    private ButtonsLayout buttonsLayout;
    private ButtonAction buttonLeftAction, buttonCenterAction, buttonRightAction;
    private ImageButton imageButtonLeft, imageButtonCenter, imageButtonRight;
    private ImageButton imageButtonTimerMinus, imageButtonTimerPlus, imageButtonSetsMinus, imageButtonSetsPlus;

    // Timer and Sets Pickers
    private MsPickerBuilder timerPickerBuilder;
    private NumberPickerBuilder setsPickerBuilder;
    private boolean timerPickerDone, setsPickerDone;

    // Presets Timers
    private PresetSpinner spinnerPresets;
    private List<String> spinnerPresetsArray;
    private ImageButton imageButtonPresets;
    private boolean spinnerUserInteraction;

    // Timer service related
    private TimerService.State timerState;
    private long timerCurrent;
    private long timerUser;
    private int setsInit;
    private int setsCurrent;
    private int setsUser;

    // Settings
    protected static final long[] vibrationPattern = { 0, 400, 200, 400, };

    // User preferences
    private boolean timerGetReadyEnable = true;
    private int timerGetReady = 15;
    private long timerMinus = 30;
    private long timerPlus = 30;
    private static boolean initPickerZero = false;
    private boolean vibrationEnable = false;
    private boolean vibrationReadyEnable = false;
    private Uri ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    private Uri ringtoneReady = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

    public static boolean getInitPickerZero() { return initPickerZero; }

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
        Log.d(TAG, "onCreate");

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
        setsTextView = (TextView) findViewById(R.id.textViewSets);
        timerUserTextView = (TextView) findViewById(R.id.textViewInfoTimer);
        setsUserTextView = (TextView) findViewById(R.id.textViewInfoSets);

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
        setsPickerBuilder.setPlusMinusVisibility(View.VISIBLE);
        setsPickerBuilder.setCheckboxLabelText(getString(R.string.picker_checkbox));

        timerProgressBar = (ProgressBar) findViewById(R.id.timerProgressBar);
        timerReadyProgressBar = (ProgressBar) findViewById(R.id.timerReadyProgressBar);

        imageButtonLeft = (ImageButton) findViewById(R.id.imageButtonLeft);
        imageButtonCenter = (ImageButton) findViewById(R.id.imageButtonCenter);
        imageButtonRight = (ImageButton) findViewById(R.id.imageButtonRight);

        spinnerPresetsArray =  new ArrayList<String>();
        spinnerPresets = (PresetSpinner) findViewById(R.id.spinnerPresets);
        imageButtonPresets = (ImageButton) findViewById(R.id.imageButtonPresets);
        spinnerUserInteraction = false;
        spinnerPresets.setOnItemSelectedEvenIfUnchangedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                Log.d(TAG, "spinnerPresets.onItemSelected: position=" + position + ", spinnerUserInteraction=" + spinnerUserInteraction);
                if (spinnerUserInteraction) {
                    inputPreset(position);
                }
                else {
                    spinnerUserInteraction = true;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                Log.d(TAG, "spinnerPresets.onNothingSelected");
            }
        });

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

        timerPickerDone = false;
        setsPickerDone = false;

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
        filter.addAction(IntentAction.TIMER_REBIND);
        filter.addAction(IntentAction.TIMER_STATE);
        filter.addAction(IntentAction.TIMER_UPDATE);
        filter.addAction(IntentAction.TIMER_DONE);
        filter.addAction(IntentAction.NOTIFICATION_DISMISS);
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

        updatePresetsArray();

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
                Action.TYPE_VIEW,
                "Main Page",
                Uri.parse("http://host/path"),
                Uri.parse("android-app://com.simpleworkout.timer/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);

        mainActivityVisible = true;

        timerCurrent = 0;
        timerUser = 0;
        setsInit = 0;
        setsCurrent = 0;
        setsUser = 0;
        timerState = TimerService.State.WAITING;

        if(timerServiceBound) {
            getTimerServiceContext();
            updateUserInterface();
        }
    }

    private void getTimerServiceContext() {
        timerCurrent = timerService.getTimerCurrent();
        timerUser = timerService.getTimerUser();
        setsInit = timerService.getSetsInit();
        setsCurrent = timerService.getSetsCurrent();
        setsUser = timerService.getSetsUser();
        timerState = timerService.getState();
        Log.d(TAG, "updateUserInterface: timerCurrent=" + timerCurrent + ", timerUser=" + timerUser +
                ", setsCurrent=" + setsCurrent + ", setsInit=" + setsInit + ", setsUser=" + setsUser + ", timerState=" + timerState);
    }

    private void updateUserInterface() {
        if(timerServiceBound) {
            timerService.setMainActivityVisible(mainActivityVisible);
            timerService.updateNotificationVisibility(!mainActivityVisible);
        }
        timerProgressBar.setMax((int)timerUser);
        timerProgressBar.setProgress((int)timerCurrent);
        timerReadyProgressBar.setMax((int)timerUser);
        timerReadyProgressBar.setProgress(timerGetReadyEnable? timerGetReady : 0);
        updateSetsDisplay();
        updateTimerDisplay();
        updateTimerUserDisplay();
        updateSetsUserDisplay();
        updateButtonsLayout();
    }

    private boolean timerServiceIsRunning() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        // Loop through the running services
        for(ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (TimerService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    protected void timerServiceRebind() {
        Log.d(TAG, "timerServiceRebind");
        timerServiceBound = true;
        // Rebind occurs only when relaunching the mainActivity
        timerService.setMainActivityVisible(true);
        timerService.updateNotificationVisibility(false);
        getTimerServiceContext();
        updateUserInterface();
    }

    @Override
    public void onDialogMsSet(int reference, boolean isNegative, int minutes, int seconds) {
        long timer = minutes * 60 + seconds;
        timerCurrent = timer;
        timerUser = timer;
        Log.d(TAG, "onDialogMsSet: timerUser=" + timerUser);
        timerProgressBar.setMax((int)timerUser);
        timerReadyProgressBar.setMax((int)timerUser);
        updateTimerDisplay();
        timerPickerDone = true;
        inputPickers();
    }

    @Override
    public void onDialogNumberSet(int reference, BigInteger number, double decimal, boolean isNegative, BigDecimal fullNumber, boolean checked) {
        int sets = number.intValue();
        setsInit = checked? 0 : 1;
        setsCurrent = setsInit;
        setsUser = sets;
        Log.d(TAG, "onDialogNumberSet: setsUser=" + setsUser + ", setsCurrent=" + setsCurrent);
        updateSetsDisplay();
        setsPickerDone = true;
        inputPickers();
    }

    @SuppressWarnings("deprecation")
    private void updateTimerDisplay() {
        String timeString = String.format(Locale.US, "%d:%02d", timerCurrent / 60, timerCurrent % 60);
        Log.d(TAG, "updateTimerDisplay: timeString='" + timeString + "'");
        timerTextView.setText(timeString);
        timerProgressBar.setMax((int)timerUser);
        timerProgressBar.setProgress((int)timerCurrent);
        timerReadyProgressBar.setMax((int)timerUser);
        if(!timerGetReadyEnable || timerCurrent <= timerGetReady) {
            timerReadyProgressBar.setProgress(0);
        }
        else {
            timerReadyProgressBar.setProgress(timerGetReady);
        }

        int color;
        if(timerCurrent == 0) {
            color = Color.GRAY;
        }
        else if(timerGetReadyEnable && timerCurrent <= timerGetReady) {
            color = Color.RED;
        }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            color = getColor(R.color.colorPrimary);
        }
        else {
            color = getResources().getColor(R.color.colorPrimary);
        }
        timerTextView.setTextColor(color);
        timerProgressBar.setProgressTintList(ColorStateList.valueOf(color));
    }

    private void updateSetsDisplay() {
        String setsString = String.format(Locale.US, "x%d", setsCurrent);
        Log.d(TAG, "updateSetsDisplay: setsString='" + setsString + "'");
        setsTextView.setText(setsString);
    }

    private void updateTimerUserDisplay() {
        String timerInfoString = "-";
        if (timerUser > 0) {
            timerInfoString = String.format(Locale.US, "%d:%02d", timerCurrent / 60, timerCurrent % 60);
        }
        Log.d(TAG, "updateTimerInfo: timerInfoString='" + timerInfoString + "'");
        timerUserTextView.setText(timerInfoString);
    }

    private void updateSetsUserDisplay() {
        String setsInfoString = "-";
        if (setsUser == SETS_INFINITY) {
            setsInfoString = String.format(Locale.US, "%d→∞", setsInit);
        }
        else if (setsUser > 0) {
            setsInfoString = String.format(Locale.US, "%d→%d", setsInit, setsUser);
        }
        Log.d(TAG, "updateSetsInfo: setsInfoString='" + setsInfoString + "'");
        setsUserTextView.setText(setsInfoString);
    }

    private boolean inputFromPickers() {
        return timerPickerDone && setsPickerDone;
    }

    protected void inputPickers() {
        Log.d(TAG, "inputPickers: timerPickerDone=" + timerPickerDone + ", setPickerDone=" + setsPickerDone);
        if(!timerPickerDone) {
            timerPickerBuilder.show();
        }
        else if(!setsPickerDone) {
            updateButtonsLayout(ButtonsLayout.WAITING_SETS);
            setsPickerBuilder.show();
        } else {
            timerState = TimerService.State.READY;
            updateInputTimerService();
            updateButtonsLayout();
            updateTimerUserDisplay();
            updateSetsUserDisplay();
        }
    }

    private void inputPreset(int position) {
        long timer = sharedPreferences.getLong(String.format(Locale.US, "presetArray_%d_timer", position), -1);
        int sets = sharedPreferences.getInt(String.format(Locale.US, "presetArray_%d_sets", position), -1);
        int init = sharedPreferences.getInt(String.format(Locale.US, "presetArray_%d_init", position), -1);

        Log.d(TAG, "inputPreset: position=" + position + ", timerUser=" + timer + ", setsUser=" + sets + ", setsInit=" + init);

        timerCurrent = timer;
        timerUser = timer;
        setsInit = init;
        setsCurrent = init;
        setsUser = sets;

        timerProgressBar.setMax((int)timerUser);
        timerReadyProgressBar.setMax((int) timerUser);
        updateTimerDisplay();
        updateSetsDisplay();

        timerPickerDone = false;
        setsPickerDone = false;

        timerState = TimerService.State.READY;
        updateInputTimerService();
        updateButtonsLayout();
        updateTimerUserDisplay();
        updateSetsUserDisplay();
    }

    private void updateInputTimerService() {
        if(timerServiceBound) {
            timerService.setState(timerState);
            timerService.setTimerCurrent(timerCurrent);
            timerService.setTimerUser(timerUser);
            timerService.setSetsInit(setsInit);
            timerService.setSetsCurrent(setsCurrent);
            timerService.setSetsUser(setsUser);
        }
    }

    private void updatePresetsArray() {
        while (true) {
            int position = spinnerPresetsArray.size();
            long timer = sharedPreferences.getLong(String.format(Locale.US, "presetArray_%d_timer", position), -1);
            int sets = sharedPreferences.getInt(String.format(Locale.US, "presetArray_%d_sets", position), -1);
            int init = sharedPreferences.getInt(String.format(Locale.US, "presetArray_%d_init", position), -1);
            if (timer < 0 || sets < 0 || (init != 0 && init != 1)) {
                break;
            }
            addPresetArray(timer, sets, init);
        }
    }

    private void addPresetArray(long timer, int sets, int init) {
        String presetString = "-";
        if(timer > 0 && sets > 0) {
            if (sets == SETS_INFINITY) {
                presetString = String.format(Locale.US, "%d:%02d x∞(%d)", timer / 60, timer % 60, init);
            }
            else {
                presetString = String.format(Locale.US, "%d:%02d x%d(%d)", timer / 60, timer % 60, sets, init);
            }
        }
        spinnerPresetsArray.add(presetString);
        Log.d(TAG, "addPresetArray: spinnerPresetsArray=" + spinnerPresetsArray.toString());
        updatePresetsSpinner();
        spinnerPresets.setSelection(spinnerPresetsArray.size() - 1);
    }

    private void deletePresetArray(int position) {
        spinnerPresetsArray.remove(position);
        Log.d(TAG, "addPresetArray: spinnerPresetsArray=" + spinnerPresetsArray.toString());
        SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
        for (int i = position; i < spinnerPresetsArray.size(); ++i) {
            long timer = sharedPreferences.getLong(String.format(Locale.US, "presetArray_%d_timer", i + 1), -1);
            int sets = sharedPreferences.getInt(String.format(Locale.US, "presetArray_%d_sets", i + 1), -1);
            int init = sharedPreferences.getInt(String.format(Locale.US, "presetArray_%d_init", i + 1), -1);
            sharedPreferencesEditor.putLong(String.format(Locale.US, "presetArray_%d_timer", i), timer);
            sharedPreferencesEditor.putInt(String.format(Locale.US, "presetArray_%d_sets", i), sets);
            sharedPreferencesEditor.putInt(String.format(Locale.US, "presetArray_%d_init", i), init);
        }
        sharedPreferencesEditor.apply();
        updatePresetsSpinner();
    }

    private void updatePresetsSpinner() {
        Log.d(TAG, "updatePresetsSpinner: spinnerUserInteraction=false");
        spinnerUserInteraction = false;
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, spinnerPresetsArray);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPresets.setAdapter(adapter);
    }

    private void addPreset() {
        int position = spinnerPresetsArray.size();
        Log.d(TAG, "addPreset: position=" + position + ", timerUser=" + timerUser + ", setsUser=" + setsUser + ", setsInit=" + setsInit);
        SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
        // TODO: keys to strings resources
        sharedPreferencesEditor.putLong(String.format(Locale.US, "presetArray_%d_timer", position), timerUser);
        sharedPreferencesEditor.putInt(String.format(Locale.US, "presetArray_%d_sets", position), setsUser);
        sharedPreferencesEditor.putInt(String.format(Locale.US, "presetArray_%d_init", position), setsInit);
        sharedPreferencesEditor.apply();
        addPresetArray(timerUser, setsUser, setsInit);
        updatePresetsButtonDelete(true);
    }

    private void deletePreset() {
        int position = spinnerPresets.getSelectedItemPosition();
        Log.d(TAG, "deletePreset: position=" + position);
        SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
        // TODO: keys to strings resources
        sharedPreferencesEditor.putLong(String.format(Locale.US, "presetArray_%d_timer", position), -1);
        sharedPreferencesEditor.putInt(String.format(Locale.US, "presetArray_%d_sets", position), -1);
        sharedPreferencesEditor.putInt(String.format(Locale.US, "presetArray_%d_init", position), -1);
        sharedPreferencesEditor.apply();
        deletePresetArray(position);
        updatePresetsButtonAdd(true);
    }

    protected void start() {
        timerState = TimerService.State.RUNNING;
        Log.d(TAG, "start: timerState=" + timerState);
        // Give the user the possibility to save a timer only once
        timerPickerDone = false;
        setsPickerDone = false;
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
        if(setsCurrent > 1) {
            updateButtonsLayout();
        }
        else {
            updateButtonsLayout(ButtonsLayout.READY);
        }
    }

    protected void nextSet() {
        stop();
        // Going to nextSet on the last set is allowed from the notification
        if(++setsCurrent < setsUser) {
            Log.d(TAG, "nextSet: setsCurrent=" + setsCurrent);
        }
        updateSetsDisplay();
    }

    protected void nextSetStart() {
        stop();
        // Going to nextSet on the last set is allowed from the notification
        if(++setsCurrent < setsUser) {
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

        timerUser = 0;
        setsInit = 0;
        setsCurrent = 0;
        setsUser = 0;

        timerPickerDone = false;
        setsPickerDone = false;

        updateButtonsLayout();
        updateTimerUserDisplay();
        updateSetsUserDisplay();
    }

    protected void reset() {
        timerState = TimerService.State.READY;
        Log.d(TAG, "reset: timerState=" + timerState);

        updateButtonsLayout();
    }

    protected void extraSet() {
        Log.d(TAG, "extraSet: setsCurrent=" + setsCurrent);

        updateButtonsLayout(ButtonsLayout.RUNNING);
    }

    protected void timerMinus() {
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
                ring(ringtoneReady);
                vibrate();
            }
        }
    }

    protected void done() {
        // The timer will be stopped from the alerts
        if(++setsCurrent < setsUser) {
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
        // TODO: clean up
        // Also update the next set button
//        if ((buttonsLayout == ButtonsLayout.RUNNING || buttonsLayout == ButtonsLayout.PAUSED) && setsCurrent > 1) {
//            imageButtonSetsMinus.setEnabled(true);
//            imageButtonSetsMinus.setAlpha(ALPHA_ENABLED);
//            updateButton(imageButtonRight, ButtonAction.NEXT_SET_START);
//        } else {
            imageButtonSetsMinus.setEnabled(false);
            imageButtonSetsMinus.setAlpha(ALPHA_DISABLED);
            updateButton(imageButtonRight, ButtonAction.NEXT_SET_START_DISABLED);
//        }
//        if(buttonsLayout == ButtonsLayout.RUNNING || buttonsLayout == ButtonsLayout.PAUSED && setsCurrent < setsUser - 1) {
//            imageButtonSetsPlus.setEnabled(true);
//            imageButtonSetsPlus.setAlpha(ALPHA_ENABLED);
//        } else {
            imageButtonSetsPlus.setEnabled(false);
            imageButtonSetsPlus.setAlpha(ALPHA_DISABLED);
//        }
        if((buttonsLayout == ButtonsLayout.RUNNING || buttonsLayout == ButtonsLayout.PAUSED) && setsCurrent < setsUser - 1) {
            updateButton(imageButtonRight, ButtonAction.NEXT_SET_START);
        }
        else {
            // TODO: add button reset
            updateButton(imageButtonRight, ButtonAction.NEXT_SET_START_DISABLED);
        }
    }

    private void updateTimerButtons() {
        if((buttonsLayout == ButtonsLayout.RUNNING || buttonsLayout == ButtonsLayout.PAUSED) && timerCurrent >= timerMinus){
            imageButtonTimerMinus.setEnabled(true);
            imageButtonTimerMinus.setAlpha(ALPHA_ENABLED);
        } else {
            imageButtonTimerMinus.setEnabled(false);
            imageButtonTimerMinus.setAlpha(ALPHA_DISABLED);
        }
        if(buttonsLayout == ButtonsLayout.RUNNING || buttonsLayout == ButtonsLayout.PAUSED) {
            imageButtonTimerPlus.setEnabled(true);
            imageButtonTimerPlus.setAlpha(ALPHA_ENABLED);
        } else {
            imageButtonTimerPlus.setEnabled(false);
            imageButtonTimerPlus.setAlpha(ALPHA_DISABLED);
        }
    }

    private void updatePresetsButton() {
        Log.d(TAG, "updatePresetsButton: state=" + timerState + ", inputFromPickers=" + inputFromPickers());
        if(buttonsLayout == ButtonsLayout.WAITING || buttonsLayout == ButtonsLayout.WAITING_SETS) {
            updatePresetsButtonAdd(false);
            spinnerPresets.setEnabled(true);
        }
        else if(buttonsLayout == ButtonsLayout.READY) {
            if(inputFromPickers()) {
                updatePresetsButtonAdd(inputFromPickers());
            }
            else {
                updatePresetsButtonDelete(true);
            }
            spinnerPresets.setEnabled(true);
        }
        else {
            updatePresetsButtonAdd(false);
            spinnerPresets.setEnabled(false);
        }
    }

    private void updatePresetsButtonAdd(boolean enable) {
        imageButtonPresets.setImageResource(R.drawable.ic_add_circle_black_48dp);
        imageButtonPresets.setEnabled(enable);
        imageButtonPresets.setAlpha(enable? ALPHA_ENABLED : ALPHA_DISABLED);
        imageButtonPresets.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { addPreset(); }
        });
    }

    private void updatePresetsButtonDelete(boolean enable) {
        imageButtonPresets.setImageResource(R.drawable.ic_delete_black_48dp);
        imageButtonPresets.setEnabled(enable);
        imageButtonPresets.setAlpha(enable? ALPHA_ENABLED : ALPHA_DISABLED);
        imageButtonPresets.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { deletePreset(); }
        });
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
                    nextStep = (setsCurrent < setsUser - 1)? ButtonAction.NEXT_SET_START : ButtonAction.NEXT_SET_START_DISABLED;
                    updateButtons(ButtonAction.RESET, ButtonAction.PAUSE, nextStep);
                    break;
                case PAUSED:
                    nextStep = (setsCurrent < setsUser - 1)? ButtonAction.NEXT_SET_START : ButtonAction.NEXT_SET_START_DISABLED;
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
        updatePresetsButton();
    }

    private void updateButtons(ButtonAction left, ButtonAction center, ButtonAction right) {

        if(left != buttonLeftAction && updateButton(imageButtonLeft, left)) {
            buttonLeftAction = left;
        }
        if(center != buttonCenterAction && updateButton(imageButtonCenter, center)) {
            buttonCenterAction = center;
        }
        if(right != buttonRightAction && updateButton(imageButtonRight, right)) {
            buttonRightAction = right;
        }
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
                button.setAlpha(ALPHA_ENABLED);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) { inputPickers(); }
                });
                return true;
            case START:
                button.setEnabled(true);
                button.setImageResource(R.drawable.ic_play_circle_filled_black_48dp);
                button.setAlpha(ALPHA_ENABLED);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) { sendBroadcast(new Intent(IntentAction.START)); }
                });
                return true;
            case PAUSE:
                button.setEnabled(true);
                button.setImageResource(R.drawable.ic_pause_circle_filled_black_48dp);
                button.setAlpha(ALPHA_ENABLED);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) { sendBroadcast(new Intent(IntentAction.PAUSE)); }
                });
                return true;
            case RESUME:
                button.setEnabled(true);
                button.setImageResource(R.drawable.ic_play_circle_filled_black_48dp);
                button.setAlpha(ALPHA_ENABLED);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) { sendBroadcast(new Intent(IntentAction.RESUME)); }
                });
                return true;
            case CLEAR:
                button.setEnabled(true);
                button.setImageResource(R.drawable.ic_delete_black_48dp);
                button.setAlpha(ALPHA_ENABLED);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) { sendBroadcast(new Intent(IntentAction.CLEAR)); }
                });
                return true;
            case CLEAR_DISABLED:
                button.setEnabled(false);
                button.setImageResource(R.drawable.ic_delete_black_48dp);
                button.setAlpha(ALPHA_DISABLED);
                return true;
            case NEXT_SET_START:
                button.setEnabled(true);
                button.setImageResource(R.drawable.ic_chevron_right_double_black_48dp);
                button.setAlpha(ALPHA_ENABLED);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) { sendBroadcast(new Intent(IntentAction.NEXT_SET_START)); }
                });
                return true;
            case NEXT_SET_START_DISABLED:
                button.setEnabled(false);
                button.setImageResource(R.drawable.ic_chevron_right_double_black_48dp);
                button.setAlpha(ALPHA_DISABLED);
                return true;
            case RESET:
                button.setEnabled(true);
                button.setImageResource(R.drawable.ic_refresh_black_48dp);
                button.setAlpha(ALPHA_ENABLED);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) { sendBroadcast(new Intent(IntentAction.RESET)); }
                });
                return true;
            case RESET_DISABLED:
                button.setEnabled(false);
                button.setImageResource(R.drawable.ic_refresh_black_48dp);
                button.setAlpha(ALPHA_DISABLED);
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
                Action.TYPE_VIEW,
                "Main Page",
                Uri.parse("http://host/path"),
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
            unbindService(serviceConnection);
            timerServiceBound = false;
        }

        // Complete ongoing pop-up action
        if(alertSetDone.isShowing()) {
            sendBroadcast(new Intent(new Intent(IntentAction.STOP)));
            alertSetDone.dismiss();
        }
        else if(alertAllSetsDone.isShowing()) {
            sendBroadcast(new Intent(new Intent(IntentAction.CLEAR)));
            alertAllSetsDone.dismiss();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: timerServiceBound=" + timerServiceBound);
        mainActivityVisible = true;
        updateUserInterface();
        if(timerServiceBound) {
            timerService.setMainActivityVisible(true);
            timerService.updateNotificationVisibility(false);
        }
        else {
            Intent intent = new Intent(this, TimerService.class);
            startService(intent);
            bindService(intent, serviceConnection, Context.BIND_ABOVE_CLIENT);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        if(timerServiceBound) {
            timerService.setMainActivityVisible(false);
            timerService.updateNotificationVisibility(true);
            unbindService(serviceConnection);
            timerServiceBound = false;
        }

        unregisterReceiver(mainActivityReceiver);
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        String str = "";
        switch (level) {
            case TRIM_MEMORY_RUNNING_MODERATE: // 5
                str = "RUNNING_MODERATE";
                break;
            case TRIM_MEMORY_RUNNING_LOW: // 10
                str = "RUNNING_LOW";
                break;
            case TRIM_MEMORY_RUNNING_CRITICAL: // 15
                str = "RUNNING_CRITICAL, finishing activity";
                if (!mainActivityVisible) {
                    finish();
                }
                break;
            case TRIM_MEMORY_UI_HIDDEN: // 20
                str = "UI_HIDDEN";
                // TODO: release some ressources
                break;
        }
        Log.d(TAG, "onTrimMemory: level=" + str);
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected");
            timerServiceBound = false;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            TimerBinder binder = (TimerBinder) service;
            timerService = binder.getService();
            timerServiceBound = true;
            Log.d(TAG, "onServiceConnected");
            getTimerServiceContext();
            updateAllPreferences();
            updateUserInterface();
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

        if(key.contains("presetArray_") || key.contains("timerService_"))
            return;

        Log.d(TAG, "updatePreference: key=" + key);
        String color, uri;

        // TODO: keys to strings resources
        switch(key) {
            case "timerMinus":
                timerMinus = Long.parseLong(sharedPreferences.getString("timerMinus", "30"));
                if(timerServiceBound) {
                    timerService.setTimerMinus(timerMinus);
                }
                break;
            case "timerPlus":
                timerPlus = Long.parseLong(sharedPreferences.getString("timerPlus", "30"));
                if(timerServiceBound) {
                    timerService.setTimerPlus(timerPlus);
                }
                break;
            case "pickerInitZero":
                initPickerZero = sharedPreferences.getBoolean("pickerInitZero", true);
                break;
            case "vibrationEnable":
                vibrationEnable = sharedPreferences.getBoolean("vibrationEnable", true);
                if(timerServiceBound) {
                    timerService.interactiveNotification.setVibrationEnable(vibrationEnable);
                }
                break;
            case "vibrationReadyEnable":
                vibrationReadyEnable = sharedPreferences.getBoolean("vibrationReadyEnable", true);
                if(timerServiceBound) {
                    timerService.interactiveNotification.setVibrationReadyEnable(vibrationReadyEnable);
                }
                break;
            case "timerGetReadyEnable":
                timerGetReadyEnable = sharedPreferences.getBoolean("timerGetReadyEnable", true);
                if(timerServiceBound) {
                    timerService.setTimerGetReadyEnable(timerGetReadyEnable);
                }
                break;
            case "timerGetReady":
                if(timerGetReadyEnable) {
                    timerGetReady = Integer.parseInt(sharedPreferences.getString("timerGetReady", "15"));
                }
                else {
                    timerGetReady = -1;
                }
                if(timerServiceBound) {
                    timerService.setTimerGetReady(timerGetReady);
                }
                break;
            case "lightColor":
                int lightColor;
                color = sharedPreferences.getString("lightColor", "green");
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
                if(timerServiceBound) {
                    timerService.interactiveNotification.setLightColor(lightColor);
                }
                break;
            case "lightReadyColor":
                int lightReadyColor;
                color = sharedPreferences.getString("lightReadyColor", "yellow");
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
                if(timerServiceBound) {
                    timerService.interactiveNotification.setLightReadyColor(lightReadyColor);
                }
                break;
            case "ringtoneUri":
                uri = sharedPreferences.getString("ringtoneUri", "default");
                ringtone = Uri.parse(uri);
                if(timerServiceBound)
                    timerService.interactiveNotification.setRingtone(ringtone);
                break;
            case "ringtoneUriReady":
                uri = sharedPreferences.getString("ringtoneUriReady", "default");
                ringtoneReady = Uri.parse(uri);
                if(timerServiceBound)
                    timerService.interactiveNotification.setRingtoneReady(ringtoneReady);
                break;
            default:
                Log.e(TAG, "updatePreference: not supported preference key=" + key);
        }
    }

    SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                    updatePreference(key);
                }
            };
}
