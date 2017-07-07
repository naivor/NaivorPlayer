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

/**
 * 位置控制器
 *
 * Created by tianlai on 17-7-7.
 */

public interface PositionController {

    /**
     * 从某个位置开始播放
     *
     * @param millisecond
     */
    void seekTo(long millisecond);

    /**
     * 快进
     */
    void fastward(long millisecond);

    /**
     * 快退
     */
    void backward(long millisecond);

    /**
     * 获取当前播放位置
     *
     * @return
     */
    long getCurrentDuration();

    /**
     * 总的播放位置
     *
     * @return
     */
    long getTotalDuration();
}
