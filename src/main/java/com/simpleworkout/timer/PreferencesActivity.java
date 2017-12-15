package com.simpleworkout.timer;


import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
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
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.WindowManager;

import java.util.List;
import java.util.Map;

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
public class PreferencesActivity extends AppCompatPreferenceActivity {

    private static final String TAG = "PreferencesActivity";

    private static SharedPreferences sharedPreferences;
    private TimerPreferenceFragment settingsFragment;

    private NotificationManager notificationManager;

    @Override
    @SuppressWarnings("deprecation")
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preferences_actionbar);

        // Update system color bar and icon for the system
        setSupportActionBar((Toolbar) findViewById(R.id.preferences_toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
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

        settingsFragment = new TimerPreferenceFragment();
        getFragmentManager().beginTransaction().replace(R.id.content_frame, settingsFragment).commit();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager = (NotificationManager) getBaseContext().getSystemService(Context.NOTIFICATION_SERVICE);
        }
    }

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
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            updateNotificationChannelPreferences();
        }
        updateSummaries(sharedPreferences.getAll());
        updateGetReadyPreferences();
        updateLightColorPreference();
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

    private void updateLightColorPreference() {
        boolean lightColorEnable = sharedPreferences.getBoolean(getString(R.string.pref_light_color_enable), true);
        settingsFragment.findPreference(getString(R.string.pref_light_color)).setEnabled(lightColorEnable);
    }

    private void updateGetReadyPreferences() {
        boolean timerGetReadyEnable = sharedPreferences.getBoolean(getString(R.string.pref_timer_get_ready_enable), true);
        settingsFragment.findPreference(getString(R.string.pref_timer_get_ready)).setEnabled(timerGetReadyEnable);
        settingsFragment.findPreference(getString(R.string.pref_timer_get_ready_vibrate)).setEnabled(timerGetReadyEnable);
        settingsFragment.findPreference(getString(R.string.pref_timer_get_ready_ringtone_uri)).setEnabled(timerGetReadyEnable);
        settingsFragment.findPreference(getString(R.string.pref_custom_color_ready)).setEnabled(timerGetReadyEnable);
    }

    private void updateSummaries(Map<String, ?> preferences) {
        Log.d(TAG, "updateSummaries");
        if (preferences != null) {
            for (Map.Entry<String, ?> preference : preferences.entrySet()) {
                String key = preference.getKey();
                if (isKeyPreference(key)) {
                    Log.d(TAG, "updateSummaries: key=" + key);
                    updateSummary(settingsFragment.findPreference(key));
                }
            }
        }
    }

    private void updateSummary(Preference preference) {
        if(preference != null) {
            if (preference instanceof ListPreference) {
                Log.d(TAG, "updateSummary: key=" + preference.getKey());
                ListPreference listPreference = (ListPreference) preference;
                preference.setSummary(listPreference.getEntry());
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
                if (isKeyPreference(key)) {
                    // preferenceChangeListener implementation
                    Log.d(TAG, "SharedPreferenceChanged: key=" + key);
                    updateSummary(settingsFragment.findPreference(key));

                    if (key.equals(getString(R.string.pref_timer_get_ready_enable))) {
                        updateGetReadyPreferences();
                    }

                    if (key.equals(getString(R.string.pref_light_color_enable))) {
                        updateLightColorPreference();
                    }
                }
            }
        };

    public boolean isKeyPreference(String key) {
        return !key.contains(getString(R.string.pref_preset_array)) && !key.contains(getString(R.string.pref_timer_service))
                && !key.contains(getString(R.string.pref_timer_text));
    }
}
