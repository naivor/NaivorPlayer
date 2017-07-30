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

import android.support.annotation.DrawableRes;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.naivor.player.R;
import com.naivor.player.constant.ScreenState;
import com.naivor.player.constant.VideoState;

import lombok.NonNull;
import timber.log.Timber;

/**
 * 控制栏view容器
 * <p>
 * Created by tianlai on 17-7-10.
 */

public class ControlViewHolder {
    protected static int RES_VIDEO_PLAY = R.drawable.ic_play_selector;
    protected static int RES_VIDEO_PAUSE = R.drawable.ic_pause_selector;
    protected static int RES_VIDEO_ERROR = R.drawable.ic_error_selector;

    protected View rootView;

    protected ImageView playBtn;

    protected LinearLayout buttomLayout;
    protected TextView durationView;
    protected TextView positionView;
    protected SeekBar timeBar;
    protected Button fullScreenBtn;
    protected ProgressBar thumbBar;

    protected FrameLayout topLayout;
    protected LinearLayout llTitle;
    protected Button backBtn;
    protected TextView videoTitle;
    protected RelativeLayout rlTiny;
    protected Button tinyExitBtn;
    protected Button tinyCloseBtn;

    @VideoState.VideoStateValue
    protected int videoState;
    @ScreenState.ScreenStateValue
    protected int screenState;

    public ControlViewHolder(@NonNull View rootView) {
        this.rootView = rootView;

        playBtn = (ImageView) rootView.findViewById(R.id.iv_start);

        buttomLayout = (LinearLayout) rootView.findViewById(R.id.ll_bottom);
        durationView = (TextView) rootView.findViewById(R.id.tv_total);
        positionView = (TextView) rootView.findViewById(R.id.tv_current);
        timeBar = (SeekBar) rootView.findViewById(R.id.sb_progress);
        fullScreenBtn = (Button) rootView.findViewById(R.id.iv_fullscreen);
        thumbBar = (ProgressBar) rootView.findViewById(R.id.pb_loading);

        topLayout = (FrameLayout) rootView.findViewById(R.id.fl_top);
        llTitle = (LinearLayout) rootView.findViewById(R.id.ll_title);
        backBtn = (Button) rootView.findViewById(R.id.iv_back);
        videoTitle = (TextView) rootView.findViewById(R.id.tv_title);
        rlTiny = (RelativeLayout) rootView.findViewById(R.id.rl_tiny);
        tinyExitBtn = (Button) rootView.findViewById(R.id.iv_tiny_exit);
        tinyCloseBtn = (Button) rootView.findViewById(R.id.iv_tiny_close);

    }

    /**
     * 更新播放状态
     *
     * @param videoState
     */
    public void updateVideoState(@ScreenState.ScreenStateValue int videoState) {
        this.videoState = videoState;

        Timber.d("更新播放状态:%s", VideoState.getVideoStateName(videoState));

        switch (videoState) {
            case VideoState.CURRENT_STATE_ORIGIN:
                reset();
                showPlayButton(RES_VIDEO_PLAY);
                break;
            case VideoState.CURRENT_STATE_PREPARING:
                showThumb(true);
                break;
            case VideoState.CURRENT_STATE_PLAYING:
                showPlayButton(RES_VIDEO_PAUSE);
                showThumb(false);
                if (!isShown()) {
                    hide(true);
                }
                break;
            case VideoState.CURRENT_STATE_PAUSE:
                showPlayButton(RES_VIDEO_PLAY);
                showThumb(false);
                break;
            case VideoState.CURRENT_STATE_PLAYING_BUFFERING:
                showThumb(true);
                break;
            case VideoState.CURRENT_STATE_ERROR:
                reset();
                showPlayButton(RES_VIDEO_ERROR);
                break;
            case VideoState.CURRENT_STATE_COMPLETE:
                reset();
                showPlayButton(RES_VIDEO_PLAY);
                break;
            default:
                break;
        }

    }

    /**
     * 更新屏幕状态
     *
     * @param screenState
     */
    public void updateScreenState(@ScreenState.ScreenStateValue int screenState) {
        this.screenState = screenState;

        Timber.d("更新屏幕状态:%s", ScreenState.getScreenStateName(screenState));

        if (fullScreenBtn != null) {
            if (screenState == ScreenState.SCREEN_WINDOW_FULLSCREEN_LOCK) {
                fullScreenBtn.setVisibility(View.GONE);
            } else {
                fullScreenBtn.setVisibility(View.VISIBLE);

                if (screenState == ScreenState.SCREEN_WINDOW_FULLSCREEN) {
                    fullScreenBtn.setBackgroundResource(R.drawable.ic_fullscreen_exit_selector);
                } else {
                    fullScreenBtn.setBackgroundResource(R.drawable.ic_fullscreen_selector);
                }
            }

        }
    }

    /**
     * 控制栏是否显示
     *
     * @return
     */
    public boolean isShown() {
        boolean isShown = false;

        if (buttomLayout != null) {
            isShown = buttomLayout.isShown();
        }

        if (topLayout != null) {
            isShown = isShown || topLayout.isShown();
        }

        return isShown;
    }


    /**
     * 显示控制栏
     */
    public void show() {

        Timber.d("显示");

        if (topLayout != null) {
            if (screenState == ScreenState.SCREEN_WINDOW_FULLSCREEN
                    || screenState == ScreenState.SCREEN_WINDOW_FULLSCREEN_LOCK) {  //全屏显示标题栏

                topLayout.setVisibility(View.VISIBLE);

                if (llTitle != null) {
                    llTitle.setVisibility(View.VISIBLE);
                }
                if (rlTiny != null) {
                    rlTiny.setVisibility(View.GONE);
                }
            } else if (screenState == ScreenState.SCREEN_WINDOW_TINY
                    || screenState == ScreenState.SCREEN_LAYOUT_LIST_TINY) {  //小窗显示标题栏

                topLayout.setVisibility(View.VISIBLE);

                if (llTitle != null) {
                    llTitle.setVisibility(View.GONE);
                }
                if (rlTiny != null) {
                    rlTiny.setVisibility(View.VISIBLE);

                    if (tinyExitBtn != null) {
                        if (screenState == ScreenState.SCREEN_LAYOUT_LIST_TINY) {
                            tinyExitBtn.setVisibility(View.GONE);
                        } else {
                            tinyExitBtn.setVisibility(View.VISIBLE);
                        }
                    }
                }
            } else {
                topLayout.setVisibility(View.GONE);
            }

        }

        if (buttomLayout != null) {
            if (screenState == ScreenState.SCREEN_WINDOW_TINY || screenState == ScreenState.SCREEN_LAYOUT_LIST_TINY) {  //小窗隐藏底部控制栏
                buttomLayout.setVisibility(View.GONE);
            } else {
                buttomLayout.setVisibility(View.VISIBLE);
            }

        }

        if (playBtn != null) {
            boolean isThumb = false;

            if (thumbBar != null && thumbBar.isShown()) {
                isThumb = true;
            }

            if (isThumb) {
                playBtn.setVisibility(View.GONE);
            } else {
                playBtn.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * 隐藏控制栏
     *
     * @param hidePlayButton
     */
    public void hide(boolean hidePlayButton) {

        Timber.d("隐藏，是否开始：%s", hidePlayButton);

        if (topLayout != null) {
            topLayout.setVisibility(View.GONE);
        }
        if (buttomLayout != null) {
            buttomLayout.setVisibility(View.GONE);
        }

        if (playBtn != null) {
            boolean isThumb = false;

            if (thumbBar != null && thumbBar.isShown()) {
                isThumb = true;
            }

            if (isThumb || hidePlayButton) {
                playBtn.setVisibility(View.GONE);
            } else {
                playBtn.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * 显示缓冲
     *
     * @param isThumb
     */
    protected void showThumb(boolean isThumb) {

        Timber.d("缓冲：%s", isThumb);

        if (thumbBar != null) {
            if (isThumb) {
                playBtn.setVisibility(View.GONE);
                thumbBar.setVisibility(View.VISIBLE);
            } else {
                thumbBar.setVisibility(View.GONE);

                if (isShown()) {
                    playBtn.setVisibility(View.VISIBLE);
                }
            }
        }

    }

    /**
     * 重置播放按钮
     */
    protected void showPlayButton(@DrawableRes int res) {

        if (thumbBar != null) {
            thumbBar.setVisibility(View.GONE);
        }

        if (playBtn != null) {

            playBtn.setImageResource(res);

            playBtn.setVisibility(View.VISIBLE);
        }
    }


    /**
     * 重置
     */
    protected void reset() {
        if (isShown()) {

            hide(false);

            showThumb(false);

            if (timeBar != null) {
                timeBar.setProgress(0);
            }
        }
    }
}
