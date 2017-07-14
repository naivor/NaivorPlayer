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

public class ControlTouchProcessor {

    public static final int THRESHOLD = 20; //判断事件是进度，亮度，声音的最小范围

    public static final int VOLUME_STEP = 1; // 音量增加的步长
    public static final float BRIGHTNESS_STEP = 0.02f; // 亮度增加的步长
    public static final int SEEK_STEP = 2; //进度增加的步长

    public static final int MOVE_DAMP = 10; //滑动伐值

    protected Context context;

    protected float mDownX;
    protected float mDownY;
    protected boolean mChangeVolume;
    protected boolean mChangePosition;
    protected boolean mChangeBrightness;

    protected boolean isActionProcessing; // 是否有操作正在处理

    //界面宽高
    protected int mScreenWidth;

    protected long time;

    public ControlTouchProcessor(@NonNull Context context) {
        this.context = context;

        mScreenWidth = context.getResources().getDisplayMetrics().widthPixels;
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

                time = System.currentTimeMillis();

                mDownX = x;
                mDownY = y;

                isActionProcessing = false;
                mChangeVolume = false;
                mChangePosition = false;
                mChangeBrightness = false;

                break;
            case MotionEvent.ACTION_MOVE:
                Timber.d(" actionMove");

                if (onControllViewListener != null) {

                    if (onControllViewListener.getScreenState() == ScreenState.SCREEN_WINDOW_FULLSCREEN) {
                        float deltaX = x - mDownX;
                        float deltaY = y - mDownY;

                        float absDeltaX = Math.abs(deltaX);
                        float absDeltaY = Math.abs(deltaY);

                        if (!isActionProcessing) {  //判断应该处理进度，音量，亮度中的哪个事件
                            if (absDeltaX >= absDeltaY) {         //横向滑动
                                if (absDeltaX >= THRESHOLD) {
                                    scrubbing = true;    //停止更新进度
                                    if (onControllViewListener.getCurrentState() != VideoState.CURRENT_STATE_ERROR) {
                                        mChangePosition = true;   //改变进度
                                        isActionProcessing = true;
                                    }
                                }
                            } else {     //纵向滑动
                                if (absDeltaY >= THRESHOLD) {
                                    if (mDownX < mScreenWidth * 0.5f) {       //左侧改变亮度
                                        mChangeBrightness = true;
                                    } else {        //右侧改变声音
                                        mChangeVolume = true;
                                    }
                                    isActionProcessing = true;
                                }
                            }
                        } else {   //事件处理
                            if (mChangePosition) {  //进度
                                if (absDeltaX >= MOVE_DAMP) {
                                    onControllViewListener.changePlayingPosition(deltaX, SEEK_STEP);
                                    mDownX = x;
                                }
                            } else if (mChangeBrightness) {  //亮度
                                if (absDeltaY >= MOVE_DAMP) {
                                    onControllViewListener.changeBrightness(deltaY, BRIGHTNESS_STEP);
                                    mDownY = y;
                                }
                            } else if (mChangeVolume) {  //声音
                                if (absDeltaY >= MOVE_DAMP) {
                                    onControllViewListener.changeVolume(deltaY, VOLUME_STEP);
                                    mDownY = y;
                                }
                            }
                        }


                    }

                }
                break;
            case MotionEvent.ACTION_UP:
                Timber.d(" actionUp ");

                if (onControllViewListener != null) {
                    onControllViewListener.onTouchScreenEnd();
                }

                scrubbing = false; //继续更新进度

                long between = System.currentTimeMillis() - time;
                if (between < 100) {  //视为点击事件
                    return false;
                }

                break;
        }

        return true;
    }
}
