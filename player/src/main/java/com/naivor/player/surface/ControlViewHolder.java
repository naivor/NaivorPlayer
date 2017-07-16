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

import lombok.NonNull;
import timber.log.Timber;

/**
 * 控制栏view容器
 * <p>
 * Created by tianlai on 17-7-10.
 */

public class ControlViewHolder {
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

    //屏幕状态
    protected @ScreenState.ScreenStateValue
    int state;

    public ControlViewHolder(@NonNull View rootView) {
        this.rootView = rootView;

        playBtn = rootView.findViewById(R.id.iv_start);

        buttomLayout = rootView.findViewById(R.id.ll_bottom);
        durationView = rootView.findViewById(R.id.tv_total);
        positionView = rootView.findViewById(R.id.tv_current);
        timeBar = rootView.findViewById(R.id.sb_progress);
        fullScreenBtn = rootView.findViewById(R.id.iv_fullscreen);
        thumbBar = rootView.findViewById(R.id.pb_loading);

        topLayout = rootView.findViewById(R.id.fl_top);
        llTitle = rootView.findViewById(R.id.ll_title);
        backBtn = rootView.findViewById(R.id.iv_back);
        videoTitle = rootView.findViewById(R.id.tv_title);
        rlTiny = rootView.findViewById(R.id.rl_tiny);
        tinyExitBtn = rootView.findViewById(R.id.iv_tiny_exit);
        tinyCloseBtn = rootView.findViewById(R.id.iv_tiny_close);

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
            if (state == ScreenState.SCREEN_WINDOW_FULLSCREEN) {  //全屏显示标题栏
                topLayout.setVisibility(View.VISIBLE);

                if (llTitle != null) {
                    llTitle.setVisibility(View.VISIBLE);
                }
                if (rlTiny != null) {
                    rlTiny.setVisibility(View.GONE);
                }
            } else if (state == ScreenState.SCREEN_WINDOW_TINY) {  //小窗显示标题栏
                topLayout.setVisibility(View.VISIBLE);
                if (llTitle != null) {
                    llTitle.setVisibility(View.GONE);
                }
                if (rlTiny != null) {
                    rlTiny.setVisibility(View.VISIBLE);
                }
            } else {
                topLayout.setVisibility(View.GONE);
            }

        }

        if (buttomLayout != null) {
            if (state == ScreenState.SCREEN_WINDOW_TINY) {  //小窗隐藏底部控制栏
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
    public void showThumb(boolean isThumb) {

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
    public void showPlayButton(@DrawableRes int res) {

        if (thumbBar != null) {
            thumbBar.setVisibility(View.GONE);
        }

        if (playBtn != null) {

            playBtn.setImageResource(res);

            playBtn.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 重置全屏按钮
     *
     * @param state
     */
    public void showFullScreenButton(@ScreenState.ScreenStateValue int state) {
        this.state = state;

        if (fullScreenBtn != null) {
            fullScreenBtn.setVisibility(View.VISIBLE);
            if (state == ScreenState.SCREEN_WINDOW_FULLSCREEN) {
                fullScreenBtn.setBackgroundResource(R.drawable.ic_fullscreen_exit_selector);
            } else {
                fullScreenBtn.setBackgroundResource(R.drawable.ic_fullscreen_selector);
            }

            show();
        }
    }

}
