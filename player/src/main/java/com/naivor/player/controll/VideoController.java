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

package com.naivor.player.controll;

import com.naivor.player.constant.OrientationState;
import com.naivor.player.constant.ScreenState;

/**
 * 视频控制器
 * <p>
 * Created by tianlai on 17-7-7.
 */

public interface VideoController extends PositionController, PlayController {


    /**
     * 设置视频播放时窗口的方向
     *
     * @param windowType
     * @param orientation
     */
    void setOrientation(@ScreenState.ScreenStateType int windowType, @OrientationState.OrientationType int orientation);


    /**
     * 是否正在播放
     *
     * @return
     */
    boolean isPlaying();

    /**
     * 是否暂停状态
     *
     * @return
     */
    boolean isPause();

    /**
     * 是否准备状态
     *
     * @return
     */
    boolean isPrepare();

    /**
     * 是否缓冲状态
     *
     * @return
     */
    boolean isBuffering();
}
