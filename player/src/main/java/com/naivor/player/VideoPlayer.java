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

package com.naivor.player;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.net.Uri;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.upstream.Allocator;
import com.google.android.exoplayer2.util.Assertions;
import com.naivor.player.constant.ScreenState;
import com.naivor.player.constant.VideoState;
import com.naivor.player.controll.VideoController;
import com.naivor.player.core.PlayEventListener;
import com.naivor.player.core.PlayerCore;
import com.naivor.player.surface.ControlView;
import com.naivor.player.surface.DialogHolder;
import com.naivor.player.surface.OnControllViewListener;
import com.naivor.player.utils.LogUtils;
import com.naivor.player.utils.SourceUtils;
import com.naivor.player.utils.VideoUtils;

import lombok.Getter;
import lombok.Setter;
import timber.log.Timber;


/**
 * 播放器类
 * <p>
 * Created by tianlai on 17-7-6.
 */

@TargetApi(16)
public class VideoPlayer extends FrameLayout implements OnControllViewListener,
        VideoController, ExoPlayer.EventListener, SimpleExoPlayer.VideoListener, LoadControl {

    public static final int FULL_SCREEN_NORMAL_DELAY = 300;
    public static boolean SAVE_PROGRESS = true;

    @Getter
    protected AspectRatioFrameLayout contentFrame;
    protected ProgressBar bottomProgressBar;
    protected ViewGroup parent;

    //控制界面的控件
    protected ControlView controlView;

    protected DialogHolder dialogHolder;

    protected PlayerCore playerCore;

    protected AudioManager mAudioManager;

    protected int fullscreenOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR;
    protected int normalOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

    protected long lastAutoFullscreenTime = 0;
    protected long lastQuiteFullScreenTime = 0;

    protected
    @VideoState.VideoStateValue
    int videoState = VideoState.CURRENT_STATE_ORIGIN;
    protected
    @ScreenState.ScreenStateValue
    int screenState = ScreenState.SCREEN_LAYOUT_ORIGIN;

    protected String url = "";
    public Object[] objects = null;
    protected int seekToInAdvance = 0;

    //视频拉伸模式
    @Getter
    protected
    @AspectRatioFrameLayout.ResizeMode
    int resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT;
    @Getter
    @Setter
    protected int frameBackground = Color.BLACK;

    //监听音频焦点
    protected AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:  //获得音频焦点，继续播放
                    resume();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:  //失去音频焦点，停止播放
                    stop();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:  //短暂失去音频焦点，暂停播放
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:  //闪避，暂停播放
                    pause();
                    break;
            }
        }
    };

    //监听传感器
    @Getter
    protected SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {//可以得到传感器实时测量出来的变化值
            final float x = event.values[SensorManager.DATA_X];
            float y = event.values[SensorManager.DATA_Y];
            float z = event.values[SensorManager.DATA_Z];
            //过滤掉用力过猛会有一个反向的大数值
            if (((x > -15 && x < -10) || (x < 15 && x > 10)) && Math.abs(y) < 1.5) {
                if ((System.currentTimeMillis() - lastAutoFullscreenTime) > 2000) {
                    autoFullscreen(x);

                    lastAutoFullscreenTime = System.currentTimeMillis();
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    @Getter
    @Setter
    protected PlayEventListener playEventListener;

    public VideoPlayer(@NonNull Context context) {
        this(context, null);
    }

    public VideoPlayer(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        init(context);
    }

    /**
     * 初始化
     *
     * @param context
     */
    protected void init(Context context) {
        LogUtils.init();

        View.inflate(context, getLayoutId(), this);
        //背景
        setBackgroundColor(frameBackground);

        contentFrame = findViewById(R.id.surface_container);
        bottomProgressBar = findViewById(R.id.bottom_progress);
        controlView = findViewById(R.id.cv_controll);

        contentFrame.setResizeMode(resizeMode);

        mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.requestAudioFocus(onAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN); //请求音频焦点

        playerCore = PlayerCore.instance(getContext());
        playerCore.setEventListener(this);
        playerCore.setLoadControl(this);
        playerCore.setVideoListener(this);

        dialogHolder = new DialogHolder(getContext(), this);

        controlView.setPlayer(playerCore.getPlayer());
        controlView.setOnControllViewListener(this);
    }


    @LayoutRes
    public int getLayoutId() {
        return R.layout.video_layout_base;
    }

    /**
     * 设置
     *
     * @param url
     * @param screen
     * @param objects
     */
    @Override
    public boolean setUp(String url, @ScreenState.ScreenStateValue int screen, Object... objects) {

        if (!TextUtils.isEmpty(this.url) && TextUtils.equals(this.url, url)) {
            return false;
        }
        this.url = url;
        this.objects = objects;
        this.screenState = screen;

        setVideoState(VideoState.CURRENT_STATE_ORIGIN);

        if (objects.length != 0) {
            controlView.setVideoTitle(objects[0].toString());
        }

        return true;
    }

    /**
     * 直接全屏播放
     *
     * @param url
     * @param objects
     */
    @Override
    public void startFullscreen(String url, Object... objects) {
        Timber.d("直接全屏播放");

        boolean setUp = setUp(url, ScreenState.SCREEN_WINDOW_FULLSCREEN, objects);

        if (setUp) {
            startWindowFullscreen();

            start();
        }

    }

    /**
     * 准备播放器，初始化播放源
     */
    @Override
    public void prepareSource() {
        if (playerCore != null) {
            Timber.d("准备播放");
            initTextureView();
            addTextureView();
            VideoUtils.getActivity(getContext()).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            playerCore.setMediaSource(SourceUtils.buildMediaSource(getContext(), Uri.parse(url)));

            setVideoState(VideoState.CURRENT_STATE_PREPARING);

            playerCore.prepare();

        }
    }

    /**
     * 初始化 TextureView
     */
    public void initTextureView() {
        Timber.d("初始化 TextureView");

        removeTextureView();

        if (playerCore != null) {
            playerCore.setSurfaceView(new TextureView(getContext()));
        }
    }

    /**
     * 添加 TextureView
     */
    public void addTextureView() {
        Timber.d("添加 TextureView");

        FrameLayout.LayoutParams layoutParams =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        Gravity.CENTER);
        contentFrame.addView(playerCore.getSurfaceView(), layoutParams);
    }

    /**
     * 移除 TextureView
     */
    public void removeTextureView() {
        Timber.d("移除 TextureView");

        View view = playerCore.getSurfaceView();

        if (view != null) {
            ViewParent parent = view.getParent();
            if (parent != null) {
                ((ViewGroup) parent).removeView(view);
            }
        }

    }

    @Override
    public void start() {
        controlView.start();
    }

    @Override
    public void pause() {
        controlView.pause();
    }

    @Override
    public void resume() {
        controlView.resume();
    }

    @Override
    public void previous() {
        controlView.previous();
    }

    @Override
    public void next() {
        controlView.next();
    }

    @Override
    public void stop() {
        controlView.stop();
    }

    @Override
    public void rePlay() {
        controlView.rePlay();
    }

    @Override
    public void seekTo(long millisecond) {
        controlView.seekTo(millisecond);
    }

    @Override
    public void fastward(long millisecond) {
        controlView.fastward(millisecond);
    }

    @Override
    public void backward(long millisecond) {
        controlView.backward(millisecond);
    }

    @Override
    public long getCurrentDuration() {
        return controlView.getCurrentDuration();
    }

    @Override
    public long getTotalDuration() {
        return controlView.getTotalDuration();
    }


    @Override
    public void setOrientation(int windowType, int orientation) {

    }

    @Override
    public boolean isPlaying() {
        return videoState == VideoState.CURRENT_STATE_PLAYING;
    }

    @Override
    public boolean isPause() {
        return videoState == VideoState.CURRENT_STATE_PAUSE;
    }

    @Override
    public boolean isPrepare() {
        return videoState == VideoState.CURRENT_STATE_PREPARING;
    }

    @Override
    public boolean isBuffering() {
        return videoState == VideoState.CURRENT_STATE_PLAYING_BUFFERING;
    }

    /**
     * 改变当前播放状态
     *
     * @param state
     */
    @Override
    public void setVideoState(@VideoState.VideoStateValue int state) {
        Timber.d("改变当前播放状态:%s", VideoState.getVideoStateName(state));

        switch (state) {
            case VideoState.CURRENT_STATE_ORIGIN:

                break;
            case VideoState.CURRENT_STATE_PREPARING:

                break;
            case VideoState.CURRENT_STATE_PLAYING:
            case VideoState.CURRENT_STATE_PAUSE:

                break;
            case VideoState.CURRENT_STATE_PLAYING_BUFFERING:
                onPrepared();
                break;
            case VideoState.CURRENT_STATE_ERROR:
                break;
            case VideoState.CURRENT_STATE_COMPLETE:
                backPress();
                VideoUtils.saveProgress(getContext(), url, 0);
                break;
        }

        videoState = state;

        if (state == VideoState.CURRENT_STATE_PLAYING || state == VideoState.CURRENT_STATE_PAUSE) {
            bottomProgressBar.setVisibility(VISIBLE);
        } else {
            bottomProgressBar.setVisibility(GONE);
        }

        if (playEventListener != null) {
            playEventListener.onVideoState(state);
        }
    }

    /**
     * 改变屏幕状态
     *
     * @param state
     */
    @Override
    public void setScreenState(@ScreenState.ScreenStateValue int state) {

        controlView.setFullBtnState(state);

        switch (state) {
            case ScreenState.SCREEN_LAYOUT_ORIGIN:

                break;
            case ScreenState.SCREEN_WINDOW_FULLSCREEN:

                break;
            case ScreenState.SCREEN_WINDOW_TINY:

                break;
            case ScreenState.SCREEN_LAYOUT_LIST:

                break;
        }

        screenState = state;

        if (playEventListener != null) {
            playEventListener.onScreenState(state);
        }

    }


    /**
     * 准备完成
     */
    @Override
    public void onPrepared() {
        Timber.d("准备播放完成");

        if (videoState != VideoState.CURRENT_STATE_PREPARING) return;

        if (seekToInAdvance != 0) {   //是否有跳过的进度
            playerCore.getPlayer().seekTo(seekToInAdvance);
            seekToInAdvance = 0;
        } else {
            long position = VideoUtils.getSavedProgress(getContext(), url);  //是否有保存的进度

            if (position != 0) {
                playerCore.getPlayer().seekTo(position);
            }
        }

        setVideoState(VideoState.CURRENT_STATE_PLAYING);
    }


    /**
     * 视频显示模式
     *
     * @param resizeMode
     */
    public void setResizeMode(@AspectRatioFrameLayout.ResizeMode int resizeMode) {
        Assertions.checkState(contentFrame != null);
        contentFrame.setResizeMode(resizeMode);
    }

    @Override
    public void onTracksSelected(Renderer[] renderers, TrackGroupArray trackGroupArray, TrackSelectionArray trackSelectionArray) {
        Timber.d("onTracksSelected");
    }

    @Override
    public void onStopped() {
        Timber.d("onStopped");
    }

    @Override
    public void onReleased() {
        Timber.d("onReleased");
    }

    @Override
    public Allocator getAllocator() {
        Timber.d("getAllocator");
        return null;
    }

    @Override
    public boolean shouldStartPlayback(long l, boolean b) {
        Timber.d("shouldStartPlayback");
        return false;
    }

    @Override
    public boolean shouldContinueLoading(long l) {
        Timber.d("shouldContinueLoading");
        return false;
    }

    /**
     * 重力感应的时候调用,自动全屏
     *
     * @param x
     */
    public void autoFullscreen(float x) {
        if (videoState == VideoState.CURRENT_STATE_PLAYING
                && screenState != ScreenState.SCREEN_WINDOW_FULLSCREEN
                && screenState != ScreenState.SCREEN_WINDOW_TINY) {
            if (x > 0) {
                VideoUtils.getActivity(getContext()).setRequestedOrientation(
                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            } else {
                VideoUtils.getActivity(getContext()).setRequestedOrientation(
                        ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
            }
            startWindowFullscreen();
        }
    }

    /**
     * 重力感应的时候调用,自动退出全屏
     */
    public void autoQuitFullscreen() {
        if ((System.currentTimeMillis() - lastAutoFullscreenTime) > 2000
                && videoState == VideoState.CURRENT_STATE_PLAYING
                && screenState == ScreenState.SCREEN_WINDOW_FULLSCREEN) {
            lastAutoFullscreenTime = System.currentTimeMillis();
            backPress();
        }
    }


    /**
     * 返回按钮按下
     *
     * @return
     */
    public boolean backPress() {
        Timber.i("返回按钮按下");

        if ((System.currentTimeMillis() - lastQuiteFullScreenTime) < FULL_SCREEN_NORMAL_DELAY) {
            return false;
        }

        if (backOriginWindow()) {
            lastQuiteFullScreenTime = System.currentTimeMillis();
            return true;
        }

        VideoUtils.saveProgress(getContext(), url, getCurrentDuration());

        return false;
    }


    /**
     * 全屏播放
     */
    @Override
    public void startWindowFullscreen() {
        Timber.i("全屏播放");

        if (screenState != ScreenState.SCREEN_WINDOW_FULLSCREEN) {

            parent = (ViewGroup) getParent();

            if (parent != null) {
                parent.removeView(this);   //从当前父布局移除

                pause();

                ViewGroup vp = (ViewGroup) (VideoUtils.getActivity(getContext()))  //加入contentView
                        .findViewById(Window.ID_ANDROID_CONTENT);
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

                vp.addView(this, lp);

                VideoUtils.getActivity(getContext()).setRequestedOrientation(fullscreenOrientation);
                VideoUtils.showSupportActionBar(getContext(), false);

                setScreenState(ScreenState.SCREEN_WINDOW_FULLSCREEN);

                resume();

                bottomProgressBar.setVisibility(GONE);
            }
        }

    }


    /**
     * 小窗播放
     */
    @Override
    public void startWindowTiny() {
        Timber.i("小窗播放");

        if (screenState != ScreenState.SCREEN_WINDOW_TINY) {

            if (videoState == VideoState.CURRENT_STATE_ORIGIN || videoState == VideoState.CURRENT_STATE_ERROR) {
                return;
            }

            parent = (ViewGroup) getParent();

            if (parent != null) {
                parent.removeView(this);   //从当前父布局移除

                ViewGroup vp = (ViewGroup) (VideoUtils.getActivity(getContext()))
                        .findViewById(Window.ID_ANDROID_CONTENT);

                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(400, 400);
                lp.gravity = Gravity.RIGHT | Gravity.BOTTOM;
                vp.addView(this, lp);


                setScreenState(ScreenState.SCREEN_WINDOW_TINY);
            }
        }

    }

    /**
     * 退出全屏和小窗
     */
    @Override
    public boolean backOriginWindow() {
        Timber.i("退出全屏和小窗");

        if (screenState == ScreenState.SCREEN_WINDOW_FULLSCREEN || screenState == ScreenState.SCREEN_WINDOW_TINY) {

            ViewGroup currentVP = (ViewGroup) getParent();

            if (parent != null && currentVP != null && parent != currentVP) {
                currentVP.removeView(this);   //从当前父布局移除

                pause();

                parent.addView(this);

                VideoUtils.getActivity(getContext()).setRequestedOrientation(normalOrientation);
                VideoUtils.showSupportActionBar(getContext(), true);

                setScreenState(ScreenState.SCREEN_LAYOUT_ORIGIN);
                onTouchScreenEnd();

                resume();

                bottomProgressBar.setVisibility(GONE);

                return true;
            }
        }

        return false;
    }


    @Override
    public void onTimelineChanged(Timeline timeline, Object o) {
        Timber.d("onTimelineChanged");
    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroupArray, TrackSelectionArray trackSelectionArray) {
        Timber.d("onTracksChanged");
    }

    @Override
    public void onLoadingChanged(boolean b) {
        Timber.d("onLoadingChanged:%s", b);
    }

    @Override
    public void onPlayerStateChanged(boolean b, int i) {
        Timber.d("onPlayerStateChanged:%s,%s", b, i);

        switch (i) {
            case ExoPlayer.STATE_IDLE:
//                setVideoState(VideoState.CURRENT_STATE_ERROR);
                break;
            case ExoPlayer.STATE_BUFFERING:
                setVideoState(VideoState.CURRENT_STATE_PLAYING_BUFFERING);
                break;
            case ExoPlayer.STATE_READY:
                if (b) {
                    setVideoState(VideoState.CURRENT_STATE_PLAYING);
                } else {
                    setVideoState(VideoState.CURRENT_STATE_PAUSE);
                }
                break;
            case ExoPlayer.STATE_ENDED:
                setVideoState(VideoState.CURRENT_STATE_COMPLETE);
                break;
        }
    }

    @Override
    public void onPlayerError(ExoPlaybackException e) {
        Timber.d("onPlayerError");

        setVideoState(VideoState.CURRENT_STATE_ERROR);
    }

    @Override
    public void onPositionDiscontinuity() {
        Timber.d("onPositionDiscontinuity");
    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
        Timber.d("onPlaybackParametersChanged");
    }

    @Override
    public void onclick(View view) {
        if (playEventListener != null) {
            playEventListener.onControllViewClick(view);
        }

        if (view.getId() == R.id.iv_back) {
            if (!backPress()) {
                VideoUtils.getActivity(getContext()).onBackPressed();
            }
        }
    }

    @Override
    public void onFullScreenClick() {
        if (screenState == ScreenState.SCREEN_LAYOUT_ORIGIN) {
            startWindowFullscreen();
        } else if (screenState == ScreenState.SCREEN_WINDOW_FULLSCREEN) {
            backPress();
        }

    }

    @Override
    public void onVisibilityChange(int visibility) {
        boolean shown = visibility == VISIBLE;

        if (playEventListener != null) {
            playEventListener.onControllViewShown(shown);
        }

        if (shown) {
            bottomProgressBar.setVisibility(GONE);
        } else {
            if (videoState == VideoState.CURRENT_STATE_PLAYING) {
                bottomProgressBar.setVisibility(VISIBLE);
            }
        }
    }

    @Override
    public void onProgress(int progress, int bufferedProgress) {
        bottomProgressBar.setProgress(progress);
        bottomProgressBar.setSecondaryProgress(bufferedProgress);
    }

    @Override
    public void changeVolume(float offset, int volumeStep) {
        Timber.d("changeVolume:%s,%s", offset, volumeStep);

        int volumePercent = VideoUtils.caculateVolume(mAudioManager, offset, volumeStep);

        dialogHolder.showVolumeDialog(volumePercent, offset < 0);
    }


    @Override
    public void changeBrightness(float offset, float brightnessStep) {
        Timber.d("changeBrightness:%s,%s", offset, brightnessStep);

        int brightnessPercent = VideoUtils.caculateBrightness(getContext(), offset, brightnessStep);

        dialogHolder.showBrightnessDialog(brightnessPercent);
    }


    @Override
    public void changePlayingPosition(float offset, int seekStep) {
        Timber.d("changePlayingPosition:%s,%s", offset, seekStep);

        long position = VideoUtils.caculatePlayPosition(offset, seekStep, getCurrentDuration(), getTotalDuration());

        seekTo(position);

        dialogHolder.showProgressDialog(offset, position, getTotalDuration());
    }


    @Override
    public int getScreenState() {
        return screenState;
    }

    @Override
    public int getCurrentState() {
        return videoState;
    }

    @Override
    public void onTouchScreenEnd() {
        Timber.d("onTouchScreenEnd");

        dialogHolder.dismissAllDialog();
    }

    @Override
    public void prepareSourceData() {
        if (!VideoUtils.isWifi(getContext()) && !dialogHolder.isPlayWithNotWifi()) {
            dialogHolder.showNotWifiDialog();
        } else {
            prepareSource();
        }
    }


    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees,
                                   float pixelWidthHeightRatio) {
        if (contentFrame != null) {
            float aspectRatio = height == 0 ? 1 : (width * pixelWidthHeightRatio) / height;
            contentFrame.setAspectRatio(aspectRatio);
        }
    }

    @Override
    public void onRenderedFirstFrame() {

    }


    /**
     * 释放资源,调用该方法后播放器不能再被使用
     */
    public void release() {
        mAudioManager.abandonAudioFocus(onAudioFocusChangeListener);
        playerCore.release();
        dialogHolder = null;
        mAudioManager = null;
        url = null;
        onAudioFocusChangeListener = null;
        objects = null;
        sensorEventListener = null;
        playEventListener = null;
    }

}
