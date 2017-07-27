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
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import com.naivor.player.R;
import com.naivor.player.VideoPlayer;
import com.naivor.player.constant.ScreenState;

import java.lang.ref.SoftReference;

import lombok.NonNull;
import timber.log.Timber;

/**
 * Created by naivor on 17-7-27.
 */

public final class ListVideoUtils {
    protected static SoftReference<VideoPlayer> reference;

    protected static boolean isOpendInTiny;

    /**
     * 初始化
     *
     * @param activity
     */
    public static void init(@NonNull Activity activity) {

        VideoPlayer videoPlayer = activity.findViewById(R.id.naivor_list_tiny_window);

        if (videoPlayer == null) {
            View rootView = activity.findViewById(Window.ID_ANDROID_CONTENT);

            if (rootView != null) {
                activity.getLayoutInflater().inflate(R.layout.list_tiny_window, (ViewGroup) rootView, true);
                videoPlayer = activity.findViewById(R.id.naivor_list_tiny_window);
            }
        }

        reference = new SoftReference<>(videoPlayer);
    }


    /**
     * 小窗播放滑出屏幕的视频
     *
     * @param url
     * @param title
     */
    public static void startWindowTiny(@NonNull String url, @NonNull String title) {
        Timber.v("小窗播放滑出屏幕的视频");

        if (!isOpendInTiny) {

            VideoPlayer videoPlayer = reference.get();

            if (videoPlayer != null) {
                videoPlayer.setVisibility(View.VISIBLE);

                videoPlayer.setScreenState(ScreenState.SCREEN_WINDOW_TINY);
                videoPlayer.setUp(url, title);
                videoPlayer.start();

                isOpendInTiny = true;
            }
        }
    }

    /**
     * 停止播放并退出小窗
     */
    public static void stopPlayAndCloseTiny() {
        Timber.v("停止播放并退出小窗");

        if (isOpendInTiny) {

            VideoPlayer videoPlayer = reference.get();

            if (videoPlayer != null) {
                if (videoPlayer.getScreenState() == ScreenState.SCREEN_WINDOW_TINY) {
                    videoPlayer.setVisibility(View.GONE);

                    videoPlayer.stopAndReset();
                }

                isOpendInTiny = false;
            }
        }
    }
}
