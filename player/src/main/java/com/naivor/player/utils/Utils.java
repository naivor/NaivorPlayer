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
import android.util.Log;

import com.naivor.player.BuildConfig;

import timber.log.Timber;

/**
 * 日志工具
 * <p>
 * Created by tianlai on 17-7-10.
 */

public final class Utils {
    private static Context context;

    public static final String SP_VIDEO = "SP_VIDEO";
    public static final String SP_VIDEO_URL = "SP_VIDEO_URL";

    private Utils() {
    }


    /**
     * 初始化
     */
    public static void init(Context pContext) {
        context = pContext.getApplicationContext();

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        } else {
            Timber.plant(new CrashReportingTree());
        }

        SPUtils.init(context, SP_VIDEO);


    }

    /**
     * @return
     */
    public static Context context() {
        return context;
    }


    /**
     * 崩溃的日志处理
     */
    private static class CrashReportingTree extends Timber.Tree {
        @Override
        protected void log(int priority, String tag, String message, Throwable t) {
            if (priority != Log.WARN || priority != Log.ERROR) {
                return;
            }

        }
    }
}
