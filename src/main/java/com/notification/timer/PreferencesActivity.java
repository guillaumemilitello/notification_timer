package com.notification.timer;


import android.Manifest;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.preference.SwitchPreference;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import android.provider.Settings;
import android.text.Html;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.codetroopers.betterpickers.hmspicker.HmsPickerBuilder;
import com.codetroopers.betterpickers.hmspicker.HmsPickerDialogFragment;
import com.kizitonwose.colorpreference.ColorPreference;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static android.content.pm.PackageManager.PERMISSION_DENIED;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class PreferencesActivity extends AppCompatPreferenceActivity implements HmsPickerDialogFragment.HmsPickerDialogHandlerV2 {

    private static final String TAG = "PreferencesActivity";

    private static SharedPreferences sharedPreferences;

    private static int dayNightMode;

    private boolean restoringPreferences;

    // Already used WRITE_PERMISSION_REQUEST_CODE = 1;
    // Already used READ_PERMISSION_REQUEST_CODE = 2;
    private static final int CREATE_BACKUP_REQUEST_CODE = 3;
    private static final int RESTORE_BACKUP_REQUEST_CODE = 4;
    private static final int NOTIFICATION_SETTINGS_REQUEST_CODE = 100;

    private TimerPreferenceFragment settingsFragment;

    private NotificationManager notificationManager;

    private HmsPickerBuilder timerGetReadyPickerBuilder;

    private static final int DONE_CHANNEL_ACTIVITY_REQUEST = 51;
    private static final int READY_CHANNEL_ACTIVITY_REQUEST = 52;
    private static String doneChannelUriString = "";
    private static boolean doneChannelVibrate = false;
    private static String readyChannelUriString = "";
    private static boolean readyChannelVibrate = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preferences_actionbar);

        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (windowInsetsController != null) {
            boolean isDark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
            windowInsetsController.setAppearanceLightStatusBars(!isDark);
            windowInsetsController.setAppearanceLightNavigationBars(!isDark);
        }

        // Update system color bar and icon for the system
        Toolbar toolbar = findViewById(R.id.preferences_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        setTaskDescription(new ActivityManager.TaskDescription(getApplicationInfo().name,
                BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher),
                ContextCompat.getColor(this, R.color.colorPrimary)));

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, 0);

            if (toolbar != null) {
                toolbar.setPadding(toolbar.getPaddingLeft(), systemBars.top, toolbar.getPaddingRight(), toolbar.getPaddingBottom());
            }

            View contentFrame = findViewById(R.id.content_frame);
            if (contentFrame != null) {
                contentFrame.setPadding(contentFrame.getPaddingLeft(), contentFrame.getPaddingTop(),
                        contentFrame.getPaddingRight(), systemBars.bottom);
            }

            View listView = findViewById(android.R.id.list);
            if (listView != null) {
                listView.setPadding(listView.getPaddingLeft(), listView.getPaddingTop(),
                        listView.getPaddingRight(), systemBars.bottom);
                // Ensure the list items don't get cut off
                if (listView instanceof android.widget.ListView) {
                    ((android.widget.ListView) listView).setClipToPadding(false);
                }
            }
            return WindowInsetsCompat.CONSUMED;
        });

        timerGetReadyPickerBuilder = new HmsPickerBuilder();
        timerGetReadyPickerBuilder.setFragmentManager(getFragmentManager());
        timerGetReadyPickerBuilder.setStyleResId(R.style.BetterPickersDialogFragment);
        timerGetReadyPickerBuilder.setTimeInSeconds(0);
        timerGetReadyPickerBuilder.setTitleText(getString(R.string.picker_timer_get_ready));

        settingsFragment = new TimerPreferenceFragment();
        getFragmentManager().beginTransaction().replace(R.id.content_frame, settingsFragment).commit();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager = (NotificationManager) getBaseContext().getSystemService(Context.NOTIFICATION_SERVICE);
        }

        restoringPreferences = false;
    }

    @SuppressWarnings("deprecation")
    public static class TimerPreferenceFragment extends PreferenceFragment
    {
        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
        }
    }

    @Override
    protected void onPostResume() {
        Log.d(TAG, "onPostResume");
        super.onPostResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            updateDoneNotificationChannelPreferences();
            updateReadyNotificationChannelPreferences();
            createPreferenceIntents();
        }
        updateSummaries();
        updateStepTimePreference();
        updateBackupPreferences();
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void createPreferenceIntents() {
        Preference doneChannelPreference = settingsFragment.findPreference(getString(R.string.pref_done_channel));
        doneChannelPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                openChannelSettings(InteractiveNotification.getDoneChannelId(), DONE_CHANNEL_ACTIVITY_REQUEST);
                return true;
            }
        });
        Preference readyChannelPreference = settingsFragment.findPreference(getString(R.string.pref_ready_channel));
        readyChannelPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                openChannelSettings(InteractiveNotification.getReadyChannelId(), READY_CHANNEL_ACTIVITY_REQUEST);
                return true;
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Preference notificationPermissionPreference = settingsFragment.findPreference(getString(R.string.pref_notification_permission));
            notificationPermissionPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    int notificationPermissionState = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.POST_NOTIFICATIONS);
                    if (notificationPermissionState == PackageManager.PERMISSION_DENIED) {
                        Log.d(TAG, "onPreferenceClick: request notification permission");
                        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                        intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                        startActivityForResult(intent, NOTIFICATION_SETTINGS_REQUEST_CODE);
                    }
                    return true;
                }
            });
        }
    }

    private void updateAllPreferences() {
        for (Map.Entry<String, ?> preference : sharedPreferences.getAll().entrySet()) {
            String key = preference.getKey();
            if (isKeyPreference(getBaseContext(), key)) {
                Preference settingsPreference = settingsFragment.findPreference(key);
                if (settingsPreference != null) {
                    Log.d(TAG, "updatePreference: key=" + key);
                    updatePreference(settingsPreference, preference.getValue());
                }
            }
        }
        updateSummaries();
    }

    private void updatePreference(Preference preference, Object newVal) {
        Log.d(TAG, "updatePreference: key=" + preference.getKey() + ", newVal=" + newVal);
        if (preference instanceof SwitchPreference) {
            SwitchPreference switchPreference = (SwitchPreference) preference;
            switchPreference.setChecked((boolean) newVal);
        } else if (preference instanceof ColorPreference) {
            ColorPreference colorPreference = (ColorPreference) preference;
            colorPreference.setValue((int) newVal);
        } else if (preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) preference;
            listPreference.setValue((String) newVal);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    @Override
    @TargetApi(Build.VERSION_CODES.O)
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == DONE_CHANNEL_ACTIVITY_REQUEST) {
            updateDoneNotificationChannelPreferences();
        }
        if (requestCode == READY_CHANNEL_ACTIVITY_REQUEST) {
            updateReadyNotificationChannelPreferences();
        }
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            if (requestCode == CREATE_BACKUP_REQUEST_CODE) {
                performSaveSharedPreferencesToUri(data.getData());
            } else if (requestCode == RESTORE_BACKUP_REQUEST_CODE) {
                performLoadSharedPreferencesFromUri(data.getData());
            }
        }
    }

    @Override
    public void onDialogHmsSet(int reference, boolean isNegative, int hours, int minutes, int seconds) {
        Log.d(TAG, "onDialogMsSet: hours=" + hours + ", minutes=" + minutes + ", seconds=" + seconds);
        int timerGetReady = hours * 3600 + minutes * 60 + seconds; // TODO: use time lib
        SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
        sharedPreferencesEditor.putString(getString(R.string.pref_timer_get_ready), String.valueOf(timerGetReady));
        sharedPreferencesEditor.apply();
        updateTimerGetReadySummary(timerGetReady);
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void openChannelSettings(String channelId, int requestCode) {
        Log.d(TAG, "openChannelSettings: channelId=" + channelId);
        Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        intent.putExtra(Settings.EXTRA_CHANNEL_ID, channelId);
        startActivityForResult(intent, requestCode);
    }

    private void updateTimerGetReadySummary() {
        int timerGetReady = Integer.parseInt(sharedPreferences.getString(getString(R.string.pref_timer_get_ready), getString(R.string.default_timer_get_ready)));
        updateTimerGetReadySummary(timerGetReady);
    }

    private void updateTimerGetReadySummary(int timerGetReady) {
        String summary = String.format(Locale.US, getString(R.string.get_ready_time_summary), timerGetReady / 60, timerGetReady % 60);
        settingsFragment.findPreference(getString(R.string.pref_timer_get_ready)).setSummary(summary);
        Log.d(TAG, "updateTimerGetReadySummary: summary=" + summary);
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void updateDoneNotificationChannelPreferences() {
        List<NotificationChannel> notificationChannelList = notificationManager.getNotificationChannels();
        for (NotificationChannel notificationChannel : notificationChannelList) {
            if (notificationChannel.getId().equals(InteractiveNotification.getDoneChannelId())) {
                Log.d(TAG, "updateNotificationChannelPreferences: notificationId=" + notificationChannel.getId());
                doneChannelVibrate = notificationChannel.shouldVibrate();
                doneChannelUriString = getUriString(notificationChannel);
                final boolean lightColorEnable = notificationChannel.shouldShowLights();
                final int lightColor = notificationChannel.getLightColor();

                SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
                sharedPreferencesEditor.putBoolean(getString(R.string.pref_vibrate), doneChannelVibrate);
                sharedPreferencesEditor.putString(getString(R.string.pref_ringtone_uri), doneChannelUriString);
                sharedPreferencesEditor.putBoolean(getString(R.string.pref_light_color_enable), lightColorEnable);
                sharedPreferencesEditor.putInt(getString(R.string.pref_light_color), lightColor);
                sharedPreferencesEditor.apply();

                Log.d(TAG, "updateDoneNotificationChannelPreferences: key=" + getString(R.string.pref_vibrate) + ", bool=" + doneChannelVibrate);
                Log.d(TAG, "updateDoneNotificationChannelPreferences: key=" + getString(R.string.pref_ringtone_uri) + ", string=" + doneChannelUriString);
                Log.d(TAG, "updateDoneNotificationChannelPreferences: key=" + getString(R.string.pref_light_color_enable) + ", bool=" + lightColorEnable);
                Log.d(TAG, "updateDoneNotificationChannelPreferences: key=" + getString(R.string.pref_light_color) + ", color=" + lightColor);
                return;
            }
        }
        Log.e(TAG, "updateDoneNotificationChannelPreferences: channel not found");
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void updateReadyNotificationChannelPreferences() {
        List<NotificationChannel> notificationChannelList = notificationManager.getNotificationChannels();
        for (NotificationChannel notificationChannel : notificationChannelList) {
            if (notificationChannel.getId().equals(InteractiveNotification.getReadyChannelId())) {
                Log.d(TAG, "updateReadyNotificationChannelPreferences: notificationId=" + notificationChannel.getId());
                readyChannelVibrate = notificationChannel.shouldVibrate();
                readyChannelUriString = getUriString(notificationChannel);

                SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
                sharedPreferencesEditor.putBoolean(getString(R.string.pref_timer_get_ready_vibrate), readyChannelVibrate);
                sharedPreferencesEditor.putString(getString(R.string.pref_timer_get_ready_ringtone_uri), readyChannelUriString);
                sharedPreferencesEditor.apply();

                Log.d(TAG, "updateReadyNotificationChannelPreferences: key=" + getString(R.string.pref_timer_get_ready_vibrate) + ", bool=" + readyChannelVibrate);
                Log.d(TAG, "updateReadyNotificationChannelPreferences: key=" + getString(R.string.pref_timer_get_ready_ringtone_uri) + ", string=" + readyChannelUriString);
                return;
            }
        }
        Log.e(TAG, "updateReadyNotificationChannelPreferences: channel not found");
    }

    @TargetApi(Build.VERSION_CODES.O)
    private String getUriString(NotificationChannel notificationChannel) {
        Uri uri = notificationChannel.getSound();
        if (uri == null) {
            return getString(R.string.default_ringtone_uri);
        }
        return uri.toString();
    }

    private void updateBackupPreferences() {
        settingsFragment.findPreference(getString(R.string.pref_timer_get_ready)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Log.d(TAG, "TimerGetReady");
                timerGetReadyPickerBuilder.show();
                return true;
            }
        });
        settingsFragment.findPreference(getString(R.string.pref_backup)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Log.d(TAG, "PreferencesBackup");
                saveSharedPreferencesToFile();
                return true;
            }
        });
        settingsFragment.findPreference(getString(R.string.pref_restore)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Log.d(TAG, "PreferencesRestore");
                loadSharedPreferencesFromFile();
                return true;
            }
        });
    }

    private void updateStepTimePreference() {
        ListPreference listPreference = (ListPreference) settingsFragment.findPreference(getString(R.string.pref_step_time));
        Long stepTime = Long.parseLong(listPreference.getValue());
        Log.d(TAG, "updateStepTimePreference: stepTime=" + stepTime + ", str=" + listPreference.getValue());
        if (stepTime < 0) {
            listPreference.setSummary(String.format(Locale.US, getString(R.string.step_time_decrement), -stepTime));
        } else {
            listPreference.setSummary(String.format(Locale.US, getString(R.string.step_time_increment), stepTime));
        }
    }

    private void updateSummaries() {
        Log.d(TAG, "updateSummaries");
        if (sharedPreferences != null) {
            for (Map.Entry<String, ?> preference : sharedPreferences.getAll().entrySet()) {
                String key = preference.getKey();
                if (isKeyPreference(getBaseContext(), key)) {
                    Log.d(TAG, "updateSummaries: key=" + key);
                    updateSummary(key);
                }
            }
        }
        updateTimerGetReadySummary();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            updateSummaryNotificationChannelIntent(getString(R.string.pref_done_channel), doneChannelUriString, doneChannelVibrate);
            updateSummaryNotificationChannelIntent(getString(R.string.pref_ready_channel), readyChannelUriString, readyChannelVibrate);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            updateSummaryNotificationPermissionIntent(getString(R.string.pref_notification_permission));
        }
    }

    private void updateSummary(String key) {
        Preference preference = settingsFragment.findPreference(key);
        if(preference != null) {
            if (preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                CharSequence listEntry = listPreference.getEntry();
                if (listPreference.getSummary() != listEntry) {
                    listPreference.setSummary(listPreference.getEntry());
                    Log.d(TAG, "updateSummary: key=" + preference.getKey() + ", summary=" + listEntry);
                }
            }
            else if (preference instanceof RingtonePreference) {
                String uriString = sharedPreferences.getString(preference.getKey(), "default");
                final String summary = getUriTitle(uriString);
                preference.setSummary(summary);
                Log.d(TAG, "updateSummary: key=" + preference.getKey() + ", summary=" + summary);
            }
        }
    }

    private void updateSummaryNotificationChannelIntent(String key, String uriString, boolean vibrate) {
        final String summary = getUriTitle(uriString) + "\n" + getString(vibrate ? R.string.vibrate_on : R.string.vibrate_off);
        Log.d(TAG, "updateSummaryNotificationChannelIntent: key=" + key + ", uriString=" + uriString + ", vibrate=" + vibrate + ",\nsummary=" + summary);
        Preference intentPreference = settingsFragment.findPreference(key);
        intentPreference.setSummary(summary);
    }

    private String getUriTitle(String uriString) {
        Log.d(TAG, "getUriTitle : uriString=" + uriString);
        Uri uri = Uri.parse(uriString);
        Ringtone ringtone = RingtoneManager.getRingtone(getApplicationContext(), uri);
        if (ringtone != null) {
            String title = ringtone.getTitle(getApplicationContext());
            if (title != null && !title.equals("Unknown")) {
                return title;
            }
        }

        // Fallback for custom ringtones when permission is denied or title is unknown
        String lastPathSegment = uri.getLastPathSegment();
        if (lastPathSegment != null) {
            return lastPathSegment;
        }
        return "Custom Ringtone";
    }

    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    private void updateSummaryNotificationPermissionIntent(String key) {
        int notificationPermissionState = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS);
        Log.d(TAG, "updateSummaryNotificationPermissionIntent: notificationPermissionState=" + notificationPermissionState);
        Preference intentPreference = settingsFragment.findPreference(key);
        if (notificationPermissionState == PackageManager.PERMISSION_GRANTED) {
            intentPreference.setSummary(getString(R.string.preferences_permission_granted));
        }
        else {
            intentPreference.setSummary(Html.fromHtml("<font color='red'>" + getString(R.string.preferences_notification_permission_denied) + "</font>"));
        }
    }

    private final SharedPreferences.OnSharedPreferenceChangeListener listener =
        new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                if (key != null) {
                    if (isKeyPreference(getBaseContext(), key) && !restoringPreferences) {
                        Log.d(TAG, "onSharedPreferenceChanged: key=" + key);
                        updateSummary(key);
                        if (key.equals(getString(R.string.pref_dark_theme_mode))) {
                            updateDayNightMode();
                        } else if (key.equals(getString(R.string.pref_step_time))) {
                            updateStepTimePreference();
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            if (key.equals(getString(R.string.pref_ringtone_uri)) || key.equals(getString(R.string.pref_vibrate))) {
                                updateSummaryNotificationChannelIntent(getString(R.string.pref_done_channel), doneChannelUriString, doneChannelVibrate);
                            } else if (key.equals(getString(R.string.pref_timer_get_ready_ringtone_uri)) || key.equals(getString(R.string.pref_timer_get_ready_vibrate))) {
                                updateSummaryNotificationChannelIntent(getString(R.string.pref_ready_channel), readyChannelUriString, readyChannelVibrate);
                            }
                        }
                    }
                }
            }
        };

    private void updateDayNightMode() {
        int newDarkNightMode = Integer.parseInt(sharedPreferences.getString(getString(R.string.pref_dark_theme_mode), getString(R.string.default_dark_mode)));
        Log.d(TAG, "updateDayNightMode: dayNightMode=" + dayNightMode + ", newDarkThemeMode=" + newDarkNightMode);
        if (dayNightMode != newDarkNightMode) {
            dayNightMode = newDarkNightMode;
            AppCompatDelegate.setDefaultNightMode(newDarkNightMode);
            getDelegate().applyDayNight();
            recreate();
        }
    }

    static boolean isKeyPreference(Context context, String key) {
        return key != null && !key.contains(context.getString(R.string.pref_preset_array)) && !key.contains(context.getString(R.string.pref_timer_service))
                && !key.contains(context.getString(R.string.pref_timer_text));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // Legacy storage permission handling removed in favor of SAF
    }

    private void saveSharedPreferencesToFile() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/octet-stream");
        intent.putExtra(Intent.EXTRA_TITLE, "notification_timer.backup");
        startActivityForResult(intent, CREATE_BACKUP_REQUEST_CODE);
    }

    private void performSaveSharedPreferencesToUri(Uri uri) {
        ObjectOutputStream objectOutputStream = null;
        try {
            objectOutputStream = new ObjectOutputStream(getContentResolver().openOutputStream(uri));
            objectOutputStream.writeObject(sharedPreferences.getAll());
            Toast.makeText(this, getString(R.string.preferences_backup_success), Toast.LENGTH_SHORT).show();
            Log.d(TAG, "performSaveSharedPreferencesToUri: success");
        } catch (IOException e) {
            Log.e(TAG, "performSaveSharedPreferencesToUri: error", e);
            Toast.makeText(this, getString(R.string.preferences_backup_error), Toast.LENGTH_SHORT).show();
        } finally {
            try {
                if (objectOutputStream != null) {
                    objectOutputStream.flush();
                    objectOutputStream.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void loadSharedPreferencesFromFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/octet-stream");
        startActivityForResult(intent, RESTORE_BACKUP_REQUEST_CODE);
    }

    @SuppressWarnings("unchecked")
    private void performLoadSharedPreferencesFromUri(Uri uri) {
        ObjectInputStream objectInputStream = null;
        try {
            restoringPreferences = true;
            objectInputStream = new ObjectInputStream(getContentResolver().openInputStream(uri));
            SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
            sharedPreferencesEditor.clear();
            for (Map.Entry<String, ?> entry : ((Map<String, ?>) objectInputStream.readObject()).entrySet()) {
                String key = entry.getKey();
                // Do not restore timer preferences and main activity text view settings
                if (key.contains(getString(R.string.pref_timer_service)) || key.contains(getString(R.string.pref_timer_text))) {
                    continue;
                }
                Object value = entry.getValue();
                Log.d(TAG, "performLoadSharedPreferencesFromUri: key=" + key + ", value=" + value);
                if (value instanceof Boolean) {
                    final boolean valueBoolean = (Boolean) value;
                    sharedPreferencesEditor.putBoolean(key, (Boolean) value);
                    if (key.equals(getString(R.string.pref_vibrate))) {
                        doneChannelVibrate = valueBoolean;
                    }
                    if (key.equals(getString(R.string.pref_timer_get_ready_vibrate))) {
                        readyChannelVibrate = valueBoolean;
                    }
                } else if (value instanceof Float) {
                    sharedPreferencesEditor.putFloat(key, (Float) value);
                } else if (value instanceof Integer) {
                    sharedPreferencesEditor.putInt(key, (Integer) value);
                } else if (value instanceof Long) {
                    sharedPreferencesEditor.putLong(key, (Long) value);
                } else if (value instanceof String) {
                    final String valueString = (String) value;
                    sharedPreferencesEditor.putString(key, valueString);
                    if (key.equals(getString(R.string.pref_ringtone_uri))) {
                        doneChannelUriString = valueString;
                    }
                    if (key.equals(getString(R.string.pref_timer_get_ready_ringtone_uri))) {
                        readyChannelUriString = valueString;
                    }
                }
            }
            sharedPreferencesEditor.apply();
            updateAllPreferences();
            Toast.makeText(this, getString(R.string.preferences_restore_success), Toast.LENGTH_SHORT).show();
            updateDayNightMode();
        } catch (ClassNotFoundException | IOException e) {
            Log.e(TAG, "performLoadSharedPreferencesFromUri: error", e);
            Toast.makeText(this, getString(R.string.preferences_restore_no_file), Toast.LENGTH_SHORT).show();
        } finally {
            restoringPreferences = false;
            try {
                if (objectInputStream != null) {
                    objectInputStream.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
