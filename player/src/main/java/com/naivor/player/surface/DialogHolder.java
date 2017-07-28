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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.naivor.player.R;
import com.naivor.player.controll.VideoController;
import com.naivor.player.utils.VideoUtils;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import timber.log.Timber;

/**
 * 对话框容器
 * <p>
 * Created by tianlai on 17-7-14.
 */

public class DialogHolder {
    protected Context context;

    protected VideoController controller;

    // 亮度对话框
    protected Dialog mBrightnessDialog;
    protected ProgressBar mDialogBrightnessProgressBar;
    protected TextView mDialogBrightnessTextView;
    protected ImageView mDialogBrightnessImageView;

    // 拖动进度对话框
    protected Dialog mProgressDialog;
    protected ProgressBar mDialogProgressBar;
    protected TextView mDialogSeekTime;
    protected TextView mDialogTotalTime;
    protected ImageView mDialogIcon;

    // 音量进度对话框
    protected Dialog mVolumeDialog;
    protected ProgressBar mDialogVolumeProgressBar;
    protected TextView mDialogVolumeTextView;
    protected ImageView mDialogVolumeImageView;

    //非WIFI对话框
    protected Dialog wifidialog;

    @Getter
    @Setter
    protected boolean isPlayWithNotWifi = false;  //非wifi环境是否播放

    public DialogHolder(@NonNull Context context, @NonNull VideoController controller) {
        this.context = context;
        this.controller = controller;
    }

    /**
     * 网络非wifi提示
     */
    public void showNotWifiDialog() {
        if (wifidialog == null) {
            wifidialog = new AlertDialog.Builder(context)
                    .setMessage(context.getResources().getString(R.string.tips_not_wifi))
                    .setPositiveButton(context.getResources().getString(R.string.tips_not_wifi_confirm), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            controller.prepareSource();
                            isPlayWithNotWifi = true;
                        }
                    })
                    .setNegativeButton(context.getResources().getString(R.string.tips_not_wifi_cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            controller.backOriginWindow();
                        }
                    })
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            dialog.dismiss();
                            controller.backOriginWindow();
                        }
                    })
                    .create();
        }

        if (!wifidialog.isShowing()) {
            wifidialog.show();
        }

    }


    /**
     * 显示拖动进度对话框
     */
    public void showProgressDialog(float deltaX, long seekTimePosition, long totalTimeDuration) {

        if (mProgressDialog == null) {
            View localView = LayoutInflater.from(context).inflate(R.layout.dialog_progress, null);
            mDialogProgressBar = ((ProgressBar) localView.findViewById(R.id.duration_progressbar));
            mDialogSeekTime = ((TextView) localView.findViewById(R.id.tv_current));
            mDialogTotalTime = ((TextView) localView.findViewById(R.id.tv_duration));
            mDialogIcon = ((ImageView) localView.findViewById(R.id.duration_image_tip));
            mProgressDialog = createDialogWithView(localView);
        }

        if (!mProgressDialog.isShowing()) {
            mProgressDialog.show();
        }

        if (deltaX > 0) {
            mDialogIcon.setBackgroundResource(R.drawable.ic_fast_forward);
        } else {
            mDialogIcon.setBackgroundResource(R.drawable.ic_fast_rewind);
        }

        mDialogSeekTime.setText(VideoUtils.formateTime(seekTimePosition));
        mDialogTotalTime.setText(" / " + VideoUtils.formateTime(totalTimeDuration));

        int progress = totalTimeDuration <= 0 ? 0 : (int) (seekTimePosition * 100 / totalTimeDuration);
        mDialogProgressBar.setProgress(progress);
    }

    /**
     * 创建对话框
     *
     * @param localView
     * @return
     */
    protected Dialog createDialogWithView(View localView) {
        Dialog dialog = new Dialog(context, R.style.dialog_transparent);
        dialog.setContentView(localView);
        Window window = dialog.getWindow();
        window.addFlags(Window.FEATURE_ACTION_BAR);
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        window.setLayout(-2, -2);
        WindowManager.LayoutParams localLayoutParams = window.getAttributes();
        localLayoutParams.gravity = Gravity.CENTER;
        window.setAttributes(localLayoutParams);
        return dialog;
    }

    /**
     * 音量进度对话框
     *
     * @param volumePercent
     */
    public void showVolumeDialog(int volumePercent, boolean add) {
        if (mVolumeDialog == null) {
            View localView = LayoutInflater.from(context).inflate(R.layout.dialog_volume, null);
            mDialogVolumeImageView = (ImageView) localView.findViewById(R.id.volume_image_tip);
            mDialogVolumeTextView = (TextView) localView.findViewById(R.id.tv_volume);
            mDialogVolumeProgressBar = (ProgressBar) localView.findViewById(R.id.volume_progressbar);
            mVolumeDialog = createDialogWithView(localView);
        }

        if (!mVolumeDialog.isShowing()) {
            mVolumeDialog.show();
        }

        if (volumePercent >= 100) {
            volumePercent = 100;
            mDialogVolumeImageView.setBackgroundResource(R.drawable.ic_volume_mute);
        } else if (volumePercent <= 0) {
            volumePercent = 0;
            mDialogVolumeImageView.setBackgroundResource(R.drawable.ic_volume_off);
        } else if (add) {
            mDialogVolumeImageView.setBackgroundResource(R.drawable.ic_volume_up);
        } else {
            mDialogVolumeImageView.setBackgroundResource(R.drawable.ic_volume_down);
        }

        mDialogVolumeTextView.setText(volumePercent + "%");
        mDialogVolumeProgressBar.setProgress(volumePercent);
    }


    /**
     * 显示亮度对话框
     *
     * @param brightnessPercent
     */
    public void showBrightnessDialog(int brightnessPercent) {
        if (mBrightnessDialog == null) {
            View localView = LayoutInflater.from(context).inflate(R.layout.dialog_brightness, null);
            mDialogBrightnessTextView = (TextView) localView.findViewById(R.id.tv_brightness);
            mDialogBrightnessProgressBar = (ProgressBar) localView.findViewById(R.id.brightness_progressbar);
            mDialogBrightnessImageView = (ImageView) localView.findViewById(R.id.iv_brightness);
            mBrightnessDialog = createDialogWithView(localView);
        }
        if (!mBrightnessDialog.isShowing()) {
            mBrightnessDialog.show();
        }
        if (brightnessPercent > 100) {
            brightnessPercent = 100;
        } else if (brightnessPercent < 0) {
            brightnessPercent = 0;
        }

        if (brightnessPercent >= 67) {
            mDialogBrightnessImageView.setBackgroundResource(R.drawable.ic_brightness_high);
        } else if (brightnessPercent >= 33) {
            mDialogBrightnessImageView.setBackgroundResource(R.drawable.ic_brightness_medium);
        } else {
            mDialogBrightnessImageView.setBackgroundResource(R.drawable.ic_brightness_low);
        }

        mDialogBrightnessTextView.setText(brightnessPercent + "%");
        mDialogBrightnessProgressBar.setProgress(brightnessPercent);
    }


    /**
     * 取消所有对话框
     */
    public void dismissAllDialog() {

        Timber.i("取消所有对话框");

        //隐藏亮度对话框
        if (mBrightnessDialog != null) {
            mBrightnessDialog.dismiss();
        }

        //隐藏进度对话框
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }

        //隐藏声音对话框
        if (mVolumeDialog != null) {
            mVolumeDialog.dismiss();
        }
    }

}
