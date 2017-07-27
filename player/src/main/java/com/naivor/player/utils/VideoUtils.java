/*
 * Copyright (c) 2017. Naivor.All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.naivor.player.utils;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Rect;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ContextThemeWrapper;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;

import java.util.Formatter;
import java.util.Locale;

import lombok.NonNull;
import timber.log.Timber;


/**
 * 视频工具类
 * <p>
 * Created by tianlai on 17-7-7.
 */

public final class VideoUtils {


    private VideoUtils() {

    }

    /**
     * 获取屏幕像素密度相对于标准屏幕(160dpi)倍数
     *
     * @return float 屏幕像素密度
     */
    public static float getScreenDensity() {

        return Utils.context().getResources().getDisplayMetrics().density;
    }

    /**
     * 判断view是否在屏幕内
     *
     * @param view
     */
    public static boolean isViewInScreen(@NonNull View view) {
        Rect rect = new Rect();
        view.getGlobalVisibleRect(rect);

        if (rect.top > 0) {       // 可见及部分可见
            return true;
        } else {         //全部不可见时为0，不会出现负值
            return false;
        }
    }


    /**
     * 将dp转换成px
     *
     * @param dp dp值
     * @return px的值
     */

    public static int dp2px(float dp) {
        return (int) (getScreenDensity() * dp + 0.5f);
    }

    /**
     * 格式化时间为  时：分：秒
     *
     * @param millisecond
     * @return
     */
    public static String formateTime(long millisecond) {
        if (millisecond <= 0 || millisecond >= 24 * 60 * 60 * 1000) {
            return "00:00";
        }
        long totalSeconds = millisecond / 1000;
        int seconds = (int) (totalSeconds % 60);
        int minutes = (int) ((totalSeconds / 60) % 60);
        int hours = (int) (totalSeconds / 3600);
        StringBuilder stringBuilder = new StringBuilder();
        Formatter mFormatter = new Formatter(stringBuilder, Locale.getDefault());
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    /**
     * 当前是否使用WiFi
     *
     * @return
     */
    public static boolean isWifi() {
        ConnectivityManager connectivityManager = (ConnectivityManager) Utils.context().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI;
    }

    /**
     * 获取当前的Activity
     *
     * @param context
     * @return
     */
    public static Activity getActivity(Context context) {
        if (context == null) {
            return null;
        }

        if (context instanceof Activity) {
            return (Activity) context;
        } else if (context instanceof ContextThemeWrapper) {
            return getActivity(((ContextThemeWrapper) context).getBaseContext());
        } else if (context instanceof ContextWrapper) {
            return getActivity(((ContextWrapper) context).getBaseContext());
        }

        return null;
    }


    /**
     * 保存进度
     *
     * @param url
     * @param progress
     */
    public static void saveProgress(String url, long progress) {
        SPUtils.save(url, progress);

    }

    /**
     * 获取保存的进度
     *
     * @param url
     * @return
     */
    public static long getSavedProgress(String url) {
        return SPUtils.getLong(url, 0L);
    }

    /**
     * 清空进度
     *
     * @param url if url!=null clear this url progress
     */
    public static void clearSavedProgress(String url) {

        if (TextUtils.isEmpty(url)) {

            SPUtils.clear();
        } else {
            SPUtils.save(url, 0L);
        }
    }

    /**
     * 保存自动暂停状态
     *
     * @param url
     */
    public static void saveAutoPause(@NonNull String url) {
        SPUtils.save(url + "_autoPause", true);

    }

    /**
     * 获取保存的自动暂停状态
     *
     * @param url
     * @return
     */
    public static boolean isAutoPause(@NonNull String url) {
        return SPUtils.getBoolean(url + "_autoPause", false);
    }

    /**
     * 清空进度
     *
     * @param url if url!=null clear this url progress
     */
    public static void clearSavedAutoPause(String url) {

        if (TextUtils.isEmpty(url)) {

            SPUtils.clear();
        } else {
            SPUtils.save(url + "_autoPause", false);
        }
    }

    /**
     * 保存进度
     *
     * @param url
     */
    public static void saveLastUrl(String url) {
        SPUtils.save(Utils.SP_VIDEO_URL, url);

    }

    /**
     * 获取保存的进度
     *
     * @return
     */
    public static String getLastUrl() {
        return SPUtils.getString(Utils.SP_VIDEO_URL, "");
    }


    /**
     * 改变亮度并计算亮度百分比
     *
     * @param offset
     * @param brightnessStep
     * @return
     */
    public static int caculateBrightness(@NonNull Context context, float offset, float brightnessStep) {
        float mGestureDownBrightness = 0;

        Activity activity = VideoUtils.getActivity(context);
        if (activity != null) {
            WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
            if (lp.screenBrightness < 0) {
                try {
                    mGestureDownBrightness = Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS) / 255;
                    Timber.i("当前系统亮度：%s", mGestureDownBrightness);
                } catch (Settings.SettingNotFoundException e) {
                    e.printStackTrace();
                }
            } else {
                mGestureDownBrightness = lp.screenBrightness;
                Timber.i("当前页面亮度: ", mGestureDownBrightness);
            }

            float deltaV;

            if (offset < 0) {   //加亮度
                deltaV = mGestureDownBrightness + brightnessStep;
            } else {     // 减亮度
                deltaV = mGestureDownBrightness - brightnessStep;
            }

            WindowManager.LayoutParams params = activity.getWindow().getAttributes();
            if (deltaV >= 1) { //这和声音有区别，必须自己过滤一下负值
                deltaV = 1;
            } else if (deltaV <= 0) {
                deltaV = 0.01f;
            }

            params.screenBrightness = deltaV;

            activity.getWindow().setAttributes(params);
            //亮度百分比
            return (int) (deltaV * 100);
        }
        return 0;
    }

    /**
     * 计算播放位置
     *
     * @param offset
     * @param seekStep
     * @return
     */
    public static long caculatePlayPosition(float offset, int seekStep, long currentDuration, long totalDuration) {
        long mSeekTimePosition;

        if (offset > 0) {       //加进度
            mSeekTimePosition = currentDuration + seekStep * totalDuration / 100;
        } else {       //减进度
            mSeekTimePosition = currentDuration - seekStep * totalDuration / 100;
        }

        if (mSeekTimePosition > totalDuration) {
            mSeekTimePosition = totalDuration;
        } else if (mSeekTimePosition < 0) {
            mSeekTimePosition = 0;
        }

        return mSeekTimePosition;
    }

    /**
     * 计算声音百分比
     *
     * @param offset
     * @param volumeStep
     * @return
     */
    public static int caculateVolume(@NonNull AudioManager mAudioManager, float offset, int volumeStep) {
        int mGestureDownVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int max = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        int stepValue = volumeStep * max / 100;
        if (stepValue < 1) {  //为啥最大音量只有15？
            stepValue = 1;
        }

        int deltaV;

        if (offset < 0) {  //加声音
            deltaV = mGestureDownVolume + stepValue;
        } else {     //减声音
            deltaV = mGestureDownVolume - stepValue;
        }

        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, deltaV, 0);
        //百分比
        return deltaV * 100 / max;
    }

    /**
     * 显示或隐藏 ActionBar
     *
     * @param context
     * @param show
     */
    public static void showSupportActionBar(Context context, boolean show) {

        Timber.d("显示标题栏：%s", show);

        Activity activity = VideoUtils.getActivity(context);
        if (activity != null) {
            if (activity instanceof AppCompatActivity) {
                ActionBar ab = ((AppCompatActivity) activity).getSupportActionBar();

                if (ab != null) {

                    Timber.i("ActionBar 存在，%s", ab.getClass().getCanonicalName());

                    if (show) {
                        ab.show();

                        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                    } else {
                        ab.hide();

                        activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                                WindowManager.LayoutParams.FLAG_FULLSCREEN);
                    }
                }

            } else {

                android.app.ActionBar ab = activity.getActionBar();

                if (ab != null) {

                    Timber.i("ActionBar 存在，%s", ab.getClass().getCanonicalName());

                    if (show) {
                        ab.show();

                        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                    } else {
                        ab.hide();

                        activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                                WindowManager.LayoutParams.FLAG_FULLSCREEN);
                    }
                }
            }
        }

    }

    /**
     * 保持屏幕常亮
     *
     * @param context
     */
    public static void keepScreenOn(@lombok.NonNull Context context) {
        Activity activity = VideoUtils.getActivity(context);
        if (activity != null) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }
}
