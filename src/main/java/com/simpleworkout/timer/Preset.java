package com.simpleworkout.timer;

import java.util.Locale;

class Preset {

    private long timer;
    private int sets;
    private int init;

    Preset() {
        this.timer = -1;
        this.sets = -1;
        this.init = -1;
    }

    Preset(long timer, int sets, int init) {
        this.timer = timer;
        this.sets = sets;
        this.init = init;
    }

    public int getInit() {
        return init;
    }

    public void setInit(int init) {
        this.init = init;
    }

    int getSets() {
        return sets;
    }

    String getSetsString() {
        if (sets == MainActivity.SETS_INFINITY) {
            return String.format(Locale.US, "%d...", init);
        } else {
            return String.format(Locale.US, "%d..%d", init, sets);
        }
    }

    public long getTimer() {
        return timer;
    }

    String getTimerString() {
        return String.format(Locale.US, "%d:%02d", timer / 60, timer % 60);
    }

    public void setTimer(long timer) {
        this.timer = timer;
    }

    public String toString() {
        return "t=" + timer + "|s=" + sets + "|i=" + init;
    }

    public boolean isValid() {
        return this.timer > 0 && this.sets > 0 && (this.init == 0 || this.init == 1);
    }

    @Override
    public boolean equals(Object object)
    {
        if (object != null && object instanceof Preset) {
            Preset preset = (Preset)object;
            return this.timer == preset.timer && this.sets == preset.sets && this.init == preset.init;
        } else {
            return false;
        }
    }
}
