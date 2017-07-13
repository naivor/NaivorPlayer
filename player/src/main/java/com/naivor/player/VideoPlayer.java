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
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

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
import com.naivor.player.core.PlayerCore;
import com.naivor.player.surface.ControlView;
import com.naivor.player.surface.OnControllViewListener;
import com.naivor.player.utils.LogUtils;
import com.naivor.player.utils.SourceUtils;
import com.naivor.player.utils.VideoUtils;

import java.util.Map;

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

    @Getter
    private AspectRatioFrameLayout contentFrame;
    //控制界面的控件
    private ControlView controlView;

    // 亮度对话框
    protected Dialog mBrightnessDialog;
    protected ProgressBar mDialogBrightnessProgressBar;
    protected TextView mDialogBrightnessTextView;

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


    private PlayerCore playerCore;

    public static boolean ACTION_BAR_EXIST = true;
    public static boolean TOOL_BAR_EXIST = true;
    public static int FULLSCREEN_ORIENTATION = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
    public static int NORMAL_ORIENTATION = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
    public static boolean SAVE_PROGRESS = true;

    public boolean isPlayWithNotWifi = false;  //非wifi环境是否播放

    public static final int FULLSCREEN_ID = 33797;
    public static final int TINY_ID = 33798;

    public static final int FULL_SCREEN_NORMAL_DELAY = 300;
    public static long CLICK_QUIT_FULLSCREEN_TIME = 0;

    public static final int SCREEN_LAYOUT_NORMAL = 0;
    public static final int SCREEN_LAYOUT_LIST = 1;

    public static final int SCREEN_WINDOW_FULLSCREEN = 2;
    public static final int SCREEN_WINDOW_TINY = 3;


    public static int BACKUP_PLAYING_BUFFERING_STATE = -1;

    public
    @VideoState.VideoStateValue
    int videoState = VideoState.CURRENT_STATE_ORIGIN;
    public
    @ScreenState.ScreenStateValue
    int screenState = ScreenState.SCREEN_LAYOUT_ORIGIN;

    public Map<String, String> headData;

    public String url = "";
    public Object[] objects = null;
    public int seekToInAdvance = 0;

    protected AudioManager mAudioManager;

    //视频拉伸模式
    @Getter
    protected @AspectRatioFrameLayout.ResizeMode
    int resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT;
    @Getter
    @Setter
    protected int frameBackground = Color.BLACK;

    //监听音频焦点
    private AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
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
    private SensorEventListener sensorEventListener = new SensorEventListener() {
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
    private void init(Context context) {
        LogUtils.init();

        View.inflate(context, getLayoutId(), this);

        //背景
        setBackgroundColor(frameBackground);

        contentFrame = findViewById(R.id.surface_container);
        contentFrame.setResizeMode(resizeMode);
        controlView = findViewById(R.id.cv_controll);

        controlView.setOnControllViewListener(this);

        mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.requestAudioFocus(onAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN); //请求音频焦点

        playerCore = PlayerCore.instance(getContext());
        playerCore.setEventListener(this);
        playerCore.setLoadControl(this);
        playerCore.setVideoListener(this);

        controlView.setPlayer(playerCore.getPlayer());
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
    public boolean setUp(String url, int screen, Object... objects) {

        if (!TextUtils.isEmpty(this.url) && TextUtils.equals(this.url, url)) {
            return false;
        }
        this.url = url;
        this.objects = objects;
        this.screenState = screen;
        this.headData = null;

        setVideoState(VideoState.CURRENT_STATE_ORIGIN);

        return true;
    }

    /**
     * 直接全屏播放
     *
     * @param url
     * @param objects
     */
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
    public void prepareMediaPlayer() {
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
    public void setVideoState(@VideoState.VideoStateValue int state) {

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
    }

    /**
     * 改变屏幕状态
     *
     * @param state
     */
    public void setScreenState(@ScreenState.ScreenStateValue int state) {
        switch (state) {
            case SCREEN_LAYOUT_NORMAL:

                break;
            case SCREEN_WINDOW_FULLSCREEN:

                break;
            case SCREEN_WINDOW_TINY:

                break;
            case SCREEN_LAYOUT_LIST:

                break;
        }

        screenState = state;
    }


    /**
     * 准备完成
     */
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

    public void clearFullscreenLayout() {
        ViewGroup vp = (ViewGroup) (VideoUtils.getActivity(getContext()))//.getWindow().getDecorView();
                .findViewById(Window.ID_ANDROID_CONTENT);
        View oldF = vp.findViewById(FULLSCREEN_ID);
        View oldT = vp.findViewById(TINY_ID);
        if (oldF != null) {
            vp.removeView(oldF);
        }
        if (oldT != null) {
            vp.removeView(oldT);
        }
        showSupportActionBar(getContext(), true);
    }


    public static long lastAutoFullscreenTime = 0;

    /**
     * 重力感应的时候调用,自动全屏
     *
     * @param x
     */
    public void autoFullscreen(float x) {
        if (videoState == VideoState.CURRENT_STATE_PLAYING
                && screenState != SCREEN_WINDOW_FULLSCREEN
                && screenState != SCREEN_WINDOW_TINY) {
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
                && screenState == SCREEN_WINDOW_FULLSCREEN) {
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

        if ((System.currentTimeMillis() - CLICK_QUIT_FULLSCREEN_TIME) < FULL_SCREEN_NORMAL_DELAY) {
            return false;
        }

        if (screenState == ScreenState.SCREEN_WINDOW_FULLSCREEN ||
                screenState == ScreenState.SCREEN_WINDOW_TINY) {

            backOriginWindow();

            return true;
        }

        VideoUtils.saveProgress(getContext(), url, getCurrentDuration());

        return false;
    }

    private ViewGroup parent;

    /**
     * 全屏播放
     */
    public void startWindowFullscreen() {
        Timber.i("全屏播放");

        showSupportActionBar(getContext(), false);

        VideoUtils.getActivity(getContext()).setRequestedOrientation(FULLSCREEN_ORIENTATION);

        parent = (ViewGroup) getParent();

        if (parent != null) {
            parent.removeView(this);   //从当前父布局移除

            ViewGroup vp = (ViewGroup) (VideoUtils.getActivity(getContext()))  //加入contentView
                    .findViewById(Window.ID_ANDROID_CONTENT);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            vp.addView(this, lp);
        }

        setScreenState(ScreenState.SCREEN_WINDOW_FULLSCREEN);
        controlView.setFullBtnState(true);
    }


    /**
     * 小窗播放
     */
    public void startWindowTiny() {
        Timber.i("小窗播放");

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
        }


        setScreenState(ScreenState.SCREEN_WINDOW_TINY);
    }

    /**
     * 退出全屏和小窗
     */
    public void backOriginWindow() {
        Timber.i("退出全屏和小窗");

        VideoUtils.getActivity(getContext()).setRequestedOrientation(NORMAL_ORIENTATION);
        showSupportActionBar(getContext(), true);

        ViewGroup currentVP = (ViewGroup) getParent();

        if (parent != null && currentVP != null && parent != currentVP) {
            currentVP.removeView(this);   //从当前父布局移除

            parent.addView(this);
        }

        controlView.setFullBtnState(true);
        setScreenState(ScreenState.SCREEN_LAYOUT_ORIGIN);
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
        Timber.d("onVisibilityChange:%s", visibility);
    }

    @Override
    public void onProgress(int progress, int bufferedProgress) {

    }

    @Override
    public void changeVolume(float offset, float total) {
        Timber.d("changeVolume:%s,%s", offset, total);

        int volumePercent = caculateVolume(offset, total);

        showVolumeDialog(volumePercent);
    }

    /**
     * 计算声音百分比
     *
     * @param offset
     * @param total
     * @return
     */
    protected int caculateVolume(float offset, float total) {
        int mGestureDownVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int max = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int deltaV = (int) (max * offset * 3 / total);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mGestureDownVolume + deltaV, 0);
        //百分比
        return (int) (mGestureDownVolume * 100 / max + offset * 3 * 100 / total);
    }

    @Override
    public void changeBrightness(float offset, float total) {
        Timber.d("changeBrightness:%s,%s", offset, total);

        int brightnessPercent = caculateBrightness(offset, total);

        showBrightnessDialog(brightnessPercent);
    }

    /**
     * 计算亮度百分百
     *
     * @param offset
     * @param total
     * @return
     */
    protected int caculateBrightness(float offset, float total) {
        float mGestureDownBrightness = 0;

        WindowManager.LayoutParams lp = VideoUtils.getActivity(getContext()).getWindow().getAttributes();
        if (lp.screenBrightness < 0) {
            try {
                mGestureDownBrightness = Settings.System.getInt(getContext().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
                Timber.i("当前系统亮度：%s", mGestureDownBrightness);
            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            mGestureDownBrightness = lp.screenBrightness * 255;
            Timber.i("当前页面亮度: ", mGestureDownBrightness);
        }

        int deltaV = (int) (255 * offset * 3 / total);
        WindowManager.LayoutParams params = VideoUtils.getActivity(getContext()).getWindow().getAttributes();
        if (((mGestureDownBrightness + deltaV) / 255) >= 1) {//这和声音有区别，必须自己过滤一下负值
            params.screenBrightness = 1;
        } else if (((mGestureDownBrightness + deltaV) / 255) <= 0) {
            params.screenBrightness = 0.01f;
        } else {
            params.screenBrightness = (mGestureDownBrightness + deltaV) / 255;
        }
        VideoUtils.getActivity(getContext()).getWindow().setAttributes(params);
        //亮度百分比
        return (int) (mGestureDownBrightness * 100 / 255 + offset * 3 * 100 / total);
    }

    @Override
    public void changePlayingPosition(float offset, float total) {
        Timber.d("changePlayingPosition:%s,%s", offset, total);

        long position = caculatePlayPosition(offset, total);

        showProgressDialog(offset, position, getTotalDuration());
    }

    /**
     * 计算播放位置
     *
     * @param offset
     * @param total
     * @return
     */
    protected long caculatePlayPosition(float offset, float total) {
        long mSeekTimePosition;
        long totalTimeDuration = getTotalDuration();
        mSeekTimePosition = (int) (getCurrentDuration() + offset * totalTimeDuration / total);
        if (mSeekTimePosition > totalTimeDuration) {
            mSeekTimePosition = totalTimeDuration;
        }

        return mSeekTimePosition;
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

    @Override
    public void prepareSourceData() {
        if (!VideoUtils.isWifi(getContext()) && !isPlayWithNotWifi) {
            showNotWifiDialog();
        } else {
            prepareMediaPlayer();
        }
    }

    /**
     * 显示或隐藏 ActionBar
     *
     * @param context
     * @param show
     */
    public static void showSupportActionBar(Context context, boolean show) {

        Timber.d("显示标题栏：%s", show);

            ActionBar ab = VideoUtils.getActivity(context).getSupportActionBar();

            if (ab != null) {

                Timber.i("ActionBar 存在，%s", ab.getClass().getCanonicalName());

                if (show) {
                    ab.show();
                } else {
                    ab.hide();
                }

                if (TOOL_BAR_EXIST) {
                    if (show) {
                        VideoUtils.getActivity(context).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                    } else {
                        VideoUtils.getActivity(context).getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                                WindowManager.LayoutParams.FLAG_FULLSCREEN);
                    }
                }

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
     * 网络非wifi提示
     */
    public void showNotWifiDialog() {
        new AlertDialog.Builder(getContext())
                .setMessage(getResources().getString(R.string.tips_not_wifi))
                .setPositiveButton(getResources().getString(R.string.tips_not_wifi_confirm), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        prepareMediaPlayer();
                        isPlayWithNotWifi = true;
                    }
                })
                .setNegativeButton(getResources().getString(R.string.tips_not_wifi_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (screenState == SCREEN_WINDOW_FULLSCREEN) {
                            dialog.dismiss();
                            clearFullscreenLayout();
                        }
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        dialog.dismiss();
                        if (screenState == SCREEN_WINDOW_FULLSCREEN) {
                            dialog.dismiss();
                            clearFullscreenLayout();
                        }
                    }
                })
                .create().show();
    }

    /**
     * 显示拖动进度对话框
     */
    public void showProgressDialog(float deltaX, long seekTimePosition, long totalTimeDuration) {

        if (mProgressDialog == null) {
            View localView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_progress, null);
            mDialogProgressBar = ((ProgressBar) localView.findViewById(R.id.duration_progressbar));
            mDialogSeekTime = ((TextView) localView.findViewById(R.id.tv_current));
            mDialogTotalTime = ((TextView) localView.findViewById(R.id.tv_duration));
            mDialogIcon = ((ImageView) localView.findViewById(R.id.duration_image_tip));
            mProgressDialog = createDialogWithView(localView);
        }
        if (!mProgressDialog.isShowing()) {
            mProgressDialog.show();
        }


        mDialogSeekTime.setText(VideoUtils.formateTime(seekTimePosition));
        mDialogTotalTime.setText(" / " + VideoUtils.formateTime(totalTimeDuration));

        int progress = totalTimeDuration <= 0 ? 0 : (int) (seekTimePosition * 100 / totalTimeDuration);

        mDialogProgressBar.setProgress(progress);
        if (deltaX > 0) {
            mDialogIcon.setBackgroundResource(R.drawable.jc_forward_icon);
        } else {
            mDialogIcon.setBackgroundResource(R.drawable.jc_backward_icon);
        }
    }

    /**
     * 创建对话框
     *
     * @param localView
     * @return
     */
    public Dialog createDialogWithView(View localView) {
        Dialog dialog = new Dialog(getContext(), R.style.dialog_transparent);
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
    public void showVolumeDialog(int volumePercent) {
        if (mVolumeDialog == null) {
            View localView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_volume, null);
            mDialogVolumeImageView = ((ImageView) localView.findViewById(R.id.volume_image_tip));
            mDialogVolumeTextView = ((TextView) localView.findViewById(R.id.tv_volume));
            mDialogVolumeProgressBar = ((ProgressBar) localView.findViewById(R.id.volume_progressbar));
            mVolumeDialog = createDialogWithView(localView);
        }
        if (!mVolumeDialog.isShowing()) {
            mVolumeDialog.show();
        }
        if (volumePercent <= 0) {
            mDialogVolumeImageView.setBackgroundResource(R.drawable.jc_close_volume);
        } else {
            mDialogVolumeImageView.setBackgroundResource(R.drawable.jc_add_volume);
        }
        if (volumePercent > 100) {
            volumePercent = 100;
        } else if (volumePercent < 0) {
            volumePercent = 0;
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
            View localView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_brightness, null);
            mDialogBrightnessTextView = ((TextView) localView.findViewById(R.id.tv_brightness));
            mDialogBrightnessProgressBar = ((ProgressBar) localView.findViewById(R.id.brightness_progressbar));
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
        mDialogBrightnessTextView.setText(brightnessPercent + "%");
        mDialogBrightnessProgressBar.setProgress(brightnessPercent);
    }


    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        release();
    }

    /**
     * 释放资源
     */
    public void release() {
        mAudioManager.abandonAudioFocus(onAudioFocusChangeListener);
    }

}
