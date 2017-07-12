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

import android.content.Context;
import android.view.MotionEvent;

import com.naivor.player.constant.ScreenState;
import com.naivor.player.constant.VideoState;

import lombok.NonNull;
import timber.log.Timber;

/**
 * 控制栏的触摸处理
 * <p>
 * Created by tianlai on 17-7-12.
 */

public class ControlTouchProsser {

    public static final int THRESHOLD = 80;

    private Context context;

    protected boolean mTouchingProgressBar;
    protected float mDownX;
    protected float mDownY;
    protected boolean mChangeVolume;
    protected boolean mChangePosition;
    protected boolean mChangeBrightness;

    //界面宽高
    protected int mScreenWidth;
    protected int mScreenHeight;

    public ControlTouchProsser(@NonNull Context context) {
        this.context = context;

        mScreenWidth = context.getResources().getDisplayMetrics().widthPixels;
        mScreenHeight = context.getResources().getDisplayMetrics().heightPixels;
    }

    /**
     * 处理触摸事件
     *
     * @param event
     * @param onControllViewListener
     * @param scrubbing
     * @return
     */
    public boolean processTouchEvent(MotionEvent event, OnControllViewListener onControllViewListener, boolean scrubbing) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Timber.d(" actionDown");

                mTouchingProgressBar = true;

                mDownX = x;
                mDownY = y;
                mChangeVolume = false;
                mChangePosition = false;
                mChangeBrightness = false;
                break;
            case MotionEvent.ACTION_MOVE:
                Timber.d(" actionMove");

                if (onControllViewListener != null) {
                    float deltaX = x - mDownX;
                    float deltaY = y - mDownY;
                    float absDeltaX = Math.abs(deltaX);
                    float absDeltaY = Math.abs(deltaY);

                    if (onControllViewListener.getScreenState() == ScreenState.SCREEN_WINDOW_FULLSCREEN) {
                        if (!mChangePosition && !mChangeVolume && !mChangeBrightness) {
                            if (absDeltaX > THRESHOLD || absDeltaY > THRESHOLD) {
                                scrubbing = true; //停止更新进度
                                if (absDeltaX >= THRESHOLD) {
                                    // 全屏模式下的CURRENT_STATE_ERROR状态下,不响应进度拖动事件.
                                    // 否则会因为mediaplayer的状态非法导致App Crash
                                    if (onControllViewListener.getCurrentState() != VideoState.CURRENT_STATE_ERROR) {
                                        mChangePosition = true;
                                    }
                                } else {
                                    //如果y轴滑动距离超过设置的处理范围，那么进行滑动事件处理
                                    if (mDownX < mScreenWidth * 0.5f) {//左侧改变亮度
                                        mChangeBrightness = true;
                                    } else {//右侧改变声音
                                        mChangeVolume = true;
                                    }
                                }
                            }
                        }
                    }
                    if (mChangePosition) {
                        onControllViewListener.changePlayingPosition(deltaX, mScreenWidth);
                    }

                    if (mChangeVolume) {  //改变音量
                        onControllViewListener.changeVolume(-deltaY, mScreenHeight);
                    }

                    if (mChangeBrightness) {  //改变亮度
                        onControllViewListener.changeBrightness(-deltaY, mScreenHeight);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                Timber.d(" actionUp ");

                mTouchingProgressBar = false;

                if (onControllViewListener != null) {
                    onControllViewListener.onTouchScreenEnd();
                }

                scrubbing = false; //继续更新进度

                break;
        }

        return false;
    }
}
