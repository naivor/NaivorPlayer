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

package com.naivor.player.constant;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 视频状态类
 * <p>
 * Created by tianlai on 17-7-6.
 */

public final class VideoState {

    private VideoState() {
    }

    public static final int CURRENT_STATE_ORIGIN = 0;
    public static final int CURRENT_STATE_PREPARING = 1;
    public static final int CURRENT_STATE_PLAYING_BUFFERING = 2;
    public static final int CURRENT_STATE_PLAYING = 3;
    public static final int CURRENT_STATE_PAUSE = 4;
    public static final int CURRENT_STATE_COMPLETE = 5;
    public static final int CURRENT_STATE_ERROR = 6;


    @IntDef({CURRENT_STATE_ORIGIN, CURRENT_STATE_PREPARING, CURRENT_STATE_PLAYING,
            CURRENT_STATE_PLAYING_BUFFERING, CURRENT_STATE_PAUSE, CURRENT_STATE_COMPLETE, CURRENT_STATE_ERROR})
    @Retention(RetentionPolicy.SOURCE)
    public @interface VideoStateValue {
    }


    /**
     * 获取视频状态的名称
     *
     * @param state
     * @return
     */
    public static String getVideoStateName(@VideoStateValue int state) {
        String stateName = null;

        switch (state) {
            case CURRENT_STATE_ORIGIN:
                stateName = "初始状态";
                break;
            case CURRENT_STATE_PREPARING:
                stateName = "准备中";
                break;
            case CURRENT_STATE_PLAYING:
                stateName = "正在播放";
                break;
            case CURRENT_STATE_PLAYING_BUFFERING:
                stateName = "正在缓冲";
                break;
            case CURRENT_STATE_PAUSE:
                stateName = "暂停中";
                break;
            case CURRENT_STATE_COMPLETE:
                stateName = "播放完成";
                break;
            case CURRENT_STATE_ERROR:
                stateName = "播放出错";
                break;
            default:
                break;
        }

        return stateName;
    }
}
