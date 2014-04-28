
package com.awaysoft.nightlymode.utils;

import java.util.ArrayList;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

/**
 * Nightly manager preference
 *
 * @author kang
 */
public final class Preference {
    /** Global flag */
    public volatile static int sNightlyMode = Constant.MODE_AUTO;
    /** Mask color */
    public static int sMatteColor = Constant.DEFAULT_COLOR;
    /** Mask alpha */
    public static float sMatteAlpha = Constant.DEFAULT_ALPHA;
    /** Auto start */
    public static boolean sAutoStart = false;
    /** Show notification */
    public static boolean sNotification = true;
    /** Show float widget */
    public static boolean sFloatWidget = false;
    /** Apply for all APPs */
    public static boolean sApplyAll = false;
    /** Service status */
    public static boolean sServiceRunning = false;
    public static boolean sActivityRunning = false;
    /** Float widget location */
    public static String sFloatLocation = "";
    /** Auto night time buckets */
    public static String sTimeBuckets = Constant.DEFAULT_TIMEBUCKETS;
    /** Auto night white list */
    public static String sWhiteList = Constant.DEFAULT_WHITELIST;
    /** Memory white list pool */
    private static ArrayList<String> sWhiteListPool = new ArrayList<String>();

    public static void save(Context context) {
        saveKey(context, Constant.KEY_MATTE_LAYER_COLOR, Integer.valueOf(sMatteColor));
        saveKey(context, Constant.KEY_MATTE_LAYER_ALPHA, Float.valueOf(sMatteAlpha));

        saveKey(context, Constant.KEY_SERVICES_RUNNING, Boolean.valueOf(sServiceRunning));
        saveKey(context, Constant.KEY_SERVICES_NIGHTLY_MODE, Integer.valueOf(sNightlyMode));
        saveKey(context, Constant.KEY_SERVICES_AUTOSTART, Boolean.valueOf(sAutoStart));
        saveKey(context, Constant.KEY_SERVICES_SHOW_NOTIFICATON, Boolean.valueOf(sNotification));
        saveKey(context, Constant.KEY_SERVICES_SHOW_FLOAT_WIDGET, Boolean.valueOf(sFloatWidget));

        saveKey(context, Constant.KEY_NIGHLTY_FOR_ALLAPP, Boolean.valueOf(sApplyAll));
        saveKey(context, Constant.KEY_NIGHTLY_TIMEBUCKETS, sTimeBuckets);
        saveKey(context, Constant.KEY_NIGHTLY_WHITE_LIST, sWhiteList);

        saveKey(context, Constant.KEY_FLOAT_WIDGET_LOCATION, sFloatLocation);
    }

    public static void read(Context context) {
        SharedPreferences sp = getPreference(context);

        sMatteColor = sp.getInt(Constant.KEY_MATTE_LAYER_COLOR, Constant.DEFAULT_COLOR);
        sMatteAlpha = sp.getFloat(Constant.KEY_MATTE_LAYER_ALPHA, Constant.DEFAULT_ALPHA);

        sServiceRunning = sp.getBoolean(Constant.KEY_SERVICES_RUNNING, false);
        sAutoStart = sp.getBoolean(Constant.KEY_SERVICES_AUTOSTART, false);
        sNotification = sp.getBoolean(Constant.KEY_SERVICES_SHOW_NOTIFICATON, true);
        sFloatWidget = sp.getBoolean(Constant.KEY_SERVICES_SHOW_FLOAT_WIDGET, true);
        sNightlyMode = sp.getInt(Constant.KEY_SERVICES_NIGHTLY_MODE, Constant.MODE_AUTO);

        sApplyAll = sp.getBoolean(Constant.KEY_NIGHLTY_FOR_ALLAPP, false);
        sTimeBuckets = sp.getString(Constant.KEY_NIGHTLY_TIMEBUCKETS, Constant.DEFAULT_TIMEBUCKETS);
        sWhiteList = sp.getString(Constant.KEY_NIGHTLY_WHITE_LIST, Constant.DEFAULT_WHITELIST);

        sFloatLocation = sp.getString(Constant.KEY_FLOAT_WIDGET_LOCATION, "");
    }

    public static void saveKey(Context context, String key, Object value) {
        SharedPreferences sp = getPreference(context);
        if (value instanceof Boolean) {
            sp.edit().putBoolean(key, (Boolean) value).commit();
        } else if (value instanceof Integer) {
            sp.edit().putInt(key, (Integer) value).commit();
        } else if (value instanceof String) {
            sp.edit().putString(key, String.valueOf(value)).commit();
        } else if (value instanceof Float) {
            sp.edit().putFloat(key, (Float) value).commit();
        }
    }

    public static boolean inWhiteList(String pkg) {
        return sWhiteListPool.contains(pkg);
    }

    public static void parseWhiteList() {
        if (!TextUtils.isEmpty(sWhiteList)) {
            sWhiteListPool.clear();
            String[] array = sWhiteList.split("\\|");
            for (int i = 0; i < array.length; ++i) {
                sWhiteListPool.add(array[i]);
            }
        }
    }

    private static SharedPreferences getPreference(Context context) {
        return context.getSharedPreferences(Constant.KEY_PREFERENCE_FILE, Context.MODE_PRIVATE);
    }
}