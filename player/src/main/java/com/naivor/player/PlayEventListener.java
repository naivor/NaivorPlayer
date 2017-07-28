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

package com.naivor.player;

import android.view.View;

import com.naivor.player.constant.ScreenState;
import com.naivor.player.constant.VideoState;

/**
 * 播放事件，供用户使用
 * <p>
 * Created by tianlai on 17-7-14.
 */

public interface PlayEventListener {

    /**
     * 视频播放状态改变
     *
     * @param videoState
     */
    void onVideoState(@VideoState.VideoStateValue int videoState);

    /**
     * 屏幕状态改变
     *
     * @param screenState
     */
    void onScreenState(@ScreenState.ScreenStateValue int screenState);

    /**
     * 控制栏控件点击事件
     *
     * @param view
     */
    void onControllViewClick(View view);

    /**
     * 控制栏被呼出
     *
     * @param isShow
     */
    void onControllViewShown(boolean isShow);
}
