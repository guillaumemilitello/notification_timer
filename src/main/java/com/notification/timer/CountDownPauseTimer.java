/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.notification.timer;

import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;

/**
 * Schedule a countdown until a time in the future, with
 * regular notifications on intervals along the way.
 *
 * Example of showing a 30 second countdown in a text field:
 *
 * <pre class="prettyprint">
 * new CountDownPauseTimer(30000, 1000) {
 *
 *     public void onTick(long millisUntilFinished) {
 *         mTextField.setText("seconds remaining: " + millisUntilFinished / 1000);
 *     }
 *
 *     public void onFinish() {
 *         mTextField.setText("done!");
 *     }
 *  }.start();
 * </pre>
 *
 * The calls to {@link #onTick(long)} are synchronized to this object so that
 * one call to {@link #onTick(long)} won't ever occur before the previous
 * callback is complete.  This is only relevant when the implementation of
 * {@link #onTick(long)} takes an amount of time to execute that is significant
 * compared to the countdown interval.
 */
abstract class CountDownPauseTimer {

    /**
     * Millis since epoch when alarm should stop.
     */
    private final long mMillisInFuture;

    /**
     * The interval in millis that the user receives callbacks
     */
    private final long mCountdownInterval;

    private long mStopTimeInFuture;

    private long mTimeLeft;

    /**
     * boolean representing if the com.notification.timer was cancelled
     */
    private boolean mCancelled = false;

    /**
     * boolean representing if the com.notification.timer was paused
     */
    private boolean mPaused = false;

    /**
     * @param millisInFuture The number of millis in the future from the call
     *   to {@link #start()} until the countdown is done and {@link #onFinish()}
     *   is called.
     * @param countDownInterval The interval along the way to receive
     *   {@link #onTick(long)} callbacks.
     */
    CountDownPauseTimer(long millisInFuture, long countDownInterval) {
        // An extra second is added to match com.notification.timer's behavior
        mMillisInFuture = millisInFuture + 250;
        mCountdownInterval = countDownInterval;
        mTimeLeft = mMillisInFuture;
    }

    /**
     * Pause the countdown
     */
    public synchronized final void pause() {
        mPaused = true;
        mHandler.removeMessages(MSG);
    }

    /**
     * Resume the countdown
     */
    public synchronized final void resume() {
        mPaused = false;
        mStopTimeInFuture = SystemClock.elapsedRealtime() + mTimeLeft;
        mHandler.sendMessage(mHandler.obtainMessage(MSG));
    }

    /**
     * Cancel the countdown.
     */
    public synchronized final void cancel() {
        mCancelled = true;
        mHandler.removeMessages(MSG);
    }

    /**
     * Update the remaining time to the countdown
     */
    public synchronized final void update(long millis) {
        // An extra second is added to match com.notification.timer's behavior
        millis += 250;
        mStopTimeInFuture += (millis - mTimeLeft);
        mTimeLeft = millis;
	    if(!mPaused) {
            mHandler.removeMessages(MSG);
            mHandler.sendMessage(mHandler.obtainMessage(MSG));
        }        
    }

    /**
     * Start the countdown.
     */
    public synchronized final void start() {
        mCancelled = false;
        mPaused = false;
        if (mMillisInFuture <= 0) {
            onFinish();
            return;
        }
        mStopTimeInFuture = SystemClock.elapsedRealtime() + mMillisInFuture;
        mHandler.sendMessage(mHandler.obtainMessage(MSG));
    }


    /**
     * Callback fired on regular interval.
     * @param millisUntilFinished The amount of time until finished.
     */
    protected abstract void onTick(long millisUntilFinished);

    /**
     * Callback fired when the time is up.
     */
    protected abstract void onFinish();

    private static final int MSG = 1;

    private final CountDownPauseTimerHandler mHandler = new CountDownPauseTimerHandler(this);

    private static class CountDownPauseTimerHandler extends Handler {

        private final CountDownPauseTimer timer;

        CountDownPauseTimerHandler(CountDownPauseTimer timer) {
            this.timer = timer;
        }

        @Override
        public void handleMessage(Message msg) {

            synchronized (timer) {
                if (timer.mCancelled) {
                    return;
                }

                timer.mTimeLeft = timer.mStopTimeInFuture - SystemClock.elapsedRealtime();

                if (timer.mTimeLeft <= 1000) {
                    timer.onFinish();
                } else {
                    long lastTickStart = SystemClock.elapsedRealtime();
                    timer.onTick(timer.mTimeLeft);

                    // take into account user's onTick taking time to execute
                    long delay = lastTickStart + timer.mCountdownInterval - SystemClock.elapsedRealtime();

                    // special case: user's onTick took more than interval to
                    // complete, skip to next interval
                    while (delay < 0) delay += timer.mCountdownInterval;

                    sendMessageDelayed(obtainMessage(MSG), delay);
                }
            }
        }
    }
}
