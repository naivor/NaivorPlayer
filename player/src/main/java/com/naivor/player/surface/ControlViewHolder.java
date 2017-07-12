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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.naivor.player.R;

import lombok.NonNull;
import timber.log.Timber;

/**
 * 控制栏view容器
 * <p>
 * Created by tianlai on 17-7-10.
 */

public class ControlViewHolder {
    View rootView;

    ImageView playButton;
    TextView durationView;
    TextView positionView;
    SeekBar timeBar;
    ImageView fullScreenButton;
    ProgressBar thumbBar;

    LinearLayout topLayout;
    LinearLayout buttomLayout;

    public ControlViewHolder(@NonNull View rootView) {
        this.rootView = rootView;

        playButton = rootView.findViewById(R.id.iv_start);
        durationView = rootView.findViewById(R.id.tv_total);
        positionView = rootView.findViewById(R.id.tv_current);
        timeBar = rootView.findViewById(R.id.sb_progress);
        fullScreenButton = rootView.findViewById(R.id.iv_fullscreen);
        thumbBar = rootView.findViewById(R.id.pb_loading);
        topLayout = rootView.findViewById(R.id.ll_top);
        buttomLayout = rootView.findViewById(R.id.ll_bottom);

    }

    public boolean isShown() {
        return topLayout.isShown() || buttomLayout.isShown();
    }

    /**
     * 显示控制栏
     */
    public void show() {

        Timber.d("显示");

        if (topLayout != null) {
            topLayout.setVisibility(View.VISIBLE);
        }
        if (buttomLayout != null) {
            buttomLayout.setVisibility(View.VISIBLE);
        }

        if (playButton != null) {
            boolean isThumb = false;

            if (thumbBar != null && thumbBar.isShown()) {
                isThumb = true;
            }

            if (isThumb) {
                playButton.setVisibility(View.GONE);
            } else {
                playButton.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * 隐藏控制栏
     *
     * @param isStart
     */
    public void hide(boolean isStart) {

        Timber.d("隐藏，是否开始：%s", isStart);

        if (topLayout != null) {
            topLayout.setVisibility(View.GONE);
        }
        if (buttomLayout != null) {
            buttomLayout.setVisibility(View.GONE);
        }

        if (playButton != null && isStart) {
            playButton.setVisibility(View.GONE);
        }
    }

    /**
     * 显示缓冲
     *
     * @param isThumb
     */
    public void showThumb(boolean isThumb) {

        Timber.d("缓冲：%s", isThumb);

        boolean isShown = false;

        if (buttomLayout != null && buttomLayout.isShown()) {
            isShown = true;
        }

        if (thumbBar != null) {
            if (isThumb) {
                playButton.setVisibility(View.GONE);
                thumbBar.setVisibility(View.VISIBLE);
            } else {
                thumbBar.setVisibility(View.GONE);

                if (isShown) {
                    playButton.setVisibility(View.VISIBLE);
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

        if (playButton != null) {

            playButton.setImageResource(res);

            playButton.setVisibility(View.VISIBLE);
        }
    }

}
