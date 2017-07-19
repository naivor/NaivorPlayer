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
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
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
import android.widget.ImageView;
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
import com.naivor.player.surface.VideoPreview;
import com.naivor.player.utils.SourceUtils;
import com.naivor.player.utils.Utils;
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
    //小窗的宽高 16:9
    public static final int WIDTH_TINY_WINDOW = 224;
    public static final int HEIGHT_TINY_WINDOW = 126;

    @Getter
    protected AspectRatioFrameLayout contentFrame;
    protected ProgressBar bottomProgressBar;

    //用于全屏，小窗记录原来父控件
    protected ViewGroup parent;
    protected ViewGroup.LayoutParams parentLayoutParams;
    protected int indexInParent;

    //控制界面的控件
    protected ControlView controlView;
    //视频预览
    protected VideoPreview videoPreview;

    protected DialogHolder dialogHolder;

    protected PlayerCore playerCore;

    protected AudioManager mAudioManager;

    protected int fullscreenOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR;
    protected int normalOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

    protected long lastAutoFullscreenTime = 0;
    protected long lastQuiteFullScreenTime = 0;

    @Getter
    @VideoState.VideoStateValue
    protected int videoState = VideoState.CURRENT_STATE_ORIGIN;

    @ScreenState.ScreenStateValue
    protected int screenState = ScreenState.SCREEN_LAYOUT_ORIGIN;

    protected String url = "";
    protected Object[] objects = null;
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
                default:
                    break;
            }
        }
    };

    //监听传感器
    @Getter
    protected SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) { //可以得到传感器实时测量出来的变化值
            if (event != null) {
                final float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];
                //过滤掉用力过猛会有一个反向的大数值
                if (((x > -15 && x < -10) || (x < 15 && x > 10)) && Math.abs(y) < 1.5) {
                    if ((System.currentTimeMillis() - lastAutoFullscreenTime) > 2000) {
                        autoFullscreen(x);

                        lastAutoFullscreenTime = System.currentTimeMillis();
                    }
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

        View.inflate(context, getLayoutId(), this);
        //背景
        setBackgroundColor(frameBackground);

        if (!isInEditMode()) {
            Utils.init(context);

            contentFrame = findViewById(R.id.surface_container);
            bottomProgressBar = findViewById(R.id.bottom_progress);
            controlView = findViewById(R.id.cv_controll);

            contentFrame.setResizeMode(resizeMode);

            mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
            mAudioManager.requestAudioFocus(onAudioFocusChangeListener, AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN); //请求音频焦点

            playerCore = PlayerCore.instance(getContext());
            playerCore.setEventListener(this);
            playerCore.setLoadControl(this);
            playerCore.setVideoListener(this);

            dialogHolder = new DialogHolder(getContext(), this);

            controlView.setPlayer(playerCore.getPlayer());
            controlView.setOnControllViewListener(this);

            videoPreview = new VideoPreview(contentFrame, (ImageView) findViewById(R.id.iv_artwork),
                    playerCore.getPlayer());
        }
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

        VideoUtils.saveLastUrl(url);

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
            ViewParent viewParent = view.getParent();
            if (viewParent != null) {
                ((ViewGroup) viewParent).removeView(view);
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
                VideoUtils.clearSavedAutoPause(url);
                break;
            case VideoState.CURRENT_STATE_PAUSE:

                break;
            case VideoState.CURRENT_STATE_PLAYING_BUFFERING:
                onPrepared();
                break;
            case VideoState.CURRENT_STATE_ERROR:
                break;
            case VideoState.CURRENT_STATE_COMPLETE:
                backPress();
                VideoUtils.clearSavedProgress(url);
                break;
            default:
                break;
        }

        videoState = state;

        updateBottomProgress();

        if (playEventListener != null) {
            playEventListener.onVideoState(state);
        }
    }

    /**
     * 显示底部播放进度
     */
    protected void updateBottomProgress() {
        if (videoState == VideoState.CURRENT_STATE_PLAYING && !controlView.isShown()) {
            bottomProgressBar.setVisibility(VISIBLE);
        } else {
            bottomProgressBar.setVisibility(GONE);
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
            default:
                break;
        }

        screenState = state;

        updateBottomProgress();

        if (playEventListener != null) {
            playEventListener.onScreenState(state);
        }

    }


    /**
     * 准备完成
     */
    @Override
    public void onPrepared() {
        Timber.d("准备播放成功，可以开始播放");

        if (videoState != VideoState.CURRENT_STATE_PREPARING) {
            return;
        }
        Timber.d("计算播放位置");

        if (seekToInAdvance != 0) {   //是否有跳过的进度

            Timber.d("跳过时长：%s", seekToInAdvance);

            playerCore.getPlayer().seekTo(seekToInAdvance);
            seekToInAdvance = 0;
        } else {
            long position = VideoUtils.getSavedProgress(url);  //是否有保存的进度

            if (position != 0) {

                Timber.d("上次保存的进度：%s", position);

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

        VideoUtils.saveProgress(url, getCurrentDuration());

        return false;
    }


    /**
     * 全屏播放
     */
    @Override
    public void startWindowFullscreen() {
        Timber.i("全屏播放");

        if (screenState != ScreenState.SCREEN_WINDOW_FULLSCREEN) {

            ViewParent viewParent = getParent();

            if (parent == null && viewParent != null) {

                ViewGroup vp = (ViewGroup) (VideoUtils.getActivity(getContext()))  //加入contentView
                        .findViewById(Window.ID_ANDROID_CONTENT);

                if (viewParent != vp) {

                    parent = (ViewGroup) viewParent;
                    indexInParent = parent.indexOfChild(this);
                    parentLayoutParams = getLayoutParams();
                    parent.removeView(this);   //从当前父布局移除

                    pause();

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

            ViewParent viewParent = getParent();

            if (parent == null && viewParent != null) {

                ViewGroup vp = (ViewGroup) (VideoUtils.getActivity(getContext()))
                        .findViewById(Window.ID_ANDROID_CONTENT);

                if (viewParent != vp) {
                    parent = (ViewGroup) viewParent;
                    indexInParent = parent.indexOfChild(this);
                    parentLayoutParams = getLayoutParams();
                    parent.removeView(this);   //从当前父布局移除

                    pause();

                    int widthTinyWindow = VideoUtils.dp2px(WIDTH_TINY_WINDOW);
                    int heightTinyWindow = VideoUtils.dp2px(HEIGHT_TINY_WINDOW);
                    FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(widthTinyWindow, heightTinyWindow);
                    lp.gravity = Gravity.RIGHT | Gravity.BOTTOM;
                    lp.bottomMargin = VideoUtils.dp2px(20);
                    lp.rightMargin = VideoUtils.dp2px(5);
                    vp.addView(this, lp);


                    setScreenState(ScreenState.SCREEN_WINDOW_TINY);

                    resume();
                }
            }
        }

    }

    /**
     * 退出全屏和小窗
     */
    @Override
    public boolean backOriginWindow() {
        return backOriginWindow(true);
    }

    /**
     * 退出全屏和小窗
     */
    public boolean backOriginWindow(boolean continuePlay) {
        Timber.i("退出全屏和小窗");

        if (screenState == ScreenState.SCREEN_WINDOW_FULLSCREEN || screenState == ScreenState.SCREEN_WINDOW_TINY) {

            ViewGroup currentVP = (ViewGroup) getParent();

            if (parent != null && currentVP != null && parent != currentVP) {
                currentVP.removeView(this);   //从当前父布局移除

                pause();

                parent.addView(this, indexInParent, parentLayoutParams);

                VideoUtils.getActivity(getContext()).setRequestedOrientation(normalOrientation);
                VideoUtils.showSupportActionBar(getContext(), true);

                setScreenState(ScreenState.SCREEN_LAYOUT_ORIGIN);
                onTouchScreenEnd();

                if (continuePlay) {
                    resume();
                }

                bottomProgressBar.setVisibility(GONE);

                parent = null;
                indexInParent = 0;
                parentLayoutParams = null;

                return true;
            }
        }

        return false;
    }


    @Override
    public void onTimelineChanged(Timeline timeline, Object o) {
        Timber.d("onTimelineChanged");

        videoPreview.updatePreview();
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
            default:
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

        int id = view.getId();
        if (id == R.id.iv_back) {
            if (!backPress()) {
                VideoUtils.getActivity(getContext()).onBackPressed();
            }
        } else if (id == R.id.iv_tiny_exit) {
            backOriginWindow();
        } else if (id == R.id.iv_tiny_close) {
            backOriginWindow(false);
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

        updateBottomProgress();
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
    public void requestPrepareSourceData() {
        if (!VideoUtils.isWifi() && !dialogHolder.isPlayWithNotWifi()) {
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
     * activity的onResume生命周期调用，用于继续播放
     */
    public void onResume() {
        if (TextUtils.isEmpty(url)) {
            url = VideoUtils.getLastUrl();
        }

        boolean isAutoPause = VideoUtils.isAutoPause(url);

        Timber.d("onResume，是否继续：%s", isAutoPause);

        if (isAutoPause) {

            resume();

            if (videoState == VideoState.CURRENT_STATE_ORIGIN) { //播放源被重置
                prepareSource();
            }
        }
    }

    /**
     * activity的onResume生命周期调用，用于保存进度
     */
    public void onPause() {

        boolean playing = isPlaying();

        Timber.d("onResume，是否保存：%s", playing);

        if (playing) {
            VideoUtils.saveAutoPause(url);
            pause();

            VideoUtils.saveProgress(url, getCurrentDuration());

        }
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
        videoPreview = null;
    }


    /**
     * 获取显示预览的控件
     *
     * @return
     */
    public ImageView getPreviewView() {
        if (videoPreview != null) {
            return videoPreview.getPreview();
        }
        return null;
    }

    /**
     * 设置预览图片
     *
     * @param bitmap
     */
    public void setPreviewImage(@lombok.NonNull Bitmap bitmap) {
        if (videoPreview != null) {
            videoPreview.setDefaultPreview(bitmap);

            ImageView imageView = videoPreview.getPreview();
            if (imageView != null) {
                imageView.setImageBitmap(bitmap);
            }
        }
    }

}
