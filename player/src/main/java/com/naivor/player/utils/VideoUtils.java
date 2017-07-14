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

import android.content.Context;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ContextThemeWrapper;
import android.text.TextUtils;
import android.view.WindowManager;

import com.naivor.player.VideoPlayer;

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
    private static final String SP_VIDEO = "SP_VIDEO";

    private VideoUtils() {

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
     * @param context
     * @return
     */
    public static boolean isWifi(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI;
    }

    /**
     * 获取当前的Activity
     *
     * @param context
     * @return
     */
    public static AppCompatActivity getActivity(Context context) {
        if (context == null) return null;
        if (context instanceof AppCompatActivity) {
            return (AppCompatActivity) context;
        } else if (context instanceof ContextThemeWrapper) {
            return getActivity(((ContextThemeWrapper) context).getBaseContext());
        }
        return null;
    }


    /**
     * 保存进度
     *
     * @param context
     * @param url
     * @param progress
     */
    public static void saveProgress(Context context, String url, long progress) {
        if (!VideoPlayer.SAVE_PROGRESS) return;

        SPUtils.init(context, SP_VIDEO);

        SPUtils.save(url, progress);

    }

    /**
     * 获取保存的进度
     *
     * @param context
     * @param url
     * @return
     */
    public static long getSavedProgress(Context context, String url) {
        if (!VideoPlayer.SAVE_PROGRESS) return 0;

        SPUtils.init(context, SP_VIDEO);

        return SPUtils.getLong(url, 0);
    }

    /**
     * 清空进度
     *
     * @param context context
     * @param url     if url!=null clear this url progress
     */
    public static void clearSavedProgress(Context context, String url) {
        SPUtils.init(context, SP_VIDEO);

        if (TextUtils.isEmpty(url)) {

            SPUtils.clear();
        } else {
            SPUtils.save(url, 0);
        }
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

        WindowManager.LayoutParams lp = VideoUtils.getActivity(context).getWindow().getAttributes();
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

        WindowManager.LayoutParams params = VideoUtils.getActivity(context).getWindow().getAttributes();
        if (deltaV >= 1) {//这和声音有区别，必须自己过滤一下负值
            deltaV = 1;
        } else if (deltaV <= 0) {
            deltaV = 0.01f;
        }

        params.screenBrightness = deltaV;

        VideoUtils.getActivity(context).getWindow().setAttributes(params);
        //亮度百分比
        return (int) (deltaV * 100);
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

        ActionBar ab = VideoUtils.getActivity(context).getSupportActionBar();

        if (ab != null) {

            Timber.i("ActionBar 存在，%s", ab.getClass().getCanonicalName());

            if (show) {
                ab.show();

                VideoUtils.getActivity(context).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            } else {
                ab.hide();

                VideoUtils.getActivity(context).getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }

        }

    }
}
