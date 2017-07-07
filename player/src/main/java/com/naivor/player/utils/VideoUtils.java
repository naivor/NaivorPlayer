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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ContextThemeWrapper;
import android.text.TextUtils;

import com.naivor.player.VideoPlayer;

import java.util.Formatter;
import java.util.Locale;

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
    public static String formateTime(int millisecond) {
        if (millisecond <= 0 || millisecond >= 24 * 60 * 60 * 1000) {
            return "00:00";
        }
        int totalSeconds = millisecond / 1000;
        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;
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
    public static void saveProgress(Context context, String url, int progress) {
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
    public static int getSavedProgress(Context context, String url) {
        if (!VideoPlayer.SAVE_PROGRESS) return 0;

        SPUtils.init(context, SP_VIDEO);

        return SPUtils.getInt(url, 0);
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
}
