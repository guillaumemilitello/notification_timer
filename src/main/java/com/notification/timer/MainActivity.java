package com.notification.timer;

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
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
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
import com.notification.timer.TimerService.TimerBinder;

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
    private static final float ALPHA_ENABLED = (float) 1.0;
    private static final float ALPHA_DISABLED = (float) 0.2;

    // Main user interface
    private Menu toolbarMenu;
    private TextView emptyPresetsTextView, setsTextView;
    private ProgressBar timerProgressBar;
    private ButtonsLayout buttonsLayout;
    private ButtonAction buttonLeftAction, buttonCenterAction, buttonRightAction;
    private ImageButton imageButtonLeft, imageButtonCenter, imageButtonRight;
    private ImageButton imageButtonTimerMinusMulti, imageButtonTimerPlusMulti;
    private ImageButton imageButtonTimerMinus, imageButtonTimerPlus, imageButtonKeepScreenOn;
    private LinearLayout mainLayout, bottomButtonsLayout, fullButtonsLayout;
    private RelativeLayout activityLayout, timerLayout;
    private FrameLayout presetsFrameLayout;

    private int activityLayoutWidth, activityLayoutHeight;
    private LayoutMode layoutMode;
    private TimerTextView timerTextViewLeft, timerTextViewSeparator, timerTextViewRight, timerTextViewSeconds;

    static Typeface typefaceLektonBold;

    static private float density;

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

    private HelpOverlay helpOverlay;

    // Settings
    static final long[] vibrationPattern = {0, 400, 200, 400,};

    // User preferences
    private boolean setsPickerEnable;
    private boolean vibrationEnable;
    private Uri ringtoneUri;
    private boolean timerGetReadyEnable;
    private int timerGetReady;
    private boolean vibrationEnableReady;
    private Uri ringtoneUriReady;
    private int colorRunning;
    private int colorReady;
    private int colorDone;

    public Preset getPresetUser() {
        return new Preset(timerUser, setsUser);
    }

    private SharedPreferences sharedPreferences;

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
        NEXT_SET("next_set"),
        NEXT_SET_DISABLED("next_set_disabled");

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

        private final String layout;

        ButtonsLayout(String layout) {
            this.layout = layout;
        }

        @Override
        public String toString() {
            return layout;
        }
    }

    // Multi window layout mode
    enum LayoutMode {

        TINY(0, "tiny"),
        COMPACT(1 , "compact"),
        FULL(2, "full");

        private final int index;
        private final String layout;

        LayoutMode(int index, String layout) {
            this.index = index;
            this.layout = layout;
        }

        public int getIndex() {
            return index;
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
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_main);

        // Update system color bar and icon for the system
        Toolbar toolbar = findViewById(R.id.actionBar);
        setSupportActionBar(toolbar);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        setTaskDescription(new ActivityManager.TaskDescription(getApplicationInfo().name,
                BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher),
                ContextCompat.getColor(this, R.color.colorPrimary)));
        if (toolbar != null) {
            toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.bpWhite));
        }

        density = getResources().getDisplayMetrics().density;

        timerTextViewLeft = new TimerTextView((TextView)findViewById(R.id.textViewTimerLeft));
        timerTextViewSeparator = new TimerTextView((TextView)findViewById(R.id.textViewTimerSeparator));
        timerTextViewRight = new TimerTextView((TextView)findViewById(R.id.textViewTimerRight));
        timerTextViewSeconds =  new TimerTextView((TextView)findViewById(R.id.textViewTimerSeconds));

        emptyPresetsTextView = findViewById(R.id.textViewEmptyPresets);
        setsTextView = findViewById(R.id.textViewSets);

        presetsFrameLayout = findViewById(R.id.frameLayoutPresets);
        activityLayout = findViewById(R.id.layoutActivity);
        timerLayout = findViewById(R.id.layoutTimer);
        mainLayout = findViewById(R.id.layoutMain);
        fullButtonsLayout = findViewById(R.id.layoutFullButtons);
        bottomButtonsLayout = findViewById(R.id.layoutBottomButtons);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            activityLayout.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    // Used for the free-form window resizing, small resize may not trigger onDestroy()
                    if (isInMultiWindowMode() && (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) &&
                            (left != 0 || top != 0 || right != 0 || bottom != 0) && (oldLeft != 0 || oldTop != 0 || oldRight != 0 || oldBottom != 0)) {
                        Log.d(TAG, "activityViewLayoutChanged: left=" + left + ", top=" + top + ", right=" + right + ", bottom=" + bottom);
                        // Some sizes may come incorrect resulting a broken UI, it is safer to recreate the activity
                        // This causes the activity to blink while resizing it in free-form window mode
                        // Blanking the activity for a better visual rather than call updateUserInterface(); scaleActivity(); updateColorLayout();
                        activityLayout.setVisibility(View.INVISIBLE);
                        recreate();
                    }
                }
            });
        }

        typefaceLektonBold = Typeface.createFromAsset(getAssets(), "fonts/Lekton-Bold.ttf");
        Typeface typefaceLekton = Typeface.createFromAsset(getAssets(), "fonts/Lekton-Regular.ttf");
        timerTextViewLeft.setTypeface(typefaceLektonBold);
        timerTextViewSeparator.setTypeface(typefaceLekton);
        timerTextViewRight.setTypeface(typefaceLekton);
        timerTextViewSeconds.setTypeface(typefaceLektonBold);

        setsTextView.setTypeface(typefaceLektonBold);

        AlertBuilderSetDone alertBuilderSetDone = new AlertBuilderSetDone(this);
        AlertBuilderAllSetsDone alertBuilderAllSetsDone = new AlertBuilderAllSetsDone(this);
        alertSetDone = alertBuilderSetDone.create();
        alertAllSetsDone = alertBuilderAllSetsDone.create();

        timerPickerBuilder = new MsPickerBuilder();
        timerPickerBuilder.setFragmentManager(getFragmentManager());
        timerPickerBuilder.setStyleResId(R.style.BetterPickersDialogFragment_Light);
        timerPickerBuilder.setTimeInSeconds(0);
        timerPickerBuilder.setTitleText(getString(R.string.picker_timer));

        setsPickerBuilder = new NumberPickerBuilder();
        setsPickerBuilder.setFragmentManager(getFragmentManager());
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
        startService(intent);
        bindService(intent, serviceConnection, Context.BIND_ABOVE_CLIENT);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);

        FragmentManager fragmentManager = getSupportFragmentManager();
        presetCardsList = new PresetCardsList();
        presetCardsList.initContext(this);
        presetCardsList.createPresetsList(sharedPreferences);
        fragmentManager.beginTransaction().replace(R.id.frameLayoutPresets, presetCardsList).commit();

        if (!timerServiceIsRunning()) {
            Log.d(TAG, "onCreate: starting service TimerService");
            startForegroundService(new Intent(getBaseContext(), TimerService.class));
        }

        helpOverlay = new HelpOverlay(this);
    }

    private void setKeepScreenOnStatus(boolean enable) {
        keepScreenOn = enable;
        Log.d(TAG, "setKeepScreenOnStatus: keepScreenOn=" + keepScreenOn);
        if (keepScreenOn) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            imageButtonKeepScreenOn.setImageResource(R.drawable.ic_screen_locked);
            if (toolbarMenu != null) {
                toolbarMenu.getItem(TOOLBAR_MENU_KEEPSCREENON_INDEX).setChecked(true);
            }
        } else {
            getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            imageButtonKeepScreenOn.setImageResource(R.drawable.ic_screen);
            if (toolbarMenu != null) {
                toolbarMenu.getItem(TOOLBAR_MENU_KEEPSCREENON_INDEX).setChecked(false);
            }
        }
    }

    private void scaleActivity() {
        if (updateLayoutMode()) {
            updatePresetsLayout();
            scaleTimerProgressBar();
            scaleLayouts();
            scaleTextViews();
            // force update center button action, add preset is only available in FULL mode
            updateButton(imageButtonCenter, buttonCenterAction);
        }
    }

    private boolean updateLayoutMode() {
        int width = activityLayout.getMeasuredWidth();
        int height = activityLayout.getMeasuredHeight();

        if (width == 0 || height == 0) {
            Log.e(TAG, "updateLayoutMode: invalid width=" + width + ", height=" + height);
            return false;
        }

        if (width == activityLayoutWidth && height == activityLayoutHeight) {
            Log.d(TAG, "updateLayoutMode: already set properly");
            return false;
        }

        Log.d(TAG, "updateLayoutMode: width=" + width + ", height=" + height);
        activityLayoutWidth = width;
        activityLayoutHeight = height;

        int fullModeThreshold = getResources().getDimensionPixelSize(R.dimen.full_mode_threshold);
        int compactModeThreshold = getResources().getDimensionPixelSize(R.dimen.compact_mode_threshold);
        layoutMode = height > fullModeThreshold ? LayoutMode.FULL : height > compactModeThreshold ? LayoutMode.COMPACT : LayoutMode.TINY;

        Log.d(TAG, "updateLayoutMode: height=" + height / density + "dp, layoutMode=" + layoutMode);
        return true;
    }

    private boolean isLayoutModeFull() {
        return layoutMode == LayoutMode.FULL;
    }

    private void scaleTimerProgressBar() {
        int timerProgressBarWidth = timerProgressBar.getMeasuredWidth();
        int timerProgressBarHeight = (int)(isLayoutModeFull() ? activityLayoutHeight - getResources().getDimension(R.dimen.preset_card_total_height) : activityLayoutHeight);
        float layoutScaleX = (float) timerProgressBarHeight / timerProgressBarWidth;
        Log.d(TAG, "scaleTimerProgressBar: layoutScaleX=" + layoutScaleX + ", timerProgressBarHeight=" + timerProgressBarHeight + ", timerProgressBarWidth=" + timerProgressBarWidth);
        timerProgressBar.setScaleX(layoutScaleX);
    }

    private void scaleLayouts() {
        int layoutWeight;
        switch (layoutMode) {
            case TINY:
                updateFullLayoutVisibility(View.GONE);
                setsTextView.setVisibility(View.GONE);
                layoutWeight = 5;
                break;
            case COMPACT:
                updateFullLayoutVisibility(View.GONE);
                setsTextView.setVisibility(View.VISIBLE);
                layoutWeight = 4;
                break;
            default:
            case FULL:
                updateFullLayoutVisibility(View.VISIBLE);
                setsTextView.setVisibility(View.VISIBLE);
                layoutWeight = 3;
                break;
        }
        LinearLayout.LayoutParams timerLayoutParams = (LinearLayout.LayoutParams) timerLayout.getLayoutParams();
        timerLayoutParams.weight = layoutWeight;
        Log.d(TAG, "scaleLayouts: layoutMode=" + layoutMode + ", layoutWeight=" + layoutWeight);
    }

    private void scaleTextViews() {
        int timerLayoutWidth = timerLayout.getMeasuredWidth();
        int timerLayoutHeight;

        int fullButtonsLayoutHeight = fullButtonsLayout.getMeasuredHeight();
        int bottomButtonsLayoutHeight = bottomButtonsLayout.getMeasuredHeight();
        int setsTextViewHeight = setsTextView.getMeasuredHeight();
        switch (layoutMode) {
            case TINY:
                timerLayoutHeight = activityLayoutHeight - bottomButtonsLayoutHeight;
                break;
            case COMPACT:
                timerLayoutHeight = activityLayoutHeight - bottomButtonsLayoutHeight - setsTextViewHeight;
                break;
            default:
            case FULL:
                timerLayoutHeight = activityLayoutHeight - bottomButtonsLayoutHeight - fullButtonsLayoutHeight - setsTextViewHeight;
                break;
        }

        // TimerTextViewParams generates parameters for the Lekton typeface
        TimerTextViewParameters timerTextViewParams = new TimerTextViewParameters(layoutMode, timerLayoutWidth, timerLayoutHeight, density, this, sharedPreferences);
        Log.d(TAG, "scaleTextViews: timerTextViewParams=" + timerTextViewParams);

        timerTextViewLeft.setParameters(timerTextViewParams, false);
        timerTextViewSeparator.setParameters(timerTextViewParams, true);
        timerTextViewRight.setParameters(timerTextViewParams, false);
        timerTextViewSeconds.setParameters(timerTextViewParams, false);

        updateTimerDisplay();

        float setsLayoutHeight = setsTextView.getMeasuredHeight() / density;
        // Threshold are fixed for the Typeface Lekton
        float setsTextSize = isLayoutModeFull() ? setsLayoutHeight / 2 : setsLayoutHeight / 1.3f;
        Log.d(TAG, "scaleTextViews: setsLayoutHeight=" + setsLayoutHeight + ", setsTextSize=" + setsTextSize);
        setsTextView.setTextSize(setsTextSize);
    }

    private void updateFullLayoutVisibility(int visible) {
        fullButtonsLayout.setVisibility(visible);
        if (visible == View.VISIBLE) {
            imageButtonTimerMinusMulti.setVisibility(View.GONE);
            imageButtonTimerPlusMulti.setVisibility(View.GONE);
            bottomButtonsLayout.setWeightSum(5);
        } else {
            imageButtonTimerMinusMulti.setVisibility(View.VISIBLE);
            imageButtonTimerPlusMulti.setVisibility(View.VISIBLE);
            bottomButtonsLayout.setWeightSum(7);
        }
    }

    void updatePresetsVisibility() {
        Log.d(TAG, "updatePresetsVisibility: layoutMode=" + layoutMode);
        setPresetsVisible(isLayoutModeFull());
        updatePresetsExpandButton(false);
    }

    private void setPresetsVisible(boolean visible) {
        Log.d(TAG, "setPresetsVisible: visible=" + visible);
        if (timerState == TimerService.State.WAITING && presetCardsList.isEmpty()) {
            emptyPresetsTextView.setVisibility(visible ? View.VISIBLE : View.GONE);
            presetsFrameLayout.setVisibility(View.GONE);
        } else {
            emptyPresetsTextView.setVisibility(View.GONE);
            presetsFrameLayout.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private boolean isPresetsVisible() {
        if (presetCardsList.isEmpty()) {
            return emptyPresetsTextView.getVisibility() == View.VISIBLE;
        } else {
            return presetsFrameLayout.getVisibility() == View.VISIBLE;
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: timerService=" + timerService + ", timerServiceBound=" + timerServiceBound);

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

        if (sharedPreferences.getBoolean("firstRun", true)) {
            Log.d(TAG, "onStart: firstRun=true");
            helpOverlay.show();
            sharedPreferences.edit().putBoolean("firstRun", false).apply();
        }
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
        scaleActivity();
        updateColorLayout();
    }

    @Override
    public void onDialogMsSet(int reference, boolean isNegative, int minutes, int seconds) {
        long timer = minutes * 60 + seconds;
        timerCurrent = timer;
        timerUser = timer;
        Log.d(TAG, "onDialogMsSet: timerUser=" + timerUser);
        timerProgressBar.setMax((int) timerUser);
        updateServiceTimers();
        if (setsPickerEnable) {
            updateButtonsLayout(ButtonsLayout.WAITING_SETS);
            setsPickerBuilder.show();
        } else {
            setsCurrent = 1;
            setsUser = Integer.MAX_VALUE;
            Log.d(TAG, "onDialogMsSet: setsUser=" + setsUser + ", setsCurrent=" + setsCurrent);
            updateSetsDisplay();
            terminatePickers();
        }
    }

    @Override
    public void onDialogNumberSet(int reference, BigInteger number, double decimal, boolean isNegative, BigDecimal fullNumber) {
        int sets = number.intValue();
        setsCurrent = 1;
        setsUser = sets;
        Log.d(TAG, "onDialogNumberSet: setsUser=" + setsUser + ", setsCurrent=" + setsCurrent);
        updateSetsDisplay();
        terminatePickers();
    }

    @SuppressWarnings("deprecation")
    private void updateTimerDisplay() {
        // TODO : merge with Preset class
        if (timerCurrent >= 60) {
            timerTextViewLeft.setVisibility(View.VISIBLE);
            timerTextViewSeparator.setVisibility(View.VISIBLE);
            timerTextViewRight.setVisibility(View.VISIBLE);
            timerTextViewSeconds.setVisibility(View.GONE);

            int digits = timerCurrent >= 600 ? 4 : 3;
            timerTextViewLeft.setDigits(digits);
            timerTextViewSeparator.setDigits(digits);
            timerTextViewRight.setDigits(digits);

            timerTextViewLeft.setText(String.format(Locale.US, "%d", timerCurrent / 60));
            timerTextViewRight.setText(String.format(Locale.US, "%02d", timerCurrent % 60));
        } else {
            timerTextViewLeft.setVisibility(View.GONE);
            timerTextViewSeparator.setVisibility(View.GONE);
            timerTextViewRight.setVisibility(View.GONE);
            timerTextViewSeconds.setVisibility(View.VISIBLE);

            timerTextViewSeconds.setDigits(2);

            timerTextViewSeconds.setText(String.format(Locale.US, "%d", timerCurrent % 60));
        }
        timerProgressBar.setMax((int) timerUser);
        timerProgressBar.setProgress((int) (timerUser - timerCurrent));
    }

    private void updateColorLayout() {
        int progressColor = Color.WHITE, backgroundColor = Color.WHITE;
        switch (buttonsLayout) {
            case READY:
                break;
            case PAUSED:
            case RUNNING:
                if (timerGetReadyEnable && timerCurrent <= timerGetReady && timerUser > timerGetReady) {
                    progressColor = colorReady;
                } else {
                    progressColor = colorRunning;
                }
                break;
            case STOPPED:
                progressColor = colorDone;
                break;
            default:
            case WAITING:
            case WAITING_SETS:
                backgroundColor = ContextCompat.getColor(this, R.color.preset_card_add_background);
                progressColor = backgroundColor;
                timerProgressBar.setMax(1);
                timerProgressBar.setProgress(1);
                break;
        }
        timerProgressBar.setProgressBackgroundTintList(ColorStateList.valueOf(backgroundColor));
        timerProgressBar.setProgressTintList(ColorStateList.valueOf(progressColor));

        int textColor = ContextCompat.getColor(this, R.color.timer_font_color);
        timerTextViewLeft.setTextColor(textColor);
        timerTextViewSeparator.setTextColor(textColor);
        timerTextViewRight.setTextColor(textColor);
        timerTextViewSeconds.setTextColor(textColor);
        setsTextView.setTextColor(textColor);
    }

    private void updateSetsDisplay() {
        Log.d(TAG, "updateSetsDisplay: buttonsLayout=" + buttonsLayout);
        if (buttonsLayout != ButtonsLayout.STOPPED) {
            if (buttonsLayout != ButtonsLayout.WAITING && buttonsLayout != ButtonsLayout.WAITING_SETS) {
                if (setsUser == Integer.MAX_VALUE || setsCurrent > setsUser) {
                    setsTextView.setText(String.format(Locale.US, "%d", setsCurrent));
                } else {
                    setsTextView.setText(String.format(Locale.US, "%d", setsUser - setsCurrent + 1));
                }
            } else {
                setsTextView.setText("0");
            }
        }
    }

    private void terminatePickers() {
        timerState = TimerService.State.READY;
        updateServiceState();
        updateButtonsLayout();
        updatePresetsVisibility();
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

    void start() {
        timerState = TimerService.State.RUNNING;
        Log.d(TAG, "start: timerState=" + timerState);
        updateButtonsLayout();
    }

    void pause() {
        timerState = TimerService.State.PAUSED;
        Log.d(TAG, "pause: timerState=" + timerState);
        updateButtonsLayout();
    }

    void resume() {
        timerState = TimerService.State.RUNNING;
        Log.d(TAG, "resume: timerState=" + timerState);
        updateButtonsLayout();
    }

    void stop() {
        timerState = TimerService.State.READY;
        Log.d(TAG, "stop: timerState=" + timerState + ", setsCurrent=" + setsCurrent);
        updateButtonsLayout(ButtonsLayout.READY);
    }

    void nextSet() {
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

    void clear() {
        timerState = TimerService.State.WAITING;
        Log.d(TAG, "clear: timerState=" + timerState);

        timerUser = 0;
        timerCurrent = 0;
        setsCurrent = 0;
        setsUser = 0;

        updateButtonsLayout();
        updatePresetsVisibility();
    }

    void reset() {
        timerState = TimerService.State.READY;
        Log.d(TAG, "reset: timerState=" + timerState);

        updateButtonsLayout(ButtonsLayout.READY);
    }

    void extraSet() {
        Log.d(TAG, "extraSet: setsCurrent=" + setsCurrent);

        updateButtonsLayout(ButtonsLayout.RUNNING);
    }

    void timerMinus() {
        updateTimerDisplay();
        updateTimerButtons();
        updateColorLayout();
    }

    void timerPlus() {
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

    void setsPlus() {
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

    void setsUpdate(int sets) {
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
                    if (vibrationEnableReady) {
                        vibrate();
                    }
                }
                timerCurrent = time;
                updateTimerDisplay();
                updateTimerButtons();
                updateColorLayout();
            }
        }
    }

    void done() {
        // The timer will be stopped from the alerts
        if (++setsCurrent <= setsUser) {
            Log.d(TAG, "done: setsCurrent=" + setsCurrent);
            if (mainActivityVisible) {
                updateTimerDone();
                alertSetDone.show();
            }
        } else {
            Log.d(TAG, "done: all sets done, setsCurrent=" + setsCurrent);
            if (mainActivityVisible) {
                updateTimerDone();
                alertAllSetsDone.show();
            }
        }
        updateButtonsLayout(ButtonsLayout.STOPPED);
    }

    private void updateTimerDone() {
        timerCurrent = 0;
        updateTimerDisplay();
        updateTimerButtons();
        updateColorLayout();
        ring(ringtoneUri);
        if (vibrationEnable) {
            vibrate();
        }
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
            Intent sendEmail = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto","guillaume.militello@gmail.com", null));
            sendEmail.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.app_name) + " feedback");
            startActivity(Intent.createChooser(sendEmail, "Send email..."));
            return true;
        } else if (id == R.id.help) {
            helpOverlay.show();
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
        // Update the preset in case of preferences restore
        presetCardsList.updateFromPreferences();
        updatePresetsVisibility();
    }

    private void vibrate() {
        Log.i(TAG, "vibrate");
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            vibrator.vibrate(vibrationPattern, -1);
        }
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
            imageButtonTimerPlus.setEnabled(true);
            imageButtonTimerPlus.setAlpha(ALPHA_ENABLED);
            imageButtonTimerPlusMulti.setEnabled(true);
            imageButtonTimerPlusMulti.setAlpha(ALPHA_ENABLED);
        } else {
            imageButtonTimerMinus.setEnabled(false);
            imageButtonTimerMinus.setAlpha(ALPHA_DISABLED);
            imageButtonTimerMinusMulti.setEnabled(false);
            imageButtonTimerMinusMulti.setAlpha(ALPHA_DISABLED);
            imageButtonTimerPlus.setEnabled(false);
            imageButtonTimerPlus.setAlpha(ALPHA_DISABLED);
            imageButtonTimerPlusMulti.setEnabled(false);
            imageButtonTimerPlusMulti.setAlpha(ALPHA_DISABLED);
        }
    }

    static int getTimerMinusResId(long timerMinus) {
        if (timerMinus == 10) {
            return R.drawable.ic_timer_minus_10;
        } else if (timerMinus == 15) {
            return R.drawable.ic_timer_minus_15;
        } else if (timerMinus == 20) {
            return R.drawable.ic_timer_minus_20;
        } else if (timerMinus == 30) {
            return R.drawable.ic_timer_minus_30;
        } else if (timerMinus == 45) {
            return R.drawable.ic_timer_minus_45;
        } else {
            return R.drawable.ic_timer_minus_60;
        }
    }

    static int getTimerPlusResId(long timerPlus) {
        if (timerPlus == 10) {
            return R.drawable.ic_timer_plus_10;
        } else if (timerPlus == 15) {
            return R.drawable.ic_timer_plus_15;
        } else if (timerPlus == 20) {
            return R.drawable.ic_timer_plus_20;
        } else if (timerPlus == 30) {
            return R.drawable.ic_timer_plus_30;
        } else if (timerPlus == 45) {
            return R.drawable.ic_timer_plus_45;
        } else {
            return R.drawable.ic_timer_plus_60;
        }
    }

    private void changePresetsFrameLayout() {
        if (presetsFrameLayout != null) {
            if (isPresetsVisible()) {
                Log.d(TAG, "changePresetsFrameLayout: setVisibility=invisible");
                setPresetsVisible(false);
                updatePresetsExpandButton(false);
            } else {
                Log.d(TAG, "changePresetsFrameLayout: setVisibility=visible");
                setPresetsVisible(true);
                updatePresetsExpandButton(true);
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

    private void updatePresetsLayout() {
        if (presetsFrameLayout != null) {
            Log.d(TAG, "updatePresetsLayout: layoutMode=" + layoutMode);
            setPresetsVisible(isLayoutModeFull());
            updateToolBarMenuItems(!isLayoutModeFull());
            int backgroundColor, topMargin = 0;
            if (isLayoutModeFull()) {
                backgroundColor = ContextCompat.getColor(this, R.color.preset_fragment_background);
                topMargin = getResources().getDimensionPixelSize(R.dimen.preset_card_total_height);
            } else {
                backgroundColor = ContextCompat.getColor(this, R.color.preset_fragment_transparent_background);
            }
            presetsFrameLayout.setBackgroundColor(backgroundColor);
            emptyPresetsTextView.setBackgroundColor(backgroundColor);
            RelativeLayout.LayoutParams mainLayoutParams = (RelativeLayout.LayoutParams) mainLayout.getLayoutParams();
            RelativeLayout.LayoutParams timerProgressBarLayoutParams = (RelativeLayout.LayoutParams) timerProgressBar.getLayoutParams();
            mainLayoutParams.setMargins(0, topMargin, 0, 0);
            timerProgressBarLayoutParams.setMargins(0, topMargin, 0, 0);
        }
    }

    private void updatePresetsExpandButton(boolean expand) {
        if (toolbarMenu != null) {
            if (expand) {
                toolbarMenu.getItem(TOOLBAR_MENU_PRESET_INDEX).setIcon(getDrawable(R.drawable.ic_action_up));
            } else {
                toolbarMenu.getItem(TOOLBAR_MENU_PRESET_INDEX).setIcon(getDrawable(R.drawable.ic_action_down));
            }
        }
    }

    private void updateButtonsLayout() {
        ButtonsLayout layout = ButtonsLayout.valueOf(timerState.toString().toUpperCase(Locale.US));
        if (layout == ButtonsLayout.WAITING && timerUser > 0) {
            layout = ButtonsLayout.WAITING_SETS;
        }
        if (timerState != TimerService.State.WAITING && timerCurrent == 0 && setsCurrent == 0) {
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

        // Some elements are INVISIBLE by default in xml to enhance the multi window resize
        bottomButtonsLayout.setVisibility(View.VISIBLE);
        timerProgressBar.setVisibility(View.VISIBLE);

        updateSetsButtons();
        updateTimerButtons();
        updateTimerDisplay();
        updateSetsDisplay();
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
                button.setImageResource(R.drawable.ic_add_circle);
                button.setEnabled(true);
                if (activityLayoutHeight < getResources().getDimensionPixelSize(R.dimen.pickers_threshold)) {
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
                button.setImageResource(R.drawable.ic_play_circle);
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
                button.setImageResource(R.drawable.ic_pause_circle);
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
                button.setImageResource(R.drawable.ic_play_circle);
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
                button.setImageResource(R.drawable.ic_trash);
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
                button.setImageResource(R.drawable.ic_trash);
                button.setAlpha(ALPHA_DISABLED);
                return true;
            case NEXT_SET:
                button.setEnabled(true);
                button.setImageResource(R.drawable.ic_chevron_right);
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
                button.setImageResource(R.drawable.ic_chevron_right);
                button.setAlpha(ALPHA_DISABLED);
                return true;
            case RESET:
                button.setEnabled(true);
                button.setImageResource(R.drawable.ic_chevrons_left);
                button.setAlpha(ALPHA_ENABLED);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        sendBroadcast(new Intent(IntentAction.RESET));
                    }
                });
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

        if (alertSetDone.isShowing()){
            alertSetDone.dismiss();
        }
        if (alertAllSetsDone.isShowing()){
            alertAllSetsDone.dismiss();
        }

        unregisterReceiver(mainActivityReceiver);
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        String str = "";
        switch (level) {
            case TRIM_MEMORY_RUNNING_MODERATE:
                str = "RUNNING_MODERATE";
                break;
            case TRIM_MEMORY_RUNNING_LOW:
                str = "RUNNING_LOW";
                break;
            case TRIM_MEMORY_RUNNING_CRITICAL:
                str = "RUNNING_CRITICAL, finishing activity";
                if (!mainActivityVisible) {
                    finish();
                }
                break;
            case TRIM_MEMORY_UI_HIDDEN:
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
            scaleActivity();
            updateColorLayout();
        }
    };

    private void updateAllPreferences() {
        Log.d(TAG, "updateAllPreferences");
        Map<String, ?> preferences = sharedPreferences.getAll();
        if (preferences != null) {
            // Force default values
            updatePreference(getString(R.string.pref_timer_minus));
            updatePreference(getString(R.string.pref_timer_plus));
            updatePreference(getString(R.string.pref_sets_picker_enable));
            updatePreference(getString(R.string.pref_vibrate));
            updatePreference(getString(R.string.pref_ringtone_uri));
            updatePreference(getString(R.string.pref_timer_get_ready_enable));
            updatePreference(getString(R.string.pref_timer_get_ready));
            updatePreference(getString(R.string.pref_timer_get_ready_vibrate));
            updatePreference(getString(R.string.pref_timer_get_ready_ringtone_uri));
            updatePreference(getString(R.string.pref_custom_color_running));
            updatePreference(getString(R.string.pref_custom_color_ready));
            updatePreference(getString(R.string.pref_custom_color_done));
            if (timerService != null) {
                // Propagate the preferences to the notification
                timerService.updateAllPreferences();
            }
        }
    }

    private boolean updatePreference(String key) {

        if (!isKeyPreference(key)) {
            return false;
        }

        Log.d(TAG, "updatePreference: key=" + key);

        if (key.equals(getString(R.string.pref_timer_minus))) {
            long timerMinus = Long.parseLong(sharedPreferences.getString(key, getString(R.string.default_timer_minus)));
            int resId = getTimerMinusResId(timerMinus);
            imageButtonTimerMinus.setImageResource(resId);
            imageButtonTimerMinusMulti.setImageResource(resId);
        } else if (key.equals(getString(R.string.pref_timer_plus))) {
            long timerPlus = Long.parseLong(sharedPreferences.getString(key, getString(R.string.default_timer_plus)));
            int resId = getTimerPlusResId(timerPlus);
            imageButtonTimerPlus.setImageResource(resId);
            imageButtonTimerPlusMulti.setImageResource(resId);
        } else if (key.equals(getString(R.string.pref_sets_picker_enable))) {
            setsPickerEnable = sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.default_sets_picker));
        } else if (key.equals(getString(R.string.pref_vibrate))) {
            vibrationEnable = sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.default_vibrate));
        } else if (key.equals(getString(R.string.pref_ringtone_uri))) {
            ringtoneUri = Uri.parse(sharedPreferences.getString(key, getString(R.string.default_ringtone_uri)));
        } else if (key.equals(getString(R.string.pref_timer_get_ready_enable))) {
            timerGetReadyEnable = sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.default_timer_get_ready_enable));
        } else if (key.equals(getString(R.string.pref_timer_get_ready))) {
            timerGetReady = Integer.parseInt(sharedPreferences.getString(key, getString(R.string.default_timer_get_ready)));
        } else if (key.equals(getString(R.string.pref_timer_get_ready_vibrate))) {
            vibrationEnableReady = sharedPreferences.getBoolean(key, getResources().getBoolean(R.bool.default_timer_get_ready_vibrate));
        } else if (key.equals(getString(R.string.pref_timer_get_ready_ringtone_uri))) {
            ringtoneUriReady = Uri.parse(sharedPreferences.getString(key, getString(R.string.default_timer_get_ready_ringtone_uri)));
        } else if (key.equals(getString(R.string.pref_custom_color_running))) {
            colorRunning = sharedPreferences.getInt(key, ContextCompat.getColor(this, R.color.default_color_running));
        } else if (key.equals(getString(R.string.pref_custom_color_ready))) {
            colorReady = sharedPreferences.getInt(key, ContextCompat.getColor(this, R.color.default_color_ready));
        } else if (key.equals(getString(R.string.pref_custom_color_done))) {
            colorDone = sharedPreferences.getInt(key, ContextCompat.getColor(this, R.color.default_color_done));
        } else {
            Log.e(TAG, "updatePreference: not supported preference key=" + key);
            return false;
        }
        return true;
    }

    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                    Log.d(TAG, "onSharedPreferenceChanged: key=" + key);
                    if (updatePreference(key) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        timerService.interactiveNotification.updateNotificationChannels();
                    }
                }
            };

    private boolean isKeyPreference(String key) {
        return !key.contains(getString(R.string.pref_preset_array)) && !key.contains(getString(R.string.pref_timer_service))
                && !key.contains(getString(R.string.pref_timer_text));
    }
}
