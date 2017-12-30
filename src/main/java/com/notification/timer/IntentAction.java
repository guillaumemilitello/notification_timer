package com.notification.timer;

class IntentAction {

    // Sent from the buttons
    static final String START = "com.notification.timer.action.START";
    static final String PAUSE = "com.notification.timer.action.PAUSE";
    static final String RESUME = "com.notification.timer.action.RESUME";
    static final String STOP = "com.notification.timer.action.STOP";
    static final String RESET = "com.notification.timer.action.RESET";
    static final String CLEAR = "com.notification.timer.action.CLEAR";
    static final String NEXT_SET = "com.notification.timer.action.NEXT_SET";
    static final String NEXT_SET_START = "com.notification.timer.action.NEXT_SET_START";
    static final String EXTRA_SET = "com.notification.timer.action.EXTRA_SET";
    static final String TIMER_MINUS = "com.notification.timer.action.TIMER_MINUS";
    static final String TIMER_PLUS = "com.notification.timer.action.TIMER_PLUS";
    static final String SETS_MINUS = "com.notification.timer.action.SETS_MINUS";
    static final String SETS_PLUS = "com.notification.timer.action.SETS_PLUS";

    // Sent from the TimerService to launch Alerts
    static final String SET_DONE = "com.notification.timer.action.SET_DONE";
    static final String ALL_SETS_DONE = "com.notification.timer.action.ALL_SETS_DONE";

    static final String NOTIFICATION_DISMISS = "com.notification.timer.action.NOTIFICATION_DISMISS";

    // Sent from the TimerService to the MainActivity UI and InteractiveNotification
    static final String TIMER_REBIND = "com.notification.timer.action.TIMER_REBIND";
    static final String TIMER_UPDATE = "com.notification.timer.action.TIMER_UPDATE";
    static final String TIMER_STATE = "com.notification.timer.action.TIMER_STATE";
    static final String TIMER_DONE = "com.notification.timer.action.TIMER_DONE";

    // Sent from the AlarmManager to refresh the InteractiveNotification
    static final String ACQUIRE_WAKELOCK = "com.notification.timer.action.ACQUIRE_WAKELOCK";
}
