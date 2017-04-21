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
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
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
import android.widget.Toast;

import com.codetroopers.betterpickers.mspicker.MsPickerBuilder;
import com.codetroopers.betterpickers.mspicker.MsPickerDialogFragment;
import com.codetroopers.betterpickers.numberpicker.NumberPickerBuilder;
import com.codetroopers.betterpickers.numberpicker.NumberPickerDialogFragment;
import com.simpleworkout.timer.TimerService.TimerBinder;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Locale;
import java.util.Map;

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
    public static final float ALPHA_ENABLED = (float) 1.0;
    public static final float ALPHA_DISABLED = (float) 0.2;

    // Sets number for infinity
    public static final int SETS_INFINITY = Integer.MAX_VALUE;

    // Main user interface
    private TextView timerTextView, setsCurrentTextView, setsNextTextView;
    private ProgressBar timerProgressBar, timerReadyProgressBar, setsProgressBar;
    private ButtonsLayout buttonsLayout;
    private ButtonAction buttonLeftAction, buttonCenterAction, buttonRightAction;
    private ImageButton imageButtonLeft, imageButtonCenter, imageButtonRight;
    private ImageButton imageButtonTimerMinus, imageButtonTimerPlus;

    // Timer and Sets Pickers
    private MsPickerBuilder timerPickerBuilder;
    private NumberPickerBuilder setsPickerBuilder;

    // Preset Timers
    private PresetCardsList presetCardsList;

    // Timer service related
    private TimerService.State timerState;
    private long timerCurrent;
    private long timerUser;
    private int setsInit;
    private int setsCurrent;
    private int setsUser;

    // Settings
    protected static final long[] vibrationPattern = {0, 400, 200, 400,};

    // User preferences
    private long timerMinus;
    private long timerPlus;
    private static boolean initPickerZero;
    private boolean vibrationEnable;
    private Uri ringtoneUri;
    private boolean timerGetReadyEnable;
    private int timerGetReady;
    private Uri ringtoneUriReady;

    public static boolean getInitPickerZero() {
        return initPickerZero;
    }

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
        NEXT_SET("next_set"),
        NEXT_SET_DISABLED("next_set_disabled"),
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
        setsCurrentTextView = (TextView) findViewById(R.id.textViewSetsCurrent);
        setsNextTextView = (TextView) findViewById(R.id.textViewSetsNext);

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
        setsProgressBar = (ProgressBar) findViewById(R.id.progressBarSets);

        imageButtonLeft = (ImageButton) findViewById(R.id.imageButtonLeft);
        imageButtonCenter = (ImageButton) findViewById(R.id.imageButtonCenter);
        imageButtonRight = (ImageButton) findViewById(R.id.imageButtonRight);

        imageButtonTimerMinus = (ImageButton) findViewById(R.id.imageButtonTimerMinus);
        imageButtonTimerPlus = (ImageButton) findViewById(R.id.imageButtonTimerPlus);
        imageButtonTimerMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendBroadcast(new Intent(IntentAction.TIMER_MINUS));
            }
        });
        imageButtonTimerPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendBroadcast(new Intent(IntentAction.TIMER_PLUS));
            }
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

        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentById(R.id.fragmentContainerPresetCards);
        if (fragment == null) {
            presetCardsList = new PresetCardsList();
            presetCardsList.createPresetsList(this, sharedPreferences);
            fragmentManager.beginTransaction().add(R.id.fragmentContainerPresetCards, presetCardsList).commit();
        }

        if (!timerServiceIsRunning()) {
            Log.d(TAG, "onCreate: starting service TimerService");
            startService(new Intent(getBaseContext(), TimerService.class));
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        mainActivityVisible = true;

        timerCurrent = 0;
        timerUser = 0;
        setsInit = 0;
        setsCurrent = 0;
        setsUser = 0;
        timerState = TimerService.State.WAITING;

        if (timerServiceBound) {
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
        if (timerServiceBound) {
            timerService.setMainActivityVisible(mainActivityVisible);
            timerService.updateNotificationVisibility(!mainActivityVisible);
        }
        timerProgressBar.setMax((int) timerUser);
        timerProgressBar.setProgress((int) timerCurrent);
        timerReadyProgressBar.setMax((int) timerUser);
        timerReadyProgressBar.setProgress(timerGetReadyEnable ? timerGetReady : 0);
        setsProgressBar.setMax((int) timerUser);
        setsProgressBar.setProgress((int) (timerUser - timerCurrent));
        updateSetsDisplay();
        updateTimerDisplay();
        updateButtonsLayout();
    }

    private boolean timerServiceIsRunning() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        // Loop through the running services
        for (ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)) {
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
        timerProgressBar.setMax((int) timerUser);
        setsProgressBar.setMax((int) timerUser);
        timerReadyProgressBar.setMax((int) timerUser);
        updateTimerDisplay();
        updateButtonsLayout(ButtonsLayout.WAITING_SETS);
        setsPickerBuilder.show();
    }

    @Override
    public void onDialogNumberSet(int reference, BigInteger number, double decimal, boolean isNegative, BigDecimal fullNumber, boolean checked) {
        int sets = number.intValue();
        setsInit = checked ? 0 : 1;
        setsCurrent = setsInit;
        setsUser = sets;
        Log.d(TAG, "onDialogNumberSet: setsUser=" + setsUser + ", setsCurrent=" + setsCurrent);
        updateSetsDisplay();
        terminatePickers();
    }

    @SuppressWarnings("deprecation")
    private void updateTimerDisplay() {
        String timeString = String.format(Locale.US, "%d:%02d", timerCurrent / 60, timerCurrent % 60);
        timerTextView.setText(timeString);
        timerProgressBar.setMax((int) timerUser);
        timerProgressBar.setProgress((int) timerCurrent);
        setsProgressBar.setMax((int) timerUser);
        setsProgressBar.setProgress((int) (timerUser - timerCurrent));
        timerReadyProgressBar.setMax((int) timerUser);
        if (!timerGetReadyEnable || timerCurrent <= timerGetReady) {
            timerReadyProgressBar.setProgress(0);
        } else {
            timerReadyProgressBar.setProgress(timerGetReady);
        }

        int color;
        if (timerCurrent == 0) {
            color = Color.GRAY;
        } else if (timerGetReadyEnable && timerCurrent <= timerGetReady) {
            color = Color.RED;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            color = getColor(R.color.colorPrimary);
        } else {
            color = getResources().getColor(R.color.colorPrimary);
        }
        timerTextView.setTextColor(color);
        timerProgressBar.setProgressTintList(ColorStateList.valueOf(color));
    }

    private void updateSetsDisplay() {
        String setsCurrentString = String.format(Locale.US, "%d", setsCurrent);
        String setsNextString = String.format(Locale.US, "%d", setsCurrent + 1);
        Log.d(TAG, "updateSetsDisplay: setsCurrentString='" + setsCurrentString + "', setsNextString='" + setsNextString + "'");
        setsCurrentTextView.setText(setsCurrentString);
        setsNextTextView.setText(setsNextString);
    }

    private void terminatePickers() {
        timerState = TimerService.State.READY;
        updateInputTimerService();
        updateButtonsLayout();
    }

    private void launchPickers() {
        if (buttonsLayout == ButtonsLayout.WAITING) {
            timerPickerBuilder.show();
        } else if (buttonsLayout == ButtonsLayout.WAITING_SETS) {
            setsPickerBuilder.show();
        } else {
            Log.e(TAG, "launchPickers: buttonsLayout=" + buttonsLayout);
        }
    }

    public void inputPreset(Preset preset) {
        Log.d(TAG, "inputPreset: preset=" + preset);

        timerService.stopCountDown();

        timerCurrent = preset.getTimer();
        timerUser = timerCurrent;
        setsInit = preset.getInit();
        setsCurrent = setsInit;
        setsUser = preset.getSets();

        timerProgressBar.setMax((int) timerUser);
        setsProgressBar.setMax((int) timerUser);
        timerReadyProgressBar.setMax((int) timerUser);
        updateTimerDisplay();
        updateSetsDisplay();

        terminatePickers();
    }

    private void updateInputTimerService() {
        if (timerServiceBound) {
            timerService.setState(timerState);
            timerService.setTimerCurrent(timerCurrent);
            timerService.setTimerUser(timerUser);
            timerService.setSetsInit(setsInit);
            timerService.setSetsCurrent(setsCurrent);
            timerService.setSetsUser(setsUser);
        }
    }

    public void addPreset() {
        if (buttonsLayout == ButtonsLayout.WAITING) {
            Toast.makeText(this, getString(R.string.picker_toast_all), Toast.LENGTH_SHORT).show();
        } else if (buttonsLayout == ButtonsLayout.WAITING_SETS) {
            Toast.makeText(this, getString(R.string.picker_toast_sets), Toast.LENGTH_SHORT).show();
        } else {
            if (!presetCardsList.addPreset(new Preset(timerUser, setsUser, setsInit))) {
                Toast.makeText(this, "The preset is already in the list", Toast.LENGTH_SHORT).show();
            }
        }
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
        if (setsCurrent > 1) {
            updateButtonsLayout();
        } else {
            updateButtonsLayout(ButtonsLayout.READY);
        }
    }

    protected void nextSet() {
        stop();
        // Going to nextSet on the last set is allowed from the notification
        if (++setsCurrent < setsUser) {
            Log.d(TAG, "nextSet: setsCurrent=" + setsCurrent);
        } else {
            Log.e(TAG, "nextSetStart: setsCurrent=" + setsCurrent);
        }
        updateSetsDisplay();
    }

    protected void nextSetStart() {
        nextSet();
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

        updateButtonsLayout();
        updateSetsDisplay();
    }

    protected void reset() {
        timerState = TimerService.State.READY;
        Log.d(TAG, "reset: timerState=" + timerState);

        updateButtonsLayout();
    }

    protected void extraSet() {
        Log.d(TAG, "extraSet: setsCurrent=" + setsCurrent);

        updateSetsDisplay();
        updateButtonsLayout(ButtonsLayout.RUNNING);
    }

    protected void timerMinus() {
        timerCurrent -= timerMinus;
        if (timerCurrent <= 0) {
            timerCurrent = 1;
        }
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
        if (timerState != state) {
            timerState = state;
            Log.d(TAG, "updateTimerState: synchronising timerState=" + timerState);
            updateButtonsLayout();
        }
    }

    protected void setsUpdate(int sets) {
        Log.d(TAG, "setsUpdate: sets=" + sets + ", setsCurrent=" + setsCurrent);
        if (setsCurrent != sets) {
            setsCurrent = sets;
            updateSetsDisplay();
            updateSetsButtons();
        }
    }

    protected void timerUpdate(long time) {
        if (timerCurrent != time) {
            timerCurrent = time;
            updateTimerDisplay();
            updateTimerButtons();
            if (time == timerGetReady && timerGetReadyEnable && mainActivityVisible) {
                ring(ringtoneUriReady);
                vibrate();
            }
        }
    }

    protected void done() {
        // The timer will be stopped from the alerts
        if (++setsCurrent < setsUser) {
            Log.d(TAG, "done: setsCurrent=" + setsCurrent);
            if (mainActivityVisible) {
                vibrate();
                ring();
                alertSetDone.show();
            }
            updateButtonsLayout(ButtonsLayout.STOPPED);
            updateSetsDisplay();
        } else {
            Log.d(TAG, "done: all sets done, setsCurrent=" + setsCurrent);
            if (mainActivityVisible) {
                vibrate();
                ring();
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

        if (id == R.id.preferences) {
            Log.d(TAG, "onOptionsItemSelected: item.id=settings");
            startActivityForResult(new Intent(this, PreferencesActivity.class), requestCode);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            return true;
        } else if (id == R.id.feedback) {
            Intent sendEmail = new Intent(Intent.ACTION_SEND);
            sendEmail.setType("text/email");
            sendEmail.putExtra(Intent.EXTRA_EMAIL, new String[]{"guillaume.militello@gmail.com"});
            sendEmail.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.app_name) + " feedback");
            startActivity(Intent.createChooser(sendEmail, "Send Feedback:"));
            return true;
        } else if (id == R.id.about) {
            Log.d(TAG, "onOptionsItemSelected: item.id=about");
            startActivityForResult(new Intent(this, AboutActivity.class), requestCode);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1) {
            Log.d(TAG, "onActivityResult: resultCode=" + resultCode);
        }
    }

    private void vibrate() {
        if (vibrationEnable) {
            Log.i(TAG, "vibrate");
            Vibrator vibrator;
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(vibrationPattern, -1);
        }
    }

    private void ring() {
        ring(ringtoneUri);
    }

    private void ring(Uri uri) {
        Ringtone ringtone;
        if (uri == null) {
            Log.e(TAG, "ring: null ringtone uri, using default notification uri");
            uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        ringtone = RingtoneManager.getRingtone(getApplicationContext(), uri);
        ringtone.play();
        Log.i(TAG, "ring: uri=" + uri.toString());
    }

    private void updateSetsButtons() {
        if ((buttonsLayout == ButtonsLayout.RUNNING || buttonsLayout == ButtonsLayout.PAUSED) && setsCurrent < setsUser - 1) {
            updateButton(imageButtonRight, ButtonAction.NEXT_SET);
        } else {
            updateButton(imageButtonRight, ButtonAction.NEXT_SET_DISABLED);
        }
    }

    private void updateTimerButtons() {
        if (buttonsLayout == ButtonsLayout.RUNNING || buttonsLayout == ButtonsLayout.PAUSED) {
            imageButtonTimerMinus.setEnabled(true);
            imageButtonTimerMinus.setAlpha(ALPHA_ENABLED);
        } else {
            imageButtonTimerMinus.setEnabled(false);
            imageButtonTimerMinus.setAlpha(ALPHA_DISABLED);
        }
        if (buttonsLayout == ButtonsLayout.RUNNING || buttonsLayout == ButtonsLayout.PAUSED) {
            imageButtonTimerPlus.setEnabled(true);
            imageButtonTimerPlus.setAlpha(ALPHA_ENABLED);
        } else {
            imageButtonTimerPlus.setEnabled(false);
            imageButtonTimerPlus.setAlpha(ALPHA_DISABLED);
        }
    }

    protected void updateAddPresetButton() {
        if (buttonsLayout == ButtonsLayout.WAITING || buttonsLayout == ButtonsLayout.WAITING_SETS) {
            presetCardsList.disableAddPresetButton();
        } else {
            presetCardsList.updateAddPresetCard(new Preset(timerUser, setsUser, setsInit));
        }
    }

    private void updateButtonsLayout() {
        ButtonsLayout layout = ButtonsLayout.valueOf(timerState.toString().toUpperCase(Locale.US));
        updateButtonsLayout(layout);
    }

    private void updateButtonsLayout(ButtonsLayout layout) {
        if (buttonsLayout != layout) {
            ButtonAction nextStep;
            switch (layout) {
                case WAITING:
                    updateButtons(ButtonAction.CLEAR_DISABLED, ButtonAction.INPUT, ButtonAction.NEXT_SET_DISABLED);
                    break;
                case WAITING_SETS:
                    updateButtons(ButtonAction.CLEAR, ButtonAction.INPUT, ButtonAction.NEXT_SET_DISABLED);
                    break;
                case READY:
                    updateButtons(ButtonAction.CLEAR, ButtonAction.START, ButtonAction.NEXT_SET_DISABLED);
                    break;
                case RUNNING:
                    nextStep = (setsCurrent < setsUser - 1) ? ButtonAction.NEXT_SET : ButtonAction.NEXT_SET_DISABLED;
                    updateButtons(ButtonAction.RESET, ButtonAction.PAUSE, nextStep);
                    break;
                case PAUSED:
                    nextStep = (setsCurrent < setsUser - 1) ? ButtonAction.NEXT_SET : ButtonAction.NEXT_SET_DISABLED;
                    updateButtons(ButtonAction.RESET, ButtonAction.RESUME, nextStep);
                    break;
                case STOPPED:
                    updateButtons(ButtonAction.RESET, ButtonAction.START, ButtonAction.NEXT_SET_DISABLED);
                    break;
                default:
                    Log.e(TAG, "updateButtonsLayout: impossible layout=" + layout.toString());
            }
            buttonsLayout = layout;
            Log.d(TAG, "updateButtonsLayout: buttonsLayout=" + buttonsLayout.toString());
        }
        updateSetsButtons();
        updateTimerButtons();
        updateAddPresetButton();
    }

    private void updateButtons(ButtonAction left, ButtonAction center, ButtonAction right) {

        if (left != buttonLeftAction && updateButton(imageButtonLeft, left)) {
            buttonLeftAction = left;
        }
        if (center != buttonCenterAction && updateButton(imageButtonCenter, center)) {
            buttonCenterAction = center;
        }
        if (right != buttonRightAction && updateButton(imageButtonRight, right)) {
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
                    public void onClick(View v) {
                        launchPickers();
                    }
                });
                return true;
            case START:
                button.setEnabled(true);
                button.setImageResource(R.drawable.ic_play_circle_filled_black_48dp);
                button.setAlpha(ALPHA_ENABLED);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        sendBroadcast(new Intent(IntentAction.START));
                    }
                });
                return true;
            case PAUSE:
                button.setEnabled(true);
                button.setImageResource(R.drawable.ic_pause_circle_filled_black_48dp);
                button.setAlpha(ALPHA_ENABLED);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        sendBroadcast(new Intent(IntentAction.PAUSE));
                    }
                });
                return true;
            case RESUME:
                button.setEnabled(true);
                button.setImageResource(R.drawable.ic_play_circle_filled_black_48dp);
                button.setAlpha(ALPHA_ENABLED);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        sendBroadcast(new Intent(IntentAction.RESUME));
                    }
                });
                return true;
            case CLEAR:
                button.setEnabled(true);
                button.setImageResource(R.drawable.ic_delete_black_48dp);
                button.setAlpha(ALPHA_ENABLED);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        sendBroadcast(new Intent(IntentAction.CLEAR));
                    }
                });
                return true;
            case CLEAR_DISABLED:
                button.setEnabled(false);
                button.setImageResource(R.drawable.ic_delete_black_48dp);
                button.setAlpha(ALPHA_DISABLED);
                return true;
            case NEXT_SET:
                button.setEnabled(true);
                button.setImageResource(R.drawable.ic_chevron_right_black_48dp);
                button.setAlpha(ALPHA_ENABLED);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        sendBroadcast(new Intent(IntentAction.NEXT_SET));
                    }
                });
                return true;
            case NEXT_SET_DISABLED:
                button.setEnabled(false);
                button.setImageResource(R.drawable.ic_chevron_right_black_48dp);
                button.setAlpha(ALPHA_DISABLED);
                return true;
            case NEXT_SET_START:
                button.setEnabled(true);
                button.setImageResource(R.drawable.ic_chevron_right_double_black_48dp);
                button.setAlpha(ALPHA_ENABLED);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        sendBroadcast(new Intent(IntentAction.NEXT_SET_START));
                    }
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
                    public void onClick(View v) {
                        sendBroadcast(new Intent(IntentAction.RESET));
                    }
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
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        mainActivityVisible = false;

        if (timerServiceBound) {
            timerService.setMainActivityVisible(false);
            timerService.updateNotificationVisibility(true);
            unbindService(serviceConnection);
            timerServiceBound = false;
        }

        // Complete ongoing pop-up action
        if (alertSetDone.isShowing()) {
            sendBroadcast(new Intent(new Intent(IntentAction.STOP)));
            alertSetDone.dismiss();
        } else if (alertAllSetsDone.isShowing()) {
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
        if (timerServiceBound) {
            timerService.setMainActivityVisible(true);
            timerService.updateNotificationVisibility(false);
        } else {
            Intent intent = new Intent(this, TimerService.class);
            startService(intent);
            bindService(intent, serviceConnection, Context.BIND_ABOVE_CLIENT);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        if (timerServiceBound) {
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

        if (key.contains(getString(R.string.pref_preset_array)) || key.contains(getString(R.string.pref_timer_service))) {
            return;
        }

        Log.d(TAG, "updatePreference: key=" + key);

        if (key.equals(getString(R.string.pref_timer_minus))) {
            timerMinus = Long.parseLong(sharedPreferences.getString(key, getString(R.string.default_timer_minus)));
            if (timerServiceBound) {
                timerService.setTimerMinus(timerMinus);
            }
        } else if (key.equals(getString(R.string.pref_timer_plus))) {
            timerPlus = Long.parseLong(sharedPreferences.getString(key, getString(R.string.default_timer_plus)));
            if (timerServiceBound) {
                timerService.setTimerPlus(timerPlus);
            }
        } else if (key.equals(getString(R.string.pref_picker_init_zero))) {
            initPickerZero = sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.default_picker_init_zero));
        } else if (key.equals(getString(R.string.pref_vibrate))) {
            vibrationEnable = sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.default_vibrate));
            if (timerServiceBound) {
                timerService.interactiveNotification.setVibrationEnable(vibrationEnable);
            }
        } else if (key.equals(getString(R.string.pref_ringtone_uri))) {
            ringtoneUri = Uri.parse(sharedPreferences.getString(key, getString(R.string.default_ringtone_uri)));
            if (timerServiceBound) {
                timerService.interactiveNotification.setRingtone(ringtoneUri);
            }
        } else if (key.equals(getString(R.string.pref_light_color))) {
            if (timerServiceBound) {
                int color = getColorInt(sharedPreferences.getString(key, getString(R.string.default_light_color)));
                timerService.interactiveNotification.setLightColor(color);
            }
        } else if (key.equals(getString(R.string.pref_timer_get_ready_enable))) {
            timerGetReadyEnable = sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.default_timer_get_ready_enable));
            if (timerServiceBound) {
                timerService.setTimerGetReadyEnable(timerGetReadyEnable);
            }
        } else if (key.equals(getString(R.string.pref_timer_get_ready))) {
            timerGetReady = Integer.parseInt(sharedPreferences.getString(key, getString(R.string.default_timer_get_ready)));
            if (timerServiceBound) {
                timerService.setTimerGetReady(timerGetReady);
            }
        } else if (key.equals(getString(R.string.pref_timer_get_ready_vibrate))) {
            if (timerServiceBound) {
                boolean vibrationReadyEnable = sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.default_timer_get_ready_vibrate));
                timerService.interactiveNotification.setVibrationReadyEnable(vibrationReadyEnable);
            }
        } else if (key.equals(getString(R.string.pref_timer_get_ready_ringtone_uri))) {
            ringtoneUriReady = Uri.parse(sharedPreferences.getString(key, getString(R.string.default_timer_get_ready_ringtone_uri)));
            if (timerServiceBound) {
                timerService.interactiveNotification.setRingtoneReady(ringtoneUriReady);
            }
        } else {
            Log.e(TAG, "updatePreference: not supported preference key");
        }
    }

    private int getColorInt(String color) {
        switch (color) {
            case "none":
                return InteractiveNotification.COLOR_NONE;
            case "default":
                return InteractiveNotification.COLOR_DEFAULT;
            default:
                return Color.parseColor(color);
        }
    }

    SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                    updatePreference(key);
                }
            };
}
