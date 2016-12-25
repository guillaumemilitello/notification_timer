package com.simpleworkout.timer;


import android.app.ActivityManager;
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
        updateSummaries(sharedPreferences.getAll());
        updateExtraNotificationEnable();
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

    private void updateExtraNotificationEnable() {
        boolean timerGetReadyEnable = sharedPreferences.getBoolean("timerGetReadyEnable", true);
        settingsFragment.findPreference("timerGetReady").setEnabled(timerGetReadyEnable);
        settingsFragment.findPreference("vibrationReadyEnable").setEnabled(timerGetReadyEnable);
        settingsFragment.findPreference("ringtoneUriReady").setEnabled(timerGetReadyEnable);
        settingsFragment.findPreference("lightReadyColor").setEnabled(timerGetReadyEnable);
    }

    private void updateSummaries(Map<String, ?> preferences) {
        Log.d(TAG, "updateSummaries");
        if (preferences != null) {
            for (Map.Entry<String, ?> preference : preferences.entrySet()) {
                String key = preference.getKey();
                Log.d(TAG, "updateSummaries: key=" + key);
                updateSummary(settingsFragment.findPreference(key));
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
                preference.setSummary(title);
            }
        }
    }

    SharedPreferences.OnSharedPreferenceChangeListener listener =
        new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                // avoid checking for preset timer keys
                if(!key.contains("presetArray_")) {
                    // preferenceChangeListener implementation
                    Log.d(TAG, "SharedPreferenceChanged: key=" + key);
                    updateSummary(settingsFragment.findPreference(key));

                    if (key.equals("timerGetReadyEnable"))
                        updateExtraNotificationEnable();
                }
            }
        };
}
