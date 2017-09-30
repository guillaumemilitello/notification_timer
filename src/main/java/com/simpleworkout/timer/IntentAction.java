package com.simpleworkout.timer;

class IntentAction {

    // Sent from the buttons
    static final String START = "com.simpleworkout.action.START";
    static final String PAUSE = "com.simpleworkout.action.PAUSE";
    static final String RESUME = "com.simpleworkout.action.RESUME";
    static final String STOP = "com.simpleworkout.action.STOP";
    static final String RESET = "com.simpleworkout.action.RESET";
    static final String CLEAR = "com.simpleworkout.action.CLEAR";
    static final String NEXT_SET = "com.simpleworkout.action.NEXT_SET";
    static final String NEXT_SET_START = "com.simpleworkout.action.NEXT_SET_START";
    static final String EXTRA_SET = "com.simpleworkout.action.EXTRA_SET";
    static final String TIMER_MINUS = "com.simpleworkout.action.TIMER_MINUS";
    static final String TIMER_PLUS = "com.simpleworkout.action.TIMER_PLUS";
    static final String SETS_MINUS = "com.simpleworkout.action.SETS_MINUS";
    static final String SETS_PLUS = "com.simpleworkout.action.SETS_PLUS";

    // Sent from the TimerService to launch Alerts
    static final String SET_DONE = "com.simpleworkout.action.SET_DONE";
    static final String ALL_SETS_DONE = "com.simpleworkout.action.ALL_SETS_DONE";

    static final String NOTIFICATION_DISMISS = "com.simpleworkout.action.NOTIFICATION_DISMISS";

    // Sent from the TimerService to the MainActivity UI and InteractiveNotification
    static final String TIMER_REBIND = "com.simpleworkout.action.TIMER_REBIND";
    static final String TIMER_UPDATE = "com.simpleworkout.action.TIMER_UPDATE";
    static final String TIMER_STATE = "com.simpleworkout.action.TIMER_STATE";
    static final String TIMER_DONE = "com.simpleworkout.action.TIMER_DONE";

    // Sent from the AlarmManager to refresh the InteractiveNotification
    static final String ACQUIRE_WAKELOCK = "com.simpleworkout.action.ACQUIRE_WAKELOCK";
}
