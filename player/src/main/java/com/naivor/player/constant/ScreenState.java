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
 * 视图状态，包括窗口和布局
 * <p>
 * Created by tianlai on 17-7-6.
 */

public final class ScreenState {

    public static final int SCREEN_LAYOUT_ORIGIN = 0;
    public static final int SCREEN_LAYOUT_LIST = 1;
    public static final int SCREEN_WINDOW_FULLSCREEN = 2;
    public static final int SCREEN_WINDOW_TINY = 3;


    /**
     * 屏幕窗口
     */
    @IntDef({SCREEN_LAYOUT_ORIGIN, SCREEN_LAYOUT_LIST, SCREEN_WINDOW_FULLSCREEN,
            SCREEN_WINDOW_TINY})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ScreenStateValue {
    }

    private ScreenState() {
    }


}
