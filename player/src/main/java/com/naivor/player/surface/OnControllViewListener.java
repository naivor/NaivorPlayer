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

package com.naivor.player.surface;

import android.view.View;

import com.naivor.player.constant.ScreenState;
import com.naivor.player.constant.VideoState;

/**
 * 控制栏的点击监听
 * <p>
 * Created by tianlai on 17-7-11.
 */

public interface OnControllViewListener {

    /**
     * 点击
     *
     * @param view
     */
    void onclick(View view);

    /**
     * 全屏点击事件
     */
    void onFullScreenClick();

    /**
     * 控制栏显示，隐藏状态监听
     *
     * @param visibility
     */
    void onVisibilityChange(int visibility);

    /**
     * 播放进度回调
     *
     * @param progress
     */
    void onProgress(int progress, int bufferedProgress);

    /**
     * 改变音量
     *
     * @param offset
     * @param total
     */
    void changeVolume(float offset, float total);

    /**
     * 改变亮度
     *
     * @param offset
     * @param total
     */
    void changeBrightness(float offset, float total);

    /**
     * 改变播放位置
     */
    void changePlayingPosition(float offset, float total);

    /**
     * 屏幕状态
     *
     * @return
     */
    @ScreenState.ScreenStateValue
    int getScreenState();

    /**
     * 播放状态
     *
     * @return
     */
    @VideoState.VideoStateValue
    int getCurrentState();

    /**
     * 结束触碰屏幕
     */
    void onTouchScreenEnd();
}
