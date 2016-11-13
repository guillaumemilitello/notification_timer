package com.simpleworkout.timer;

public class IntentAction {

    // Sent from the buttons
    public static final String START = "com.simpleworkout.action.START";
    public static final String PAUSE = "com.simpleworkout.action.PAUSE";
    public static final String RESUME = "com.simpleworkout.action.RESUME";
    public static final String STOP = "com.simpleworkout.action.STOP";
    public static final String RESET = "com.simpleworkout.action.RESET";
    public static final String CLEAR = "com.simpleworkout.action.CLEAR";
    public static final String NEXT_SET = "com.simpleworkout.action.NEXT_SET";
    public static final String NEXT_SET_START = "com.simpleworkout.action.NEXT_SET_START";
    public static final String EXTRA_SET = "com.simpleworkout.action.EXTRA_SET";
    public static final String TIMER_MINUS = "com.simpleworkout.action.TIMER_MINUS";
    public static final String TIMER_PLUS = "com.simpleworkout.action.TIMER_PLUS";
    public static final String SETS_MINUS = "com.simpleworkout.action.SETS_MINUS";
    public static final String SETS_PLUS = "com.simpleworkout.action.SETS_PLUS";

    // Sent from the TimerService to launch Alerts
    public static final String SET_DONE = "com.simpleworkout.action.SET_DONE";
    public static final String ALL_SETS_DONE = "com.simpleworkout.action.ALL_SETS_DONE";

    public static final String NOTIFICATION_DISMISS = "com.simpleworkout.action.NOTIFICATION_DISMISS";

    // Sent from the TimerService to the MainActivity UI and InteractiveNotification
    public static final String TIMER_REBIND = "com.simpleworkout.action.TIMER_REBIND";
    public static final String TIMER_UPDATE = "com.simpleworkout.action.TIMER_UPDATE";
    public static final String TIMER_STATE = "com.simpleworkout.action.TIMER_STATE";
    public static final String TIMER_DONE = "com.simpleworkout.action.TIMER_DONE";

    // Sent from the AlarmManager to refresh the InteractiveNotification
    public static final String ACQUIRE_WAKELOCK = "com.simpleworkout.action.ACQUIRE_WAKELOCK";
}
