package com.simpleworkout.timer;

import java.util.Locale;

class Preset {

    private final long timer;
    private final int sets;

    Preset() {
        this.timer = -1;
        this.sets = -1;
    }

    Preset(long timer, int sets) {
        this.timer = timer;
        this.sets = sets;
    }

    int getSets() {
        return sets;
    }

    boolean isInfinity() {
        return sets == Integer.MAX_VALUE;
    }

    String getSetsString() {
        return (sets == Integer.MAX_VALUE)? "" : String.format(Locale.US, "x%d", sets);
    }

    public long getTimer() {
        return timer;
    }

    String getTimerLeftString() {
        return String.format(Locale.US, "%d", timer / 60);
    }

    String getTimerRightString() {
        return String.format(Locale.US, "%02d", timer % 60);
    }

    public String toString() {
        return String.format(Locale.US, "%s'%s %s", getTimerLeftString(), getTimerRightString(), getSetsString());
    }

    boolean isValid() {
        return this.timer > 0 && this.sets > 0;
    }

    @Override
    public boolean equals(Object object)
    {
        if (object != null && object instanceof Preset) {
            Preset preset = (Preset)object;
            return this.timer == preset.timer && this.sets == preset.sets;
        } else {
            return false;
        }
    }
}
