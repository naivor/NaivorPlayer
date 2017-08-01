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
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
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
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.util.Assertions;
import com.naivor.player.constant.OrientationState;
import com.naivor.player.constant.ScreenState;
import com.naivor.player.constant.VideoState;
import com.naivor.player.controll.VideoController;
import com.naivor.player.core.OnListVideoPlayListener;
import com.naivor.player.core.PlayerCore;
import com.naivor.player.surface.ControlView;
import com.naivor.player.surface.DialogHolder;
import com.naivor.player.surface.OnControllViewListener;
import com.naivor.player.surface.VideoPreview;
import com.naivor.player.utils.SourceUtils;
import com.naivor.player.utils.Utils;
import com.naivor.player.utils.VideoUtils;

import java.lang.ref.WeakReference;

import lombok.Getter;
import lombok.Setter;
import timber.log.Timber;

import static com.naivor.player.constant.OrientationState.ORIENTATION_TYPE_LANDSCAPE;
import static com.naivor.player.constant.OrientationState.ORIENTATION_TYPE_PORTRAIT;
import static com.naivor.player.constant.OrientationState.ORIENTATION_TYPE_SENSOR;


/**
 * 播放器类
 * <p>
 * Created by tianlai on 17-7-6.
 */

@TargetApi(16)
public class VideoPlayer extends FrameLayout implements OnControllViewListener, OnListVideoPlayListener,
        VideoController, ExoPlayer.EventListener, SimpleExoPlayer.VideoListener {

    public static final String TAG = VideoPlayer.class.getSimpleName();

    public static final int FULL_SCREEN_NORMAL_DELAY = 300;
    //小窗的宽高 16:9
    public static final int WIDTH_TINY_WINDOW = 224;
    public static final int HEIGHT_TINY_WINDOW = 126;

    protected Context context;

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

    protected AudioManager mAudioManager;

    protected int fullscreenOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR;
    protected int normalOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

    protected long lastAutoFullscreenTime = 0;
    protected long lastQuiteFullScreenTime = 0;

    @Getter
    @VideoState.VideoStateValue
    protected int videoState = VideoState.CURRENT_STATE_ORIGIN;
    @Getter
    @ScreenState.ScreenStateValue
    protected int screenState = ScreenState.SCREEN_LAYOUT_ORIGIN;
    @ScreenState.ScreenStateValue
    protected int originScreenState = ScreenState.SCREEN_LAYOUT_ORIGIN;

    @Getter
    protected String url = "";
    @Getter
    protected String videoName = null;
    protected int seekToInAdvance = 0;

    //是否开启自动缓冲，即设置播放url的时候就开始缓冲（在list中不起作用）
    @Getter
    @Setter
    protected boolean autoPrepare = false;

    //视频拉伸模式
    @Getter
    protected
    @AspectRatioFrameLayout.ResizeMode
    int resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT;
    @Getter
    @Setter
    protected int frameBackground = Color.BLACK;

    @Getter
    @Setter
    protected PlayEventListener playEventListener;

    //小窗的宽高
    @Getter
    @Setter
    protected static int tinyWidth;
    @Getter
    @Setter
    protected static int tinyHeight;


    //监听音频焦点
    protected AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            if (isVideoInPlayState()) {
                switch (focusChange) {
                    case AudioManager.AUDIOFOCUS_GAIN:  //获得音频焦点，继续播放
                        resume();
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS:  //失去音频焦点，停止播放
                        stopAndReset();
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:  //短暂失去音频焦点，暂停播放
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:  //闪避，暂停播放
                        pause();
                        break;
                    default:
                        break;
                }
            }
        }
    };


    public VideoPlayer(@NonNull Context context) {
        this(context, null);
    }

    public VideoPlayer(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VideoPlayer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context);
    }

    /**
     * 初始化
     *
     * @param context
     */
    protected void init(@lombok.NonNull Context context) {
        this.context = context.getApplicationContext();

        //背景
        setBackgroundColor(frameBackground);

        if (!isInEditMode()) {
            Utils.init(context);

            setMinimumWidth(VideoUtils.dp2px(WIDTH_TINY_WINDOW));
            setMinimumHeight(VideoUtils.dp2px(HEIGHT_TINY_WINDOW));

            View.inflate(context, getLayoutId(), this);

            contentFrame = (AspectRatioFrameLayout) findViewById(R.id.surface_container);
            bottomProgressBar = (ProgressBar) findViewById(R.id.bottom_progress);
            controlView = (ControlView) findViewById(R.id.cv_controll);

            contentFrame.setResizeMode(resizeMode);

            dialogHolder = new DialogHolder(context, this);

            controlView.setOnControllViewListener(this);

            videoPreview = new VideoPreview((ImageView) findViewById(R.id.iv_artwork));

            setTag(TAG);
        } else {
            float density = context.getResources().getDisplayMetrics().density;
            setMinimumWidth((int) (density * WIDTH_TINY_WINDOW));
            setMinimumHeight((int) (density * HEIGHT_TINY_WINDOW));

            View.inflate(context, getLayoutId(), this);
        }

        // 小窗宽高初始化为默认值
        tinyWidth = WIDTH_TINY_WINDOW;
        tinyHeight = HEIGHT_TINY_WINDOW;
    }

    @LayoutRes
    public int getLayoutId() {
        return R.layout.video_layout_base;
    }

    /**
     * 是否list播放
     *
     * @return
     */
    protected boolean isScreenList() {
        return screenState == ScreenState.SCREEN_LAYOUT_LIST
                || originScreenState == ScreenState.SCREEN_LAYOUT_LIST;
    }

    @Override
    public boolean setUp(@lombok.NonNull String playUrl, String name) {

        setVideoState(VideoState.CURRENT_STATE_ORIGIN);

        if (TextUtils.isEmpty(playUrl)) {
            Toast.makeText(context.getApplicationContext(), "视频地址无效", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!playUrl.equals(url)) {

            this.url = playUrl;
            this.videoName = name;

            if (videoName != null) {
                controlView.setVideoTitle(videoName);
            }

            VideoUtils.saveLastUrl(url);

            //自动缓冲，必须是wifi下,或者允许数据流量看视频,不能是视频列表
            if ((VideoUtils.isWifi() || dialogHolder.isPlayWithNotWifi())
                    && autoPrepare && !isScreenList()) {
                prepareSource();
            }


        }

        return true;
    }


    @Override
    public void setUpFullscreen(String url, String videoName) {
        Timber.d("锁定全屏播放:%s,%s", url, videoName);

        boolean setUp = setUp(url, videoName);

        if (setUp) {
            startWindowFullscreen(true);
        }

    }

    /**
     * 给list 的小窗设置播放url
     *
     * @param url
     * @param videoName
     */
    void setUpListTiny(String url, String videoName) {
        Timber.d("小窗播放滑出屏幕的视频:%s,%s", url, videoName);

        boolean setUp = setUp(url, videoName);

        if (setUp) {
            startWindowTiny(true);
        }
    }

    /**
     * 准备播放器，初始化播放源
     */
    @Override
    public void prepareSource() {
        Timber.d("准备播放源");

        if (TextUtils.isEmpty(url)) {
            setVideoState(VideoState.CURRENT_STATE_ERROR);
            Toast.makeText(context.getApplicationContext(), "视频地址无效", Toast.LENGTH_SHORT).show();
            return;
        }

        initTextureView();

        bindPlayer();

        VideoUtils.keepScreenOn(context);

        PlayerCore playerCore = PlayerCore.instance(context);

        playerCore.setMediaSource(SourceUtils.buildMediaSource(context, Uri.parse(url)));

        setVideoState(VideoState.CURRENT_STATE_PREPARING);

        playerCore.prepare();

    }

    /**
     * 准备播放器，注册监听
     */
    protected void bindPlayer() {
        Timber.d("准备播放器");

        controlView.setPlayer(PlayerCore.instance(context).getPlayer());

        PlayerCore.registerListener(this, this);

        removeTextureView();
        addTextureView();

        if (mAudioManager == null) {
            mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        }
        //请求音频焦点
        mAudioManager.requestAudioFocus(onAudioFocusChangeListener, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);

        // 记录正在播放的播放器
        ListVideoHelper.currentPlayingPlayer = this;
    }

    /**
     * 反注册监听
     */
    protected void unBindPlayer() {
        Timber.d("反注册监听");

        controlView.setPlayer(null);

        removeTextureView();

        PlayerCore.unRegisterListener(this, this);

        if (mAudioManager != null) {
            mAudioManager.abandonAudioFocus(onAudioFocusChangeListener);
        }

        // 取消记录正在播放的播放器
        ListVideoHelper.currentPlayingPlayer = null;
    }


    /**
     * 初始化 TextureView
     */
    public void initTextureView() {
        Timber.d("初始化 TextureView");

        PlayerCore instance = PlayerCore.instance(context);

        if (instance.getSurfaceView() == null) {
            instance.setSurfaceView(new TextureView(context));
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
        contentFrame.addView(PlayerCore.instance(context).getSurfaceView(), layoutParams);
    }

    /**
     * 移除 TextureView
     */
    public void removeTextureView() {
        Timber.d("移除 TextureView");

        View view = PlayerCore.instance(context).getSurfaceView();

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
        if (isVideoInPlayState()) {
            controlView.pause();
        }
    }

    @Override
    public void resume() {
        if (isVideoInPlayState()) {
            controlView.resume();
        }
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
        setVideoState(VideoState.CURRENT_STATE_COMPLETE);
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
    public void setFullScreenOrientation(@OrientationState.OrientationVlaue int orientation) {
        switch (orientation) {
            case ORIENTATION_TYPE_SENSOR:
                fullscreenOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR;
                break;
            case ORIENTATION_TYPE_PORTRAIT:
                fullscreenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                break;
            case ORIENTATION_TYPE_LANDSCAPE:
                fullscreenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                break;

            default:
                break;
        }
    }


    @Override
    public boolean isPlaying() {
        return videoState == VideoState.CURRENT_STATE_PLAYING;
    }


    @Override
    public boolean isVideoInPlayState() {
        return isPrepare() || isBuffering() || isPlaying() || isPause();
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
    protected void setVideoState(@VideoState.VideoStateValue int state) {
        Timber.d("改变当前播放状态:%s,%s", VideoState.getVideoStateName(videoState), VideoState.getVideoStateName(state));

        if (!autoPrepare && isPrepare() && state == VideoState.CURRENT_STATE_PLAYING_BUFFERING) {
            onPrepared();
        }

        updateUiAndState(state);

        videoState = state;

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
                break;
            case VideoState.CURRENT_STATE_ERROR:
                unBindPlayer();
                break;
            case VideoState.CURRENT_STATE_COMPLETE:
                backPress();
                unBindPlayer();
                VideoUtils.clearSavedProgress(url);
                break;
            default:
                break;
        }

    }

    /**
     * 更新显示Ui
     *
     * @param state
     */
    protected void updateUiAndState(@VideoState.VideoStateValue int state) {

        if (videoState != state) {

            updateBottomProgress();

            if (playEventListener != null) {
                playEventListener.onVideoState(state);
            }

            Timber.i(" %s 显示预览：%s", hashCode(), VideoState.getVideoStateName(state));
            //显示预览
            if (videoPreview != null) {
                if (shouldShowPreview(state)) {
                    videoPreview.showPreview(autoPrepare && !isScreenList());
                } else {
                    videoPreview.hidePreview();
                }
            }

            //自动缓冲避免显示缓冲状态
            if (shouldUpdateControll(state)) {
                controlView.updateVideoState(state);
            }
        }
    }

    /**
     * 是否应该更新控制界面
     *
     * @return
     */
    protected boolean shouldUpdateControll(@VideoState.VideoStateValue int state) {

        if (autoPrepare && (state == VideoState.CURRENT_STATE_PREPARING
                || state == VideoState.CURRENT_STATE_PLAYING_BUFFERING)) {
            return PlayerCore.instance(context).getPlayer().getPlayWhenReady();
        }

        return !(videoState == VideoState.CURRENT_STATE_PLAYING
                && state == VideoState.CURRENT_STATE_ORIGIN
                && screenState == ScreenState.SCREEN_WINDOW_FULLSCREEN);
    }

    /**
     * 是否应该显示预览
     *
     * @param state
     * @return
     */
    protected boolean shouldShowPreview(@VideoState.VideoStateValue int state) {
        boolean shouldShow = state == VideoState.CURRENT_STATE_ORIGIN
                || state == VideoState.CURRENT_STATE_COMPLETE;
        if (shouldShow) {
            return !(videoState == VideoState.CURRENT_STATE_PLAYING
                    && state == VideoState.CURRENT_STATE_ORIGIN
                    && screenState == ScreenState.SCREEN_WINDOW_FULLSCREEN);
        }
        return shouldShow;
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
    protected void setScreenState(@ScreenState.ScreenStateValue int state) {
        Timber.d("改变当前屏幕状态:%s", ScreenState.getScreenStateName(state));
        screenState = state;

        controlView.updateScreenState(state);

        switch (state) {
            case ScreenState.SCREEN_LAYOUT_ORIGIN:

                break;
            case ScreenState.SCREEN_WINDOW_FULLSCREEN:

                break;
            case ScreenState.SCREEN_WINDOW_FULLSCREEN_LOCK:

                break;
            case ScreenState.SCREEN_WINDOW_TINY:

                break;
            case ScreenState.SCREEN_LAYOUT_LIST:

                break;
            default:
                break;
        }

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

        PlayerCore playerCore = PlayerCore.instance(context);

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


        if (isVideoInPlayState()) {
            stop();
        }

        VideoUtils.saveProgress(url, getCurrentDuration());

        return false;
    }


    /**
     * 全屏播放
     */
    @Override
    public void startWindowFullscreen(boolean lockFullScreen) {
        Timber.i("全屏播放");

        if (screenState != ScreenState.SCREEN_WINDOW_FULLSCREEN && screenState != ScreenState.SCREEN_WINDOW_FULLSCREEN_LOCK) {
            Timber.i("全屏播放 111");
            ViewParent viewParent = getParent();

            if (viewParent != null) {
                Timber.i("全屏播放 222");
                Activity activity = getActivity();
                if (activity != null) {
                    Timber.i("全屏播放 333");
                    ViewGroup vp = (ViewGroup) activity  //加入contentView
                            .findViewById(Window.ID_ANDROID_CONTENT);

                    if (viewParent != vp) {
                        Timber.i("全屏播放 444");
                        originScreenState = screenState;

                        parent = (ViewGroup) viewParent;
                        indexInParent = parent.indexOfChild(this);
                        parentLayoutParams = getLayoutParams();
                        parent.removeView(this);   //从当前父布局移除

                        pause();

                        activity.setRequestedOrientation(fullscreenOrientation);
                        VideoUtils.showSupportActionBar(activity, false);

                        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

                        vp.addView(this, lp);

                        if (lockFullScreen) {
                            setScreenState(ScreenState.SCREEN_WINDOW_FULLSCREEN_LOCK);
                        } else {
                            setScreenState(ScreenState.SCREEN_WINDOW_FULLSCREEN);
                        }

                        resume();

                        bottomProgressBar.setVisibility(GONE);
                    }
                }
            }
        }

    }

    /**
     * 拿到当前VideoPlayer所在的Activity
     *
     * @return
     */
    protected Activity getActivity() {
        Activity activity = VideoUtils.getActivity(getContext());
        if (activity == null && ListVideoHelper.isPlayInList()) {
            WeakReference<View> reference = ListVideoHelper.containerReference;
            if (reference != null) {
                View view = reference.get();
                if (view != null) {
                    activity = VideoUtils.getActivity(view.getContext());
                }
            }
        }
        return activity;
    }


    /**
     * 小窗播放
     */
    @Override
    public void startWindowTiny() {
        startWindowTiny(false);
    }

    /**
     * 小窗播放
     */
    protected void startWindowTiny(boolean isList) {
        Timber.i("小窗播放");

        if (screenState != ScreenState.SCREEN_WINDOW_TINY
                && screenState != ScreenState.SCREEN_WINDOW_FULLSCREEN_LOCK) {

            if ((videoState == VideoState.CURRENT_STATE_ORIGIN
                    || videoState == VideoState.CURRENT_STATE_ERROR) && !isList) {
                return;
            }

            ViewParent viewParent = getParent();

            if (viewParent != null) {

                Activity activity = getActivity();

                if (activity != null) {
                    ViewGroup vp = (ViewGroup) activity.findViewById(Window.ID_ANDROID_CONTENT);

                    if (viewParent != vp) {
                        parent = (ViewGroup) viewParent;
                        indexInParent = parent.indexOfChild(this);
                        parentLayoutParams = getLayoutParams();
                        parent.removeView(this);   //从当前父布局移除

                        pause();

                        int widthTinyWindow = VideoUtils.dp2px(tinyWidth);
                        int heightTinyWindow = VideoUtils.dp2px(tinyHeight);
                        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(widthTinyWindow, heightTinyWindow);
                        lp.gravity = Gravity.RIGHT | Gravity.BOTTOM;
                        lp.bottomMargin = VideoUtils.dp2px(20);
                        lp.rightMargin = VideoUtils.dp2px(5);
                        vp.addView(this, lp);

                        originScreenState = screenState;

                        if (isList) {
                            setScreenState(ScreenState.SCREEN_LAYOUT_LIST_TINY);
                        } else {
                            setScreenState(ScreenState.SCREEN_WINDOW_TINY);
                        }

                        resume();

                    } else if (isList) {
                        setVisibility(VISIBLE);
                    }
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

        if (screenState == ScreenState.SCREEN_WINDOW_FULLSCREEN
                || screenState == ScreenState.SCREEN_WINDOW_TINY) {

            ViewGroup currentVP = (ViewGroup) getParent();

            if (parent != null && currentVP != null && parent != currentVP) {
                currentVP.removeView(this);   //从当前父布局移除

                pause();

                parent.addView(this, indexInParent, parentLayoutParams);

                Activity activity = getActivity();
                if (activity != null) {
                    activity.setRequestedOrientation(normalOrientation);
                }
                VideoUtils.showSupportActionBar(activity, true);

                setScreenState(originScreenState);
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
        } else if (screenState == ScreenState.SCREEN_LAYOUT_LIST_TINY && isShown()) {
            setVisibility(GONE);
            stopAndReset();
            return true;
        }

        return false;
    }


    @Override
    public void onTimelineChanged(Timeline timeline, Object o) {
        Timber.d("onTimelineChanged");

        controlView.updateAll();
        controlView.updateTimeBarMode();
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
    public void onPositionDiscontinuity() {
        Timber.d("onPositionDiscontinuity");

        controlView.updateAll();
    }


    @Override
    public void onPlayerStateChanged(boolean b, int i) {
        Timber.d("onPlayerStateChanged:%s,%s", b, i);

        controlView.updateProgress();

        switch (i) {
            case ExoPlayer.STATE_IDLE:
                if (videoState != VideoState.CURRENT_STATE_ORIGIN
                        && videoState != VideoState.CURRENT_STATE_ERROR) {
                    setVideoState(VideoState.CURRENT_STATE_ORIGIN);
                }
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
                Activity activity = getActivity();
                if (activity != null) {
                    activity.onBackPressed();
                }
            }
        } else if (id == R.id.iv_tiny_exit) {
            backOriginWindow();
        } else if (id == R.id.iv_tiny_close) {
            backOriginWindow(false);
        }
    }

    @Override
    public void onFullScreenClick() {
        if (screenState == ScreenState.SCREEN_LAYOUT_ORIGIN || screenState == ScreenState.SCREEN_LAYOUT_LIST) {
            startWindowFullscreen(false);
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

        //修正状态，list滑出自动小窗播放有可能一直显示缓冲状态
        if (videoState != VideoState.CURRENT_STATE_PLAYING
                && screenState != ScreenState.SCREEN_LAYOUT_ORIGIN
                && progress > 0 && bufferedProgress > 0) {
            SimpleExoPlayer player = PlayerCore.instance(context).getPlayer();
            if (player.getPlayWhenReady() && player.getCurrentPosition() > 0) {
                setVideoState(VideoState.CURRENT_STATE_PLAYING);
            }
        }
    }

    @Override
    public void changeVolume(float offset, int volumeStep) {
        Timber.d("changeVolume:%s,%s", offset, volumeStep);

        if (mAudioManager == null) {
            mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        }

        int volumePercent = VideoUtils.caculateVolume(mAudioManager, offset, volumeStep);

        dialogHolder.showVolumeDialog(volumePercent, offset < 0);
    }


    @Override
    public void changeBrightness(float offset, float brightnessStep) {
        Timber.d("changeBrightness:%s,%s", offset, brightnessStep);

        int brightnessPercent = VideoUtils.caculateBrightness(context, offset, brightnessStep);

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
    public void onTouchScreenEnd() {
        Timber.d("onTouchScreenEnd %s", hashCode());

        dialogHolder.dismissAllDialog();
    }

    @Override
    public void requestPrepareSourceData() {
        Timber.i("requestPrepareSourceData:%s", ScreenState.getScreenStateName(screenState));

        if (screenState == ScreenState.SCREEN_LAYOUT_ORIGIN && ListVideoHelper.isPlayInList()) {
            setScreenState(ScreenState.SCREEN_LAYOUT_LIST);
        }

        // 在list中时，告诉所有播放器播放新视频的事件
        if (isScreenList()) {
            PlayerCore.instance(context).notifyPlayNewVideoInList(this);
        }

        if (isBuffering()) {
            onPrepared();
        } else if (!VideoUtils.isWifi() && !dialogHolder.isPlayWithNotWifi()) {
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
        View view = PlayerCore.instance(context).getSurfaceView();
        if (view != null && view instanceof TextureView) {
            videoPreview.updatePreview(((TextureView) view).getBitmap());
        }
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

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        Timber.d("%s onAttachedToWindow", hashCode());

        Activity activity = getActivity();
        if (activity != null) {
            normalOrientation = activity.getResources().getConfiguration().orientation;
        }
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
     * 设置是否使用预览
     *
     * @param show
     */
    public void setShowPreview(boolean show) {
        if (videoPreview != null) {
            videoPreview.setShowPreview(show);
        }
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


    @Override
    public void onNewVideo() {
        Timber.i("播放新Video");

        stopAndReset();

        if (screenState != ScreenState.SCREEN_LAYOUT_LIST_TINY) {
            ListVideoHelper.stopWhenNewVideoPlay();
        }
    }

    /**
     * 停止播放并且重置状态
     */
    void stopAndReset() {
        Timber.i("停止播放并且重置状态");

        backOriginWindow(false);

        if (videoState != VideoState.CURRENT_STATE_ORIGIN) {
            stop();
            setVideoState(VideoState.CURRENT_STATE_ORIGIN);
        }
    }

    /**
     * 释放资源,调用该方法后播放器不能再被使用
     */
    public void release() {
        Timber.i("释放资源");
        if (mAudioManager != null) {
            mAudioManager.abandonAudioFocus(onAudioFocusChangeListener);
        }
        VideoUtils.clearSavedAutoPause(url);

        releaseAll();

        autoPrepare = false;
        dialogHolder = null;
        mAudioManager = null;
        parent = null;
        url = null;
        onAudioFocusChangeListener = null;
        videoName = null;
        playEventListener = null;
        videoPreview = null;
    }


    /**
     * 交换播放器，让画面在新的播放器继续播放
     */
    public static void swapVideoPlayer(@lombok.NonNull VideoPlayer oldPlayer,
                                       @lombok.NonNull VideoPlayer newPlayer,
                                       @VideoState.VideoStateValue int videoState,
                                       String url, String name) {
        Timber.i("交换播放器，让画面在新的播放器继续播放");

        oldPlayer.pause();
        oldPlayer.unBindPlayer();
        oldPlayer.setVideoState(VideoState.CURRENT_STATE_ORIGIN);

        if (url != null) {
            newPlayer.setUpListTiny(url, name);
        }
        newPlayer.bindPlayer();
        newPlayer.setVideoState(videoState);

        if (videoState == VideoState.CURRENT_STATE_PAUSE) {
            newPlayer.pause();
        } else {
            newPlayer.resume();
        }

    }


    /**
     * 开启小窗播放当滑出屏幕的时候
     *
     * @param listView
     */
    public static void playVideoInList(@lombok.NonNull ViewGroup listView, boolean tinyWhenOutScreen) {
        ListVideoHelper.setPlayInList(true);
        ListVideoHelper.setTinyWhenOutScreen(tinyWhenOutScreen);
        ListVideoHelper.init(listView);
    }

    /**
     * 返回键按下
     *
     * @return
     */
    public static boolean onBackPressed() {
        VideoPlayer player = ListVideoHelper.currentPlayingPlayer;
        if (player != null) {
            return player.backPress();
        }

        return false;
    }


    /**
     * 释放资源,调用该方法后播放器不能再被使用
     */
    public static void releaseAll() {
        ListVideoHelper.release();
        PlayerCore.releaseAll();
    }
}
