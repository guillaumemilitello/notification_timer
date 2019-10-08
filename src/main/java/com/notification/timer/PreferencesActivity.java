package com.notification.timer;


import android.Manifest;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;
import android.preference.SwitchPreference;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.widget.Toolbar;

import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import com.codetroopers.betterpickers.hmspicker.HmsPickerBuilder;
import com.codetroopers.betterpickers.hmspicker.HmsPickerDialogFragment;
import com.kizitonwose.colorpreference.ColorPreference;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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

    private File sharedPreferencesFile;
    private boolean restoringPreferences;
    private boolean overridePreferencesFile;

    private static final int PERMISSION_WRITE_EXTERNAL_STORAGE = 1;
    private static final int PERMISSION_READ_EXTERNAL_STORAGE = 2;

    private TimerPreferenceFragment settingsFragment;

    private NotificationManager notificationManager;

    private HmsPickerBuilder timerGetReadyPickerBuilder;

    @Override
    @SuppressWarnings("deprecation")
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preferences_actionbar);

        // Update system color bar and icon for the system
        setSupportActionBar((Toolbar) findViewById(R.id.preferences_toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        setTaskDescription(new ActivityManager.TaskDescription(getApplicationInfo().name,
                BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher),
                ContextCompat.getColor(this, R.color.colorPrimary)));

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

        String relativePath = "/NotificationTimer/prefs.backup";
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            sharedPreferencesFile = new File(getBaseContext().getExternalFilesDir(null), relativePath);
        } else {
            sharedPreferencesFile = new File(getBaseContext().getFilesDir(), relativePath);
        }

        restoringPreferences = false;
        overridePreferencesFile = false;
    }

    public static class TimerPreferenceFragment extends PreferenceFragment
    {
        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
        }

        private void sendIntentNotificationChannel(String channelId) {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_CHANNEL_ID, channelId);
            intent.putExtra("android.provider.extra.APP_PACKAGE", BuildConfig.APPLICATION_ID);
            startActivity(intent);
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
            if (preference.getKey().equals(getString(R.string.pref_done_notification))) {
                sendIntentNotificationChannel(InteractiveNotification.getDoneChannelId());
                return true;
            } else if (preference.getKey().equals(getString(R.string.pref_timer_get_ready_notification))) {
                sendIntentNotificationChannel(InteractiveNotification.getReadyChannelId());
                return true;
            }
            else
            {
                return super.onPreferenceTreeClick(preferenceScreen, preference);
            }
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            updateNotificationChannelPreferences();
        }
        updateSummaries();
        updateStepTimePreference();
        updateTimerGetReadySummary();
        updateBackupPreferences();
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
        updateTimerGetReadySummary();
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
    public void onDialogHmsSet(int reference, boolean isNegative, int hours, int minutes, int seconds) {
        Log.d(TAG, "onDialogMsSet: hours=" + hours + ", minutes=" + minutes + ", seconds=" + seconds);
        int timerGetReady = hours * 3600 + minutes * 60 + seconds; // TODO: use time lib
        SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
        sharedPreferencesEditor.putString(getString(R.string.pref_timer_get_ready), String.valueOf(timerGetReady));
        sharedPreferencesEditor.apply();
        updateTimerGetReadySummary(timerGetReady);
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
    private void updateNotificationChannelPreferences() {
        SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
        List<NotificationChannel> notificationChannelList = notificationManager.getNotificationChannels();
        for (NotificationChannel notificationChannel : notificationChannelList) {
            if (notificationChannel.getId().equals(InteractiveNotification.getDoneChannelId())) {
                Log.d(TAG, "updateNotificationChannelPreferences: notificationId=" + notificationChannel.getId());
                sharedPreferencesEditor.putBoolean(getString(R.string.pref_vibrate), notificationChannel.shouldVibrate());
                Log.d(TAG, "updateNotificationChannelPreferences: key=" + getString(R.string.pref_vibrate) + ", bool=" + notificationChannel.shouldVibrate());
                Uri uri = notificationChannel.getSound();
                if (uri != null) {
                    sharedPreferencesEditor.putString(getString(R.string.pref_ringtone_uri), uri.toString());
                    Log.d(TAG, "updateNotificationChannelPreferences: key=" + getString(R.string.pref_ringtone_uri) + ", string=" + notificationChannel.getSound().toString());
                } else {
                    sharedPreferencesEditor.putString(getString(R.string.pref_ringtone_uri), getString(R.string.default_timer_get_ready_ringtone_uri));
                    Log.d(TAG, "updateNotificationChannelPreferences: key=" + getString(R.string.pref_ringtone_uri) + ", string=default");
                }
                sharedPreferencesEditor.putBoolean(getString(R.string.pref_light_color_enable), notificationChannel.shouldShowLights());
                Log.d(TAG, "updateNotificationChannelPreferences: key=" + getString(R.string.pref_light_color_enable) + ", bool=" + notificationChannel.shouldShowLights());
                sharedPreferencesEditor.putInt(getString(R.string.pref_light_color), notificationChannel.getLightColor());
                Log.d(TAG, "updateNotificationChannelPreferences: key=" + getString(R.string.pref_light_color) + ", color=" + notificationChannel.getLightColor());
            }
            else if (notificationChannel.getId().equals(InteractiveNotification.getReadyChannelId())) {
                Log.d(TAG, "updateNotificationChannelPreferences: notificationId=" + notificationChannel.getId());
                sharedPreferencesEditor.putBoolean(getString(R.string.pref_timer_get_ready_vibrate), notificationChannel.shouldVibrate());
                Log.d(TAG, "updateNotificationChannelPreferences: key=" + getString(R.string.pref_timer_get_ready_vibrate) + ", bool=" + notificationChannel.shouldVibrate());
                Uri uri = notificationChannel.getSound();
                if (uri != null) {
                    sharedPreferencesEditor.putString(getString(R.string.pref_timer_get_ready_ringtone_uri), uri.toString());
                    Log.d(TAG, "updateNotificationChannelPreferences: key=" + getString(R.string.pref_ringtone_uri) + ", string=" + notificationChannel.getSound().toString());
                } else {
                    sharedPreferencesEditor.putString(getString(R.string.pref_timer_get_ready_ringtone_uri), getString(R.string.default_timer_get_ready_ringtone_uri));
                    Log.d(TAG, "updateNotificationChannelPreferences: key=" + getString(R.string.pref_ringtone_uri) + ", string=default");
                }
            }
        }
        sharedPreferencesEditor.apply();
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
                    updateSummary(settingsFragment.findPreference(key));
                }
            }
        }
    }

    private void updateSummary(Preference preference) {
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
                Log.d(TAG, "updateSummary: key=" + preference.getKey());
                String title = sharedPreferences.getString(preference.getKey(), "default");
                Log.d(TAG, "updateSummary: title=" + title);
                title = RingtoneManager.getRingtone(this, Uri.parse(title)).getTitle(this);
                Log.d(TAG, "updateSummary: title=" + title);
                if (title.equals("Unknown")) {
                    title = "None";
                }
                preference.setSummary(title);
            }
        }
    }

    private final SharedPreferences.OnSharedPreferenceChangeListener listener =
        new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                if (isKeyPreference(getBaseContext(), key) && !restoringPreferences) {
                    Log.d(TAG, "SharedPreferenceChanged: key=" + key);
                    if (key.equals(getString(R.string.pref_dark_theme_mode))) {
                        // no need to update summary, if new mode recreate is called
                        updateDayNightMode();
                    }
                    else {
                        updateSummary(settingsFragment.findPreference(key));
                        if (key.equals(getString(R.string.pref_step_time))) {
                            updateStepTimePreference();
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
        return !key.contains(context.getString(R.string.pref_preset_array)) && !key.contains(context.getString(R.string.pref_timer_service))
                && !key.contains(context.getString(R.string.pref_timer_text));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_WRITE_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "onRequestPermissionsResult : requestCode=" + requestCode + " granted");
                    saveSharedPreferencesToFile();
                } else {
                    Log.d(TAG, "onRequestPermissionsResult : requestCode=" + requestCode + " denied");
                    Toast.makeText(this, getString(R.string.preferences_permission_denied), Toast.LENGTH_SHORT).show();
                }
                break;
            case PERMISSION_READ_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "onRequestPermissionsResult : requestCode=" + requestCode + " granted");
                    loadSharedPreferencesFromFile();
                } else {
                    Log.d(TAG, "onRequestPermissionsResult : requestCode=" + requestCode + " denied");
                    Toast.makeText(this, getString(R.string.preferences_permission_denied), Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private void saveSharedPreferencesToFile() {
        ObjectOutputStream objectOutputStream = null;
        try {
            int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            Log.d(TAG, "saveSharedPreferencesToFile: permissionCheck=" + permissionCheck);
            if (permissionCheck == PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_WRITE_EXTERNAL_STORAGE);
                return;
            }
            if (sharedPreferencesFile.exists()) {
                Log.d(TAG, "saveSharedPreferencesToFile: " + sharedPreferencesFile.getAbsolutePath() + " already exists");
                if (!overridePreferencesFile) {
                    showAlertDialogFileOverride();
                    return;
                }
            } else {
                Log.d(TAG, "saveSharedPreferencesToFile: " + sharedPreferencesFile.getAbsolutePath() + " does not exists");
                if (sharedPreferencesFile.getParentFile().mkdirs()) {
                    if (!sharedPreferencesFile.createNewFile()) {
                        Log.d(TAG, "saveSharedPreferencesToFile: createNewFile");
                        Toast.makeText(this, getString(R.string.preferences_backup_error), Toast.LENGTH_SHORT).show();
                    }
                }
                else {
                    Log.d(TAG, "saveSharedPreferencesToFile: mkdirs");
                    Toast.makeText(this, getString(R.string.preferences_backup_error), Toast.LENGTH_SHORT).show();
                }
            }
            objectOutputStream = new ObjectOutputStream(new FileOutputStream(sharedPreferencesFile));
            objectOutputStream.writeObject(sharedPreferences.getAll());
            Toast.makeText(this, getString(R.string.preferences_backup_success), Toast.LENGTH_SHORT).show();
            Log.d(TAG, "saveSharedPreferencesToFile: " + sharedPreferencesFile.getAbsolutePath() + " overridePreferencesFile=" + overridePreferencesFile);
            overridePreferencesFile = false;
        } catch (IOException e) {
            e.printStackTrace();
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

    private void showAlertDialogFileOverride() {
        Log.d(TAG, "showAlertDialogFileOverride");
        AlertDialog alertDialog = new AlertDialog.Builder(this, R.style.AlertDialogTheme).create();
        alertDialog.setMessage(getString(R.string.preferences_backup_override));
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.alert_yes),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        overridePreferencesFile = true;
                        saveSharedPreferencesToFile();
                    }
                });
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.alert_no),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }

    @SuppressWarnings("unchecked")
    private void loadSharedPreferencesFromFile() {
        ObjectInputStream objectInputStream = null;
        try {
            int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
            Log.d(TAG, "loadSharedPreferencesFromFile : permissionCheck=" + permissionCheck);
            if (permissionCheck == PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_READ_EXTERNAL_STORAGE);
                return;
            }
            Log.d(TAG, "loadSharedPreferencesFromFile: " + sharedPreferencesFile.getAbsolutePath());
            if (!sharedPreferencesFile.exists()) {
                Toast.makeText(this, getString(R.string.preferences_restore_no_file), Toast.LENGTH_SHORT).show();
                return;
            }
            restoringPreferences = true;
            objectInputStream = new ObjectInputStream(new FileInputStream(sharedPreferencesFile));
            SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
            sharedPreferencesEditor.clear();
            for (Map.Entry<String, ?> entry : ((Map<String, ?>) objectInputStream.readObject()).entrySet()) {
                String key = entry.getKey();
                // Do not restore timer preferences and main activity text view settings
                if (key.contains(getString(R.string.pref_timer_service)) || key.contains(getString(R.string.pref_timer_text))) {
                    continue;
                }
                Object value = entry.getValue();
                Log.d(TAG, "loadSharedPreferencesFromFile: key=" + key + ", value=" + value);
                if (value instanceof Boolean) {
                    sharedPreferencesEditor.putBoolean(key, (Boolean) value);
                } else if (value instanceof Float) {
                    sharedPreferencesEditor.putFloat(key, (Float) value);
                } else if (value instanceof Integer) {
                    sharedPreferencesEditor.putInt(key, (Integer) value);
                } else if (value instanceof Long) {
                    sharedPreferencesEditor.putLong(key, (Long) value);
                } else if (value instanceof String) {
                    sharedPreferencesEditor.putString(key, (String) value);
                }
            }
            sharedPreferencesEditor.apply();
            updateAllPreferences();
            Toast.makeText(this, getString(R.string.preferences_restore_success), Toast.LENGTH_SHORT).show();
            restoringPreferences = false;
            updateDayNightMode();
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        } finally {
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
