/*
 * Copyright (C) 2014 Ruikye's open source project
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
package com.awaysoft.nightlymode.services;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.awaysoft.nightlymode.PreferenceActivity;
import com.awaysoft.nightlymode.R;
import com.awaysoft.nightlymode.utils.Constant;
import com.awaysoft.nightlymode.utils.Preference;
import com.awaysoft.nightlymode.utils.Utils;
import com.awaysoft.nightlymode.widget.FloatTouch;
import com.awaysoft.nightlymode.widget.MatteLayer;
import com.umeng.analytics.MobclickAgent;

/**
 * Background service for auto-nightly
 *
 * @author ruikye
 * @since 2014
 */
public class NightlyService extends Service implements Callback {
    private String mTopApp;
    private Handler mHandler;
    private AppMonitor mAppMonitor;
    private MatteLayer mMatteLayer;
    private WindowManager mGlobalWindow;
    private FloatTouch mFloatController;

    // On preference changed monitor
    private BroadcastReceiver mPreferenceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }

            String action = intent.getAction();
            if (Constant.BDC_PREFERENCE_CHANGED.equals(action)) {
                Message msg = Message.obtain(mHandler);
                msg.what = Constant.MSG_PREFERENCE_CHANGED;
                msg.obj = intent.getIntExtra(Constant.PREFERENCE_TARGET_KEY, -1);
                mHandler.sendMessage(msg);
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mTopApp = "null";
        mHandler = new Handler(getMainLooper(), this);
        mGlobalWindow = (WindowManager) getSystemService(WINDOW_SERVICE);

        mMatteLayer = new MatteLayer(this);
        mMatteLayer.setAttachedWindow(mGlobalWindow);

        mFloatController = new FloatTouch(this);
        mFloatController.setAttachedWindow(mGlobalWindow);

        // Register preference monitor receiver
        registerReceiver(mPreferenceReceiver, new IntentFilter(Constant.BDC_PREFERENCE_CHANGED));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        Toast.makeText(this, getString(R.string.service_started), Toast.LENGTH_SHORT).show();
        Preference.INSTANCE.read(this);

        Preference.sServiceRunning = true;
        Preference.INSTANCE.saveKey(this, Constant.KEY_SERVICES_RUNNING, true);
        startMonitor();

        if (Preference.sFloatWidget && !Preference.sPreferenceRunning) {
            mFloatController.attachToWindow(mGlobalWindow);
        } else {
            mFloatController.detachFromWindow();
        }

        if (Preference.sNotification) {
            startForeground();
        } else {
            stopForeground(true);
        }

        ComponentName name = ((ActivityManager) getSystemService(Context.ACTIVITY_SERVICE)).getRunningTasks(1).get(
                0).topActivity;
        mTopApp = name.getPackageName();
        switchMode(mTopApp);

        // for umeng sdk
        MobclickAgent.onResume(this);

        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        // Unregister preference monitor receiver
        unregisterReceiver(mPreferenceReceiver);

        Preference.INSTANCE.save(this);

        stopMonitor();
        switchMode("stop");

        if (mMatteLayer != null) {
            mMatteLayer.detachFromWindow();
        }

        if (mFloatController != null) {
            mFloatController.detachFromWindow();
        }

        //Preference.save(this);
        stopForeground(true);
        super.onDestroy();
        Toast.makeText(this, getString(R.string.service_stoped), Toast.LENGTH_SHORT).show();

        // for umeng sdk
        MobclickAgent.onPause(this);

        if (Preference.sServiceRunning) {
            startService(new Intent(this, NightlyService.class));
        }
    }

    private class AppMonitor extends Thread {
        private volatile Boolean mNeedStop = false;
        private String mLastApp;

        @Override
        public void run() {
            ComponentName name;
            while (!mNeedStop) {
                try {
                    name = ((ActivityManager) getSystemService(Context.ACTIVITY_SERVICE)).getRunningTasks(1).get(0).topActivity;
                    String tmp = name.getPackageName();
                    if (!TextUtils.equals(mLastApp, tmp) && !mNeedStop) {
                        Message msg = Message.obtain(mHandler);
                        msg.what = Constant.MSG_UPDATE_APP_INFO;
                        msg.obj = tmp;
                        mHandler.sendMessage(msg);
                    }

                    mLastApp = tmp;
                    sleep(100);
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void startMonitor() {
        if (mAppMonitor == null || !mAppMonitor.isAlive()) {
            if (mAppMonitor != null) {
                mAppMonitor.mNeedStop = true;
            }

            mAppMonitor = new AppMonitor();
            mAppMonitor.mNeedStop = false;
            mAppMonitor.start();
        }
    }

    private void stopMonitor() {
        if (mAppMonitor != null) {
            mAppMonitor.mNeedStop = true;
        }
    }

    @SuppressWarnings("deprecation")
    private void startForeground() {
        Notification mNotification = new Notification(R.drawable.night_notification_icon,
                getText(R.string.nightly_title), System.currentTimeMillis());
        Intent notificationIntent = new Intent(this, PreferenceActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        mNotification.setLatestEventInfo(this, getText(R.string.nightly_title),
                getText(R.string.night_notification_tips), pendingIntent);
        startForeground(mNotification.hashCode(), mNotification);
    }

    private void switchMode(String pkgName) {
        if ("stop".equals(pkgName) || "com.android.packageinstaller".equals(pkgName)) {
            mMatteLayer.matteSmoothOut();
        } else {
            switch (Preference.sNightlyMode) {
                case Constant.MODE_AUTO:
                    if (Preference.INSTANCE.inWhiteList(pkgName)) {
                        mMatteLayer.matteSmoothIn();
                    } else {
                        mMatteLayer.matteSmoothOut();
                    }
                    break;
                case Constant.MODE_NIGHT:
                    mMatteLayer.matteSmoothIn();
                    break;
                case Constant.MODE_NORMAL:
                    mMatteLayer.matteSmoothOut();
                    break;
            }
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            // Top application changed
            case Constant.MSG_UPDATE_APP_INFO: {
                if (msg.obj instanceof String) {
                    mTopApp = (String) msg.obj;
                    switchMode(mTopApp);
                }
                break;
            }
            // Night mode changed
            case Constant.MSG_STATUS_CHANGED: {
                switchMode(mTopApp);
                break;
            }
            // Preference changed
            case Constant.MSG_PREFERENCE_CHANGED: {
                Object obj = msg.obj;
                int tag = -1;
                if (obj instanceof Integer) {
                    tag = (Integer) obj;
                }

                onPreferenceChanged(tag);
                break;
            }

            default:
                return false;
        }

        return true;
    }

    private void onPreferenceChanged(int tag) {
        if (tag != -1) {
            switch (tag) {
                case Constant.TAG_ID_MODE: {
                    mHandler.sendEmptyMessage(Constant.MSG_STATUS_CHANGED);
                    break;
                }

                case Constant.TAG_ID_ALPHA: {
                    mMatteLayer.setMatteAlpha(Preference.sMatteAlpha);
                    return;
                }

                case Constant.TAG_ID_COLOR: {
                    mMatteLayer.setMatterColor(Preference.sMatteColor);
                    return;
                }

                case Constant.TAG_ID_NOTIFICATION: {
                    if (Preference.sNotification) {
                        startForeground();
                    } else {
                        stopForeground(true);
                    }
                    break;
                }

                case Constant.TAG_ID_FLOAT_WIDGET: {
                    if (Preference.sFloatWidget && !Preference.sPreferenceRunning) {
                        mFloatController.attachToWindow(mGlobalWindow);
                    } else {
                        mFloatController.detachFromWindow();
                    }
                    break;
                }

                case Constant.TAG_ID_ALERT: {
                    if (Preference.sNighttimeRemind) {
                        String[] times = Preference.sTimeBuckets.split("\\|");
                        String[] start = times[0].split(":");
                        String[] end = times[1].split(":");
                        Utils.INSTANCE.startNightAlarm(this, Integer.valueOf(start[0]),
                                Integer.valueOf(start[1]), Constant.ALARM_FLAG_NIGHT_START);
                        Utils.INSTANCE.startNightAlarm(this, Integer.valueOf(end[0]),
                                Integer.valueOf(end[1]), Constant.ALARM_FLAG_NIGHT_END);
                    } else {
                        Utils.INSTANCE.stopNightAlarm(this, Constant.ALARM_FLAG_NIGHT_START);
                        Utils.INSTANCE.stopNightAlarm(this, Constant.ALARM_FLAG_NIGHT_END);
                    }
                    break;
                }
            }
        }
    }
}
