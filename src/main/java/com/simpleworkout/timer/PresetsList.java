package com.simpleworkout.timer;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

class PresetsList {

    private static final String TAG = "PresetCardsList";

    private ArrayList<Preset> list = new ArrayList<>();

    private SharedPreferences sharedPreferences;
    private Context context;

    void setSharedPreferences(SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    void addPreset(int index, Preset preset) {
        Log.d(TAG, "addPreset: index=" + index + ", preset='" + preset.toString() + "'");
        list.add(index, preset);
        for (int p = getSize(); p > index; --p) {
            savePreset(p, loadPreset(p - 1));
        }
        savePreset(index, preset);
    }

    void removePreset(int index) {
        Log.d(TAG, "removePreset: index=" + index);
        list.remove(index);
        for (int p = index; p < getSize(); ++p) {
            savePreset(p, loadPreset(p + 1));
        }
        erasePreset(getSize());
    }

    void swapPreset(int fromIndex, int toIndex) {
        Log.d(TAG, "movePreset: fromIndex=" + fromIndex + ", toIndex=" + toIndex);
        Collections.swap(list, fromIndex, toIndex);
        Preset preset = loadPreset(fromIndex);
        removePreset(fromIndex);
        addPreset(toIndex, preset);
    }

    int indexOf(Preset preset) {
        return list.indexOf(preset);
    }

    Preset getPreset(int index) {
        return list.get(index);
    }

    int getSize() { return list.size(); }

    private void savePreset(int index, final Preset preset) {
        if (sharedPreferences != null) {
            SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
            sharedPreferencesEditor.putLong(String.format(Locale.US, context.getString(R.string.pref_preset_array_timer), index), preset.getTimer());
            sharedPreferencesEditor.putInt(String.format(Locale.US, context.getString(R.string.pref_preset_array_sets), index), preset.getSets());
            sharedPreferencesEditor.putInt(String.format(Locale.US, context.getString(R.string.pref_preset_array_init), index), preset.getInit());
            sharedPreferencesEditor.apply();
            Log.d(TAG, "savePreset: index=" + index + ", preset='" + preset + "'");
        }
    }

    private Preset loadPreset(int index) {
        if (sharedPreferences != null) {
            long timer = sharedPreferences.getLong(String.format(Locale.US, context.getString(R.string.pref_preset_array_timer), index), -1);
            int sets = sharedPreferences.getInt(String.format(Locale.US, context.getString(R.string.pref_preset_array_sets), index), -1);
            int init = sharedPreferences.getInt(String.format(Locale.US, context.getString(R.string.pref_preset_array_init), index), -1);
            Preset preset = new Preset(timer, sets, init);
            Log.d(TAG, "loadPreset: index=" + index + ", preset='" + preset + "'");
            return preset;
        } else {
            return new Preset();
        }
    }

    private void erasePreset(int index) {
        savePreset(index, new Preset());
    }

    void initPresets() {
        Log.d(TAG, "initPresets");
        int index = 0;
        while (true) {
            Preset preset = loadPreset(index);
            if (preset.isValid()) {
                list.add(index++, preset);
            } else {
                break;
            }
        }
    }
}