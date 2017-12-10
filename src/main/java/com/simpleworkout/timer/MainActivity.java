package com.simpleworkout.timer;

import android.app.ActivityManager;
import android.content.ComponentCallbacks2;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
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
    private static boolean timerServiceBound = false;

    // Timer done alerts
    private AlertDialog alertSetDone, alertAllSetsDone;

    // MainActivity user interface
    public static final float ALPHA_ENABLED = (float) 1.0;
    public static final float ALPHA_DISABLED = (float) 0.2;

    // Main user interface
    private Menu toolbarMenu;
    private TextView timerTextViewBold, timerTextView, setsTextView;//, timerUserTextView, setsUserTextView;
    private ProgressBar timerProgressBar;
    private ButtonsLayout buttonsLayout;
    private ButtonAction buttonLeftAction, buttonCenterAction, buttonRightAction;
    private ImageButton imageButtonLeft, imageButtonCenter, imageButtonRight;
    private ImageButton imageButtonTimerMinusMulti, imageButtonTimerPlusMulti;
    private ImageButton imageButtonTimerMinus, imageButtonTimerPlus, imageButtonKeepScreenOn;
    private LinearLayout informationLayout, fullButtonsLayout, timerButtonsMultiLayout, mainLayout, mainLayoutButton;
    private RelativeLayout timerLayout;
    private FrameLayout presetsFrameLayout;

    private boolean inMultiWindowMode;
    private int timerProgressBarWidth, timerProgressBarHeight;

    // Toolbar menu items index
    private static final int TOOLBAR_MENU_PRESET_INDEX = 0;
    private static final int TOOLBAR_MENU_KEEPSCREENON_INDEX = 2;

    // Timer and Sets Pickers
    private MsPickerBuilder timerPickerBuilder;
    private NumberPickerBuilder setsPickerBuilder;

    // Preset Timers
    private PresetCardsList presetCardsList;

    // Timer service related
    private TimerService.State timerState;
    private long timerCurrent;
    private long timerUser;
    private int setsCurrent;
    private int setsUser;

    private static boolean keepScreenOn = false;

    // Settings
    static final long[] vibrationPattern = {0, 400, 200, 400,};

    // User preferences
    private long timerMinus;
    private long timerPlus;
    private boolean vibrationEnable;
    private Uri ringtoneUri;
    private boolean timerGetReadyEnable;
    private int timerGetReady;
    private Uri ringtoneUriReady;
    private int colorRunning;
    private int colorReady;
    private int colorDone;

    public Preset getPresetUser() {
        return new Preset(timerUser, setsUser);
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

        private final String action;

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

    // Multi window layout mode
    private enum LayoutMode { ONE_THIRD, HALF, TWO_THIRD, FULL }

    @Override
    @SuppressWarnings("deprecation")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_main);

        // Update system color bar and icon for the system
        Toolbar toolbar = findViewById(R.id.actionBar);
        setSupportActionBar(toolbar);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().setStatusBarColor(getColor(R.color.colorPrimaryDark));
            setTaskDescription(new ActivityManager.TaskDescription(getApplicationInfo().name,
                    BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher),
                    getColor(R.color.colorPrimary)));
            if (toolbar != null) {
                toolbar.setTitleTextColor(getColor(R.color.bpWhite));
            }
        } else {
            getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
            setTaskDescription(new ActivityManager.TaskDescription(getApplicationInfo().name,
                    BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher),
                    getResources().getColor(R.color.colorPrimary)));
            if (toolbar != null) {
                toolbar.setTitleTextColor(getResources().getColor(R.color.bpWhite));
            }
        }

        timerTextViewBold = findViewById(R.id.textViewTimerBold);
        timerTextView = findViewById(R.id.textViewTimer);
        setsTextView = findViewById(R.id.textViewSets);

        informationLayout = findViewById(R.id.informationLayout);
        presetsFrameLayout = findViewById(R.id.fragmentContainerPresetCards);
        fullButtonsLayout = findViewById(R.id.fullButtonsLayout);
        timerLayout = findViewById(R.id.timerLayout);
        timerButtonsMultiLayout = findViewById(R.id.timerButtonsMulti);
        mainLayout = findViewById(R.id.mainLayout);
        mainLayoutButton = findViewById(R.id.mainLayoutButtons);

        Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/OpenSans-CondBold.ttf");
        Typeface typefaceLight = Typeface.createFromAsset(getAssets(), "fonts/OpenSans-CondLight.ttf");
        timerTextViewBold.setTypeface(typeface);
        timerTextView.setTypeface(typefaceLight);
        setsTextView.setTypeface(typeface);

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

        timerProgressBar = findViewById(R.id.timerProgressBar);

        imageButtonLeft = findViewById(R.id.imageButtonLeft);
        imageButtonCenter = findViewById(R.id.imageButtonCenter);
        imageButtonRight = findViewById(R.id.imageButtonRight);

        imageButtonTimerMinus = findViewById(R.id.imageButtonTimerMinus);
        imageButtonTimerPlus = findViewById(R.id.imageButtonTimerPlus);
        imageButtonTimerMinusMulti = findViewById(R.id.imageButtonTimerMinusMulti);
        imageButtonTimerPlusMulti = findViewById(R.id.imageButtonTimerPlusMulti);
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
        imageButtonTimerMinusMulti.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendBroadcast(new Intent(IntentAction.TIMER_MINUS));
            }
        });
        imageButtonTimerPlusMulti.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendBroadcast(new Intent(IntentAction.TIMER_PLUS));
            }
        });

        imageButtonKeepScreenOn = findViewById(R.id.imageButtonKeepScreenOn);
        imageButtonKeepScreenOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setKeepScreenOnStatus(!keepScreenOn);
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        bindService(intent, serviceConnection, Context.BIND_ABOVE_CLIENT);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        FragmentManager fragmentManager = getSupportFragmentManager();
        presetCardsList = new PresetCardsList();
        presetCardsList.initContext(this);
        presetCardsList.createPresetsList(sharedPreferences);
        fragmentManager.beginTransaction().replace(R.id.fragmentContainerPresetCards, presetCardsList).commit();

        if (!timerServiceIsRunning()) {
            Log.d(TAG, "onCreate: starting service TimerService");
            startForegroundService(new Intent(getBaseContext(), TimerService.class));
        }

        // Detect first run
        if (sharedPreferences.getBoolean("firstRun", true)) {
            Log.d(TAG, "onCreate: firstRun=true");
            // TODO: show overdraw indications
            // sharedPreferences.edit().putBoolean("firstRun", false).apply();
        }
    }

    private void setKeepScreenOnStatus(boolean enable) {
        keepScreenOn = enable;
        Log.d(TAG, "setKeepScreenOnStatus: keepScreenOn=" + keepScreenOn);
        if (keepScreenOn) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            imageButtonKeepScreenOn.setImageResource(R.drawable.ic_screen_lock_portrait_black_48dp);
            if (toolbarMenu != null) {
                toolbarMenu.getItem(TOOLBAR_MENU_KEEPSCREENON_INDEX).setChecked(true);
            }
        } else {
            getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            imageButtonKeepScreenOn.setImageResource(R.drawable.ic_stay_primary_portrait_black_48dp);
            if (toolbarMenu != null) {
                toolbarMenu.getItem(TOOLBAR_MENU_KEEPSCREENON_INDEX).setChecked(false);
            }
        }
    }

    private LayoutMode getLayoutMode(float scaleX) {
        // Use predefined thresholds to detect the current multi-window ratio
        if (scaleX > 1.1) {
            return LayoutMode.FULL;
        } else if (scaleX > 0.8) {
            return LayoutMode.TWO_THIRD;
        } else if (scaleX > 0.6) {
            return LayoutMode.HALF;
        } else {
            return LayoutMode.ONE_THIRD;
        }
    }

    private void scaleLayout() {

        int width = timerProgressBar.getWidth();
        int height = timerProgressBar.getHeight();

        if (width == 0 || height == 0) {
            Log.e(TAG, "scaleLayout: invalid width=" + width + ", height=" + height);
            return;
        }

        if ((width == timerProgressBarWidth && height == timerProgressBarHeight)) {
            Log.d(TAG, "scaleLayout: scale already properly defined");
            return;
        }
        else if (isScreenInLandscape()) {
            //noinspection SuspiciousNameCombination
            timerProgressBarWidth = height;
            //noinspection SuspiciousNameCombination
            timerProgressBarHeight = width;
        }
        else {
            timerProgressBarWidth = width;
            timerProgressBarHeight = height;
        }

        float layoutScaleX = (float) timerProgressBarHeight / timerProgressBarWidth;
        LayoutMode layoutMode = getLayoutMode(layoutScaleX);

        // Always display the presets in FULL layout mode
        updatePresetsLayout(layoutMode == LayoutMode.FULL);

        // In FULL layout mode, update the timerProgressBarHeight and the layout scale
        if (layoutMode == LayoutMode.FULL) {
            timerProgressBarHeight = timerProgressBar.getHeight();
            layoutScaleX = (float) timerProgressBarHeight / timerProgressBarWidth;
        }

        timerProgressBar.setScaleX(layoutScaleX);
        Log.d(TAG, "scaleLayout: width=" + width + ", height=" + height + ", layoutMode=" + layoutMode + ", inMultiWindowMode=" + inMultiWindowMode);

        // Scale main activity components
        // Use predefined thresholds to detect the current multi-window ratio
        float density = getResources().getDisplayMetrics().density;
        float timerLayoutHeight = timerProgressBar.getHeight() / density;
        Log.d(TAG, "scaleLayout: timerLayoutHeight=" + timerLayoutHeight + ", density=" + density);

        // Sizes are based on the full layout height in density
        float timerBoldTextSizeRatio, timerTextSizeRatio, layoutMarginRatio;
        int layoutWeight;
        switch (layoutMode) {
            case ONE_THIRD:
                fullButtonsLayout.setVisibility(View.GONE);
                timerButtonsMultiLayout.setVisibility(View.VISIBLE);
                informationLayout.setVisibility(View.GONE);
                // userTextViews and imageViews are not shown on this layout mode
                timerBoldTextSizeRatio = 1.7f;
                timerTextSizeRatio = 5.882f;
                layoutMarginRatio = 10;
                layoutWeight = 5;
                break;
            case HALF:
                fullButtonsLayout.setVisibility(View.GONE);
                timerButtonsMultiLayout.setVisibility(View.VISIBLE);
                informationLayout.setVisibility(View.VISIBLE);
                timerBoldTextSizeRatio = 1.8f;
                timerTextSizeRatio = 5.555f;
                layoutMarginRatio = 7;
                layoutWeight = 4;
                break;
            case TWO_THIRD:
                fullButtonsLayout.setVisibility(View.GONE);
                timerButtonsMultiLayout.setVisibility(View.VISIBLE);
                informationLayout.setVisibility(View.VISIBLE);
                timerBoldTextSizeRatio = 1.8f;
                timerTextSizeRatio = 5.555f;
                layoutMarginRatio = 8;
                layoutWeight = 4;
                break;
            default:
            case FULL:
                fullButtonsLayout.setVisibility(View.VISIBLE);
                timerButtonsMultiLayout.setVisibility(View.GONE);
                informationLayout.setVisibility(View.VISIBLE);
                timerBoldTextSizeRatio = 2f;
                timerTextSizeRatio = 3.25f;
                layoutMarginRatio = 5;
                layoutWeight = 3;
                break;
        }
        Log.d(TAG, "scaleLayout: timerBoldTextSizeRatio=" + timerBoldTextSizeRatio + ", timerTextSizeRatio=" + timerTextSizeRatio);
        timerTextViewBold.setTextSize(timerLayoutHeight / timerBoldTextSizeRatio);
        timerTextView.setTextSize(timerLayoutHeight / timerTextSizeRatio);

        Log.d(TAG, "scaleLayout: layoutMarginRatio=" + layoutMarginRatio);
        int layoutMargin = (int)(-timerLayoutHeight / layoutMarginRatio * density);
        int layoutMarginTop = (int)(-timerLayoutHeight * 1.1 / layoutMarginRatio * density);
        int layoutMarginBottom = (int)(-timerLayoutHeight * 0.9 / layoutMarginRatio * density);

        LinearLayout.LayoutParams timerButtonsMultiLayoutParams = (LinearLayout.LayoutParams) timerLayout.getLayoutParams();
        timerButtonsMultiLayoutParams.setMargins(0, layoutMarginTop, 0, layoutMarginBottom);
        timerButtonsMultiLayoutParams.weight = layoutWeight;

        LinearLayout.LayoutParams mainLayoutParams = (LinearLayout.LayoutParams) mainLayout.getLayoutParams();
        mainLayoutParams.setMargins(0, 0, 0, 2 * layoutMargin);

        RelativeLayout.LayoutParams mainLayoutButtonsParams = (RelativeLayout.LayoutParams) mainLayoutButton.getLayoutParams();
        mainLayoutButtonsParams.setMargins(0, 0, 0, 2 * layoutMargin);
    }

    // Detect the screen orientation with DisplayMetrics for better support in multiWindowMode
    private boolean isScreenInLandscape() {
        WindowManager manager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        if (manager != null) {
            Display display = manager.getDefaultDisplay();
            DisplayMetrics metrics = new DisplayMetrics();
            display.getMetrics(metrics);
            int width = metrics.widthPixels;
            int height = metrics.heightPixels;
            Log.d(TAG, "isScreenInLandscape: width=" + width + ", height=" + height);
            return width >= height;
        }
        return false; // Default is portrait mode
    }


    @Override
    public void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart: timerService=" + timerService + ", timerServiceBound=" + timerServiceBound);
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: timerService=" + timerService + ", timerServiceBound=" + timerServiceBound);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            inMultiWindowMode = isInMultiWindowMode();
            Log.d(TAG, "onStart: inMultiWindowMode=" + inMultiWindowMode);
            presetsFrameLayout.setVisibility(inMultiWindowMode? View.GONE : View.VISIBLE);
        }

        mainActivityVisible = true;

        timerCurrent = 0;
        timerUser = 0;
        setsCurrent = 0;
        setsUser = 0;
        timerState = TimerService.State.WAITING;

        if (timerService == null) {
            Intent intent = new Intent(this, TimerService.class);
            startService(intent);
            timerServiceBound = bindService(intent, serviceConnection, Context.BIND_ABOVE_CLIENT);
            Log.d(TAG, "onStart: timerService=" + timerService + ", timerServiceBound=" + timerServiceBound);
        }

        updateUserInterface();
    }

    private void updateUserInterface() {
        if (timerService != null) {
            Log.d(TAG, "updateUserInterface: mainActivityVisible=" + mainActivityVisible);
            timerService.setMainActivityVisible(mainActivityVisible);
            timerService.updateNotificationVisibility(!mainActivityVisible);
            if (mainActivityVisible) {
                timerCurrent = timerService.getTimerCurrent();
                timerUser = timerService.getTimerUser();
                setsCurrent = timerService.getSetsCurrent();
                setsUser = timerService.getSetsUser();
                timerState = timerService.getState();
                Log.d(TAG, "updateUserInterface: timerCurrent=" + timerCurrent + ", timerUser=" + timerUser +
                        ", setsCurrent=" + setsCurrent + ", setsUser=" + setsUser + ", timerState=" + timerState);
                timerProgressBar.setMax((int) timerUser);
                timerProgressBar.setProgress((int) (timerUser - timerCurrent));
                updateButtonsLayout();
            }
        }
    }

    private boolean timerServiceIsRunning() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null) {
            // Loop through the running services
            for (ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)) {
                if (TimerService.class.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

    void timerServiceRebind() {
        Log.d(TAG, "timerServiceRebind: mainActivityVisible=" + mainActivityVisible);
        // Rebind occurs only when relaunching the mainActivity
        updateUserInterface();
        scaleLayout();
    }

    @Override
    public void onDialogMsSet(int reference, boolean isNegative, int minutes, int seconds) {
        long timer = minutes * 60 + seconds;
        timerCurrent = timer;
        timerUser = timer;
        Log.d(TAG, "onDialogMsSet: timerUser=" + timerUser);
        timerProgressBar.setMax((int) timerUser);
        updateButtonsLayout(ButtonsLayout.WAITING_SETS);
        updateServiceTimers();
        setsPickerBuilder.show();
    }

    @Override
    public void onDialogNumberSet(int reference, BigInteger number, double decimal, boolean isNegative, BigDecimal fullNumber) {
        int sets = number.intValue();
        setsCurrent = 1;
        setsUser = sets;
        Log.d(TAG, "onDialogNumberSet: setsUser=" + setsUser + ", setsCurrent=" + setsCurrent);
        updateSetsDisplay();
        updatePresetDisplay();
        terminatePickers();
    }

    @SuppressWarnings("deprecation")
    private void updateTimerDisplay() {
        // TODO : merge with Preset class
        if (timerCurrent >= 60) {
            timerTextViewBold.setText(String.format(Locale.US, "%d", timerCurrent / 60));
            timerTextView.setVisibility(View.VISIBLE);
            timerTextView.setText(String.format(Locale.US, "%02d", timerCurrent % 60));
        } else {
            timerTextViewBold.setText(String.format(Locale.US, "%d", timerCurrent % 60));
            timerTextView.setVisibility(View.GONE);
        }
        timerProgressBar.setMax((int) timerUser);
        timerProgressBar.setProgress((int) (timerUser - timerCurrent));

    }

    @SuppressWarnings("deprecation")
    private void updateColorLayout() {
        int progressColor, backgroundColor;
        final double backgroundColorLighten = 0.175, pauseColorLighten = 0.125, textColorDarken = 0.7;
        Log.d(TAG, "updateColorLayout: buttonsLayout=" + buttonsLayout);
        switch (buttonsLayout) {
            case READY:
                backgroundColor = lighten(colorRunning, backgroundColorLighten);
                progressColor = backgroundColor;
                break;
            case PAUSED:
                if (timerGetReadyEnable && timerCurrent <= timerGetReady && timerUser > timerGetReady) {
                    progressColor = lighten(colorReady, pauseColorLighten);
                    backgroundColor = lighten(colorReady, backgroundColorLighten);
                } else {
                    progressColor = lighten(colorRunning, pauseColorLighten);
                    backgroundColor = lighten(colorRunning, backgroundColorLighten);
                }
                break;
            case RUNNING:
                if (timerGetReadyEnable && timerCurrent <= timerGetReady && timerUser > timerGetReady) {
                    progressColor = colorReady;
                    backgroundColor = lighten(colorReady, backgroundColorLighten);
                } else {
                    progressColor = colorRunning;
                    backgroundColor = lighten(colorRunning, backgroundColorLighten);
                }
                break;
            case STOPPED:
                backgroundColor = colorDone;
                progressColor = colorDone;
                break;
            default:
            case WAITING:
            case WAITING_SETS:
                progressColor = R.color.color_waiting;
                backgroundColor = R.color.color_waiting;
                break;
        }
        int textColor = darken(progressColor, textColorDarken);

        timerProgressBar.setProgressBackgroundTintList(ColorStateList.valueOf(backgroundColor));
        timerProgressBar.setProgressTintList(ColorStateList.valueOf(progressColor));
        timerTextView.setTextColor(textColor);
        timerTextViewBold.setTextColor(textColor);
        setsTextView.setTextColor(textColor);
    }

    public static int lighten(int color, double fraction) {
        int red = lightenColor(Color.red(color), fraction);
        int green = lightenColor(Color.green(color), fraction);
        int blue = lightenColor(Color.blue(color), fraction);
        int alpha = Color.alpha(color);
        return Color.argb(alpha, red, green, blue);
    }

    public static int darken(int color, double fraction) {
        int red = darkenColor(Color.red(color), fraction);
        int green = darkenColor(Color.green(color), fraction);
        int blue = darkenColor(Color.blue(color), fraction);
        int alpha = Color.alpha(color);

        return Color.argb(alpha, red, green, blue);
    }

    private static int darkenColor(int color, double fraction) {
        return (int)Math.max(color - (color * fraction), 0);
    }

    private static int lightenColor(int color, double fraction) {
        return (int)Math.min(color + (color * fraction), 255);
    }

    private void updateSetsDisplay() {
        setsTextView.setText(String.format(Locale.US, "%d", setsCurrent));
    }

    private void updatePresetDisplay() {
        //Preset preset = new Preset(timerUser, setsUser); // TODO : implements progress bar in preset
        //timerUserTextView.setText(preset.getTimerString());
        //setsUserTextView.setText(preset.getSetsString());
    }

    private void terminatePickers() {
        timerState = TimerService.State.READY;
        updateServiceState();
        updateButtonsLayout();
        if (inMultiWindowMode) {
            changePresetsFrameLayout();
        }
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
        setsCurrent = 1;
        setsUser = preset.getSets();

        timerProgressBar.setMax((int) timerUser);
        terminatePickers();
    }

    private void updateServiceTimers() {
        if (timerService != null) {
            timerService.setTimerCurrent(timerCurrent);
            timerService.setTimerUser(timerUser);
        }
    }

    private void updateServiceState() {
        if (timerService != null) {
            timerService.setTimerCurrent(timerCurrent);
            timerService.setTimerUser(timerUser);
            timerService.setSetsCurrent(setsCurrent);
            timerService.setSetsUser(setsUser);
            timerService.setReadyState();
        }
    }

    public void addPreset() {
        if (buttonsLayout == ButtonsLayout.WAITING) {
            Toast.makeText(this, getString(R.string.picker_toast_all), Toast.LENGTH_SHORT).show();
        } else if (buttonsLayout == ButtonsLayout.WAITING_SETS) {
            Toast.makeText(this, getString(R.string.picker_toast_sets), Toast.LENGTH_SHORT).show();
        } else {
            presetCardsList.addPreset();
        }
    }

    protected void start() {
        timerState = TimerService.State.RUNNING;
        Log.d(TAG, "start: timerState=" + timerState);
        updateButtonsLayout();
    }

    void pause() {
        timerState = TimerService.State.PAUSED;
        Log.d(TAG, "pause: timerState=" + timerState);
        updateButtonsLayout();
    }

    protected void resume() {
        timerState = TimerService.State.RUNNING;
        Log.d(TAG, "resume: timerState=" + timerState);
        updateButtonsLayout();
    }

    void stop() {
        timerState = TimerService.State.READY;
        Log.d(TAG, "stop: timerState=" + timerState + ", setsCurrent=" + setsCurrent);
        updateButtonsLayout(ButtonsLayout.READY);
    }

    protected void nextSet() {
        // Going to nextSet on the last set is allowed from the notification
        if (++setsCurrent <= setsUser) {
            Log.d(TAG, "nextSet: setsCurrent=" + setsCurrent);
        } else {
            Log.e(TAG, "nextSetStart: setsCurrent=" + setsCurrent);
        }
        stop();
    }

    void nextSetStart() {
        nextSet();
        timerState = TimerService.State.RUNNING;
        Log.d(TAG, "nextSetStart: timerState=" + timerState);
        updateButtonsLayout();
    }

    protected void clear() {
        timerState = TimerService.State.WAITING;
        Log.d(TAG, "clear: timerState=" + timerState);

        timerUser = 0;
        timerCurrent = 0;
        setsCurrent = 0;
        setsUser = 0;

        updateButtonsLayout();
    }

    void reset() {
        timerState = TimerService.State.READY;
        Log.d(TAG, "reset: timerState=" + timerState);

        updateButtonsLayout(ButtonsLayout.READY);
    }

    protected void extraSet() {
        Log.d(TAG, "extraSet: setsCurrent=" + setsCurrent);

        updateButtonsLayout(ButtonsLayout.RUNNING);
    }

    void timerMinus() {
        timerCurrent -= timerMinus;
        if (timerCurrent <= 0) {
            timerCurrent = 1;
        }
        Log.d(TAG, "timerMinus: timerCurrent=" + timerCurrent);

        updateTimerDisplay();
        updateTimerButtons();
        updateColorLayout();
    }

    protected void timerPlus() {
        timerCurrent += timerPlus;
        Log.d(TAG, "timerPlus: timerCurrent=" + timerCurrent);

        updateTimerDisplay();
        updateTimerButtons();
        updateColorLayout();
    }

    void setsMinus() {
        setsCurrent -= 1;
        Log.d(TAG, "setsMinus: setsCurrent=" + setsCurrent);

        updateSetsDisplay();
        updateSetsButtons();
        updateColorLayout();
    }

    protected void setsPlus() {
        setsCurrent += 1;
        Log.d(TAG, "setsPlus: setsCurrent=" + setsCurrent);

        updateSetsDisplay();
        updateSetsButtons();
        updateColorLayout();
    }

    void updateTimerState(TimerService.State state) {
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
            updateButtonsLayout();
        }
    }

    void timerUpdate(long time) {
        if (mainActivityVisible) {
            if (timerCurrent != time) {
                // Avoid the extra notification when the timerUser == timerGetReady and when not RUNNING
                if (time == timerGetReady && timerUser > timerGetReady && timerGetReadyEnable && timerState == TimerService.State.RUNNING) {
                    ring(ringtoneUriReady);
                    vibrate();
                }
                timerCurrent = time;
                updateTimerDisplay();
                updateTimerButtons();
                updateColorLayout();
            }
        }
    }

    protected void done() {
        // The timer will be stopped from the alerts
        if (++setsCurrent <= setsUser) {
            Log.d(TAG, "done: setsCurrent=" + setsCurrent);
            if (mainActivityVisible) {
                updateTimerDone();
                alertSetDone.show();
            }
            updateButtonsLayout(ButtonsLayout.STOPPED);
        } else {
            Log.d(TAG, "done: all sets done, setsCurrent=" + setsCurrent);
            if (mainActivityVisible) {
                updateTimerDone();
                alertAllSetsDone.show();
            }
            updateButtonsLayout(ButtonsLayout.READY);
        }
    }

    private void updateTimerDone() {
        timerCurrent = 0;
        updateTimerDisplay();
        updateTimerButtons();
        updateColorLayout();
        vibrate();
        ring();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "onCreateOptionsMenu");
        // Inflate the menu, this adds items to the action bar if it is present
        getMenuInflater().inflate(R.menu.activity_main_favorites, menu);
        toolbarMenu = menu;
        if (presetsFrameLayout != null) {
            // fullButtonsLayout is only visible in FULL layout mode
            updateToolBarMenuItems(fullButtonsLayout.getVisibility() == View.GONE);
            setKeepScreenOnStatus(keepScreenOn);
        }
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
        } else if (id == R.id.presets_display) {
            Log.d(TAG, "onOptionsItemSelected: item.id=presets_display");
            changePresetsFrameLayout();
            return true;
        } else if (id == R.id.keepScreenOn) {
            Log.d(TAG, "onOptionsItemSelected: item.id=keepScreenOn");
            setKeepScreenOnStatus(!keepScreenOn);
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
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null) {
                vibrator.vibrate(vibrationPattern, -1);
            }
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
        if ((buttonsLayout == ButtonsLayout.RUNNING || buttonsLayout == ButtonsLayout.PAUSED) && setsCurrent < setsUser) {
            updateButton(imageButtonRight, ButtonAction.NEXT_SET);
        } else {
            updateButton(imageButtonRight, ButtonAction.NEXT_SET_DISABLED);
        }
    }

    private void updateTimerButtons() {
        if (buttonsLayout == ButtonsLayout.RUNNING || buttonsLayout == ButtonsLayout.PAUSED) {
            imageButtonTimerMinus.setEnabled(true);
            imageButtonTimerMinus.setAlpha(ALPHA_ENABLED);
            imageButtonTimerMinusMulti.setEnabled(true);
            imageButtonTimerMinusMulti.setAlpha(ALPHA_ENABLED);
        } else {
            imageButtonTimerMinus.setEnabled(false);
            imageButtonTimerMinus.setAlpha(ALPHA_DISABLED);
            imageButtonTimerMinusMulti.setEnabled(false);
            imageButtonTimerMinusMulti.setAlpha(ALPHA_DISABLED);
        }
        if (buttonsLayout == ButtonsLayout.RUNNING || buttonsLayout == ButtonsLayout.PAUSED) {
            imageButtonTimerPlus.setEnabled(true);
            imageButtonTimerPlus.setAlpha(ALPHA_ENABLED);
            imageButtonTimerPlusMulti.setEnabled(true);
            imageButtonTimerPlusMulti.setAlpha(ALPHA_ENABLED);
        } else {
            imageButtonTimerPlus.setEnabled(false);
            imageButtonTimerPlus.setAlpha(ALPHA_DISABLED);
            imageButtonTimerPlusMulti.setEnabled(false);
            imageButtonTimerPlusMulti.setAlpha(ALPHA_DISABLED);
        }
    }

    private void changePresetsFrameLayout() {
        if (presetsFrameLayout != null) {
            presetCardsList.resetScrollPosition();
            if (presetsFrameLayout.getVisibility() == View.GONE) {
                Log.d(TAG, "changePresetsFrameLayout: setVisibility=visible");
                presetsFrameLayout.setVisibility(View.VISIBLE);
                updatePresetsExpandButton(true);
            } else {
                Log.d(TAG, "changePresetsFrameLayout: setVisibility=invisible");
                presetsFrameLayout.setVisibility(View.GONE);
                updatePresetsExpandButton(false);
            }
        }
    }

    private void updateToolBarMenuItems(boolean visible) {
        if (toolbarMenu != null) {
            Log.d(TAG, "updateToolBarMenuItems: visible=" + visible);
            toolbarMenu.getItem(TOOLBAR_MENU_PRESET_INDEX).setVisible(visible);
            updatePresetsExpandButton(!visible);
            toolbarMenu.getItem(TOOLBAR_MENU_KEEPSCREENON_INDEX).setVisible(visible);
        }
    }

    private void updatePresetsLayout(boolean visible) {
        if (presetsFrameLayout != null) {
            Log.d(TAG, "updatePresetsLayout: visible=" + visible);
            presetsFrameLayout.setVisibility(visible ? View.VISIBLE : View.GONE);
            updateToolBarMenuItems(!visible);
        }
    }

    private void updatePresetsExpandButton(boolean expand) {
        if (expand) {
            toolbarMenu.getItem(TOOLBAR_MENU_PRESET_INDEX).setIcon(getDrawable(R.drawable.ic_chevron_right_black_48dp));
        } else {
            toolbarMenu.getItem(TOOLBAR_MENU_PRESET_INDEX).setIcon(getDrawable(R.drawable.ic_chevron_left_black_48dp));
        }
    }

    private void updateButtonsLayout() {
        ButtonsLayout layout = ButtonsLayout.valueOf(timerState.toString().toUpperCase(Locale.US));
        if (layout == ButtonsLayout.WAITING && timerUser > 0) {
            layout = ButtonsLayout.WAITING_SETS;
        }
        if (timerState != TimerService.State.WAITING && timerCurrent == 0 && setsCurrent ==0) {
            Log.e(TAG, "updateButtonsLayout: wrong layout timerState=" + timerState + ", timerCurrent=" + timerCurrent + ", setsCurrent=" + setsCurrent);
            layout = ButtonsLayout.WAITING;
        }
        updateButtonsLayout(layout);
    }

    private void updateButtonsLayout(ButtonsLayout layout) {
        ButtonAction buttonAction;
        switch (layout) {
            case WAITING:
                updateButtons(ButtonAction.CLEAR_DISABLED, ButtonAction.INPUT, ButtonAction.NEXT_SET_DISABLED);
                break;
            case WAITING_SETS:
                updateButtons(ButtonAction.CLEAR, ButtonAction.INPUT, ButtonAction.NEXT_SET_DISABLED);
                break;
            case READY:
                buttonAction = (setsCurrent > 1)? ButtonAction.RESET : ButtonAction.CLEAR;
                updateButtons(buttonAction, ButtonAction.START, ButtonAction.NEXT_SET_DISABLED);
                break;
            case RUNNING:
                buttonAction = (setsCurrent < setsUser) ? ButtonAction.NEXT_SET : ButtonAction.NEXT_SET_DISABLED;
                updateButtons(ButtonAction.RESET, ButtonAction.PAUSE, buttonAction);
                break;
            case PAUSED:
                buttonAction = (setsCurrent < setsUser) ? ButtonAction.NEXT_SET : ButtonAction.NEXT_SET_DISABLED;
                updateButtons(ButtonAction.RESET, ButtonAction.RESUME, buttonAction);
                break;
            case STOPPED:
                updateButtons(ButtonAction.RESET, ButtonAction.START, ButtonAction.NEXT_SET_DISABLED);
                break;
            default:
                Log.e(TAG, "updateButtonsLayout: impossible layout=" + layout.toString());
        }
        buttonsLayout = layout;
        Log.d(TAG, "updateButtonsLayout: buttonsLayout=" + buttonsLayout.toString());

        // The buttons are INVISIBLE by default in xml to enahnce the multi window switch
        imageButtonLeft.setVisibility(View.VISIBLE);
        imageButtonCenter.setVisibility(View.VISIBLE);
        imageButtonRight.setVisibility(View.VISIBLE);
        imageButtonTimerMinus.setVisibility(View.VISIBLE);
        imageButtonTimerPlus.setVisibility(View.VISIBLE);
        imageButtonKeepScreenOn.setVisibility(View.VISIBLE);

        updateSetsButtons();
        updateTimerButtons();
        updateTimerDisplay();
        updateSetsDisplay();
        updatePresetDisplay();
        updateColorLayout();
        presetCardsList.update();
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
                button.setImageResource(R.drawable.ic_add_circle_black_48dp);
                button.setEnabled(true);
                if (inMultiWindowMode) {
                    button.setAlpha(ALPHA_DISABLED);
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Toast.makeText(getApplicationContext(), getString(R.string.picker_split_screen), Toast.LENGTH_SHORT).show();
                        }
                    });
                    return true;
                }
                else {
                    button.setAlpha(ALPHA_ENABLED);
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            launchPickers();
                        }
                    });
                    return true;
                }
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
        Log.d(TAG, "onStop: mainActivityVisible=" + mainActivityVisible + ", timerService=" + timerService + ", timerServiceBound=" + timerServiceBound);
        mainActivityVisible = false; // TODO: useless ?

        if (timerService != null) {
            timerService.updateNotificationVisibility(true);
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
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: timerService=" + timerService + ", timerServiceBound=" + timerServiceBound);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: timerService=" + timerService + ", timerServiceBound=" + timerServiceBound);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: timerService=" + timerService + ", timerServiceBound=" + timerServiceBound);
        super.onDestroy();

        if (timerService != null) {
            timerService.updateNotificationVisibility(true);
        }

        if (timerServiceBound) {
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
                // TODO: release some resources
                break;
            case ComponentCallbacks2.TRIM_MEMORY_BACKGROUND:
                break;
            case ComponentCallbacks2.TRIM_MEMORY_COMPLETE:
                break;
            case ComponentCallbacks2.TRIM_MEMORY_MODERATE:
                break;
        }
        Log.d(TAG, "onTrimMemory: level=" + str);
    }

    @Override
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode) {
        super.onMultiWindowModeChanged(isInMultiWindowMode);
        if (inMultiWindowMode != isInMultiWindowMode) {
            inMultiWindowMode = isInMultiWindowMode;
            // force update center button action
            updateButton(imageButtonCenter, buttonCenterAction);
            Log.d(TAG, "onMultiWindowModeChanged: inMultiWindowMode=" + inMultiWindowMode);
        }
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {

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
            updateAllPreferences();
            updateUserInterface();
            scaleLayout();
        }
    };

    private void updateAllPreferences() {
        Log.d(TAG, "updateSummaries");
        Map<String, ?> preferences = sharedPreferences.getAll();
        if (preferences != null) {
            for (Map.Entry<String, ?> preference : preferences.entrySet()) {
                updatePreference(preference.getKey());
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                timerService.interactiveNotification.updateNotificationChannels();
            }
        }
    }

    private boolean updatePreference(String key) {

        if (key.contains(getString(R.string.pref_preset_array)) || key.contains(getString(R.string.pref_timer_service))) {
            return false;
        }

        Log.d(TAG, "updatePreference: key=" + key);

        if (key.equals(getString(R.string.pref_timer_minus))) {
            timerMinus = Long.parseLong(sharedPreferences.getString(key, getString(R.string.default_timer_minus)));
            if (timerService != null) {
                timerService.setTimerMinus(timerMinus);
            }
        } else if (key.equals(getString(R.string.pref_timer_plus))) {
            timerPlus = Long.parseLong(sharedPreferences.getString(key, getString(R.string.default_timer_plus)));
            if (timerService != null) {
                timerService.setTimerPlus(timerPlus);
            }
        } else if (key.equals(getString(R.string.pref_vibrate))) {
            vibrationEnable = sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.default_vibrate));
            if (timerService != null) {
                timerService.interactiveNotification.setVibrationEnable(vibrationEnable);
                return true;
            }
        } else if (key.equals(getString(R.string.pref_ringtone_uri))) {
            ringtoneUri = Uri.parse(sharedPreferences.getString(key, getString(R.string.default_ringtone_uri)));
            if (timerService != null) {
                timerService.interactiveNotification.setRingtone(ringtoneUri);
                return true;
            }
        }  else if (key.equals(getString(R.string.pref_light_color_enable))) {
            if (timerService != null) {
                boolean colorEnable = sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.default_light_color_enable));
                timerService.interactiveNotification.setLightColorEnable(colorEnable);
                return true;
            }
        } else if (key.equals(getString(R.string.pref_light_color))) {
            if (timerService != null) {
                int light_color = sharedPreferences.getInt(key, getColor(R.color.default_light_color));
                timerService.interactiveNotification.setLightColor(light_color);
                return true;
            }
        } else if (key.equals(getString(R.string.pref_light_flash_rate))) {
            if (timerService != null) {
                int flashRate = Integer.parseInt(sharedPreferences.getString(key, getString(R.string.default_light_flash_rate)));
                timerService.interactiveNotification.setLightFlashRate(flashRate);
            }
        } else if (key.equals(getString(R.string.pref_timer_get_ready_enable))) {
            timerGetReadyEnable = sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.default_timer_get_ready_enable));
            if (timerService != null) {
                timerService.setTimerGetReadyEnable(timerGetReadyEnable);
                timerService.interactiveNotification.setTimerGetReadyEnable(timerGetReadyEnable);
            }
        } else if (key.equals(getString(R.string.pref_timer_get_ready))) {
            timerGetReady = Integer.parseInt(sharedPreferences.getString(key, getString(R.string.default_timer_get_ready)));
            if (timerService != null) {
                timerService.setTimerGetReady(timerGetReady);
                timerService.interactiveNotification.setTimerGetReady(timerGetReady);
                return true;
            }
        } else if (key.equals(getString(R.string.pref_timer_get_ready_vibrate))) {
            if (timerService != null) {
                boolean vibrationReadyEnable = sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.default_timer_get_ready_vibrate));
                timerService.interactiveNotification.setVibrationReadyEnable(vibrationReadyEnable);
                return true;
            }
        } else if (key.equals(getString(R.string.pref_timer_get_ready_ringtone_uri))) {
            ringtoneUriReady = Uri.parse(sharedPreferences.getString(key, getString(R.string.default_timer_get_ready_ringtone_uri)));
            if (timerService != null) {
                timerService.interactiveNotification.setRingtoneReady(ringtoneUriReady);
                return true;
            }
        } else if (key.equals(getString(R.string.pref_custom_color_running))) {
            colorRunning = sharedPreferences.getInt(key, getColor(R.color.default_color_running));
            if (timerService != null) {
                timerService.interactiveNotification.setColorRunning(colorRunning);
            }
        } else if (key.equals(getString(R.string.pref_custom_color_ready))) {
            colorReady = sharedPreferences.getInt(key, getColor(R.color.default_color_ready));
            if (timerService != null) {
                timerService.interactiveNotification.setColorReady(colorReady);
            }
        } else if (key.equals(getString(R.string.pref_custom_color_done))) {
            colorDone = sharedPreferences.getInt(key, getColor(R.color.default_color_done));
            if (timerService != null) {
                timerService.interactiveNotification.setColorDone(colorDone);
            }
        } else {
            Log.e(TAG, "updatePreference: not supported preference key=" + key);
        }
        return false;
    }

    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                    Log.d(TAG, "onSharedPreferenceChanged: key=" + key);
                    if (updatePreference(key) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        timerService.interactiveNotification.updateNotificationChannels();
                    }
                }
            };
}
