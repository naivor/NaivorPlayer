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
 * 播放控制器
 *
 * Created by tianlai on 17-7-6.
 */

public interface PlayController {

    /**
     * 播放
     *
     */
    void start();

    /**
     * 暂停播放
     *
     */
    void pause();

    /**
     * 继续播放
     *
     */
    void resume();

    /**
     * 上一曲
     */
    void previous();

    /**
     * 下一曲
     */
    void next();


    /**
     * 停止播放
     */
    void stop();

    /**
     * 重新播放
     */
    void rePlay();

}
