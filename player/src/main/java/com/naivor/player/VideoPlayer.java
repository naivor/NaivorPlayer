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

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.upstream.Allocator;
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
import timber.log.Timber;


/**
 * 播放器类
 * <p>
 * Created by tianlai on 17-7-6.
 */

@TargetApi(16)
public class VideoPlayer extends FrameLayout implements View.OnClickListener, OnControllViewListener, VideoController, ExoPlayer.EventListener, LoadControl {

    @Getter
    private AspectRatioFrameLayout textureViewContainer;
    //控制界面的控件
    private ControlView controlView;

    private PlayerCore playerCore;

    public static boolean ACTION_BAR_EXIST = true;
    public static boolean TOOL_BAR_EXIST = true;
    public static int FULLSCREEN_ORIENTATION = ActivityInfo.SCREEN_ORIENTATION_SENSOR;
    public static int NORMAL_ORIENTATION = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
    public static boolean SAVE_PROGRESS = true;

    public static boolean WIFI_TIP_DIALOG_SHOWED = false;

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
    int currentState = VideoState.CURRENT_STATE_ORIGIN;
    public
    @ScreenState.ScreenStateValue
    int currentScreen = ScreenState.SCREEN_LAYOUT_ORIGIN;

    public Map<String, String> headData;

    public String url = "";
    public Object[] objects = null;
    public int seekToInAdvance = 0;

    protected AudioManager mAudioManager;


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

        textureViewContainer = (AspectRatioFrameLayout) findViewById(R.id.surface_container);
        controlView = findViewById(R.id.cv_controll);

        textureViewContainer.setOnClickListener(this);
//        textureViewContainer.setOnTouchListener(this);

        controlView.setOnControllViewListener(this);

        mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);

        playerCore = PlayerCore.instance(getContext());
        playerCore.setEventListener(this);
        playerCore.setLoadControl(this);

        controlView.setPlayer(playerCore.getPlayer());
    }

    @LayoutRes
    public int getLayoutId() {
        return R.layout.video_layout_base;
    }


    public void setUp(String url, int screen, Object... objects) {

        if (!TextUtils.isEmpty(this.url) && TextUtils.equals(this.url, url)) {
            return;
        }
        this.url = url;
        this.objects = objects;
        this.currentScreen = screen;
        this.headData = null;

        setVideoState(VideoState.CURRENT_STATE_ORIGIN);
    }

    @Override
    public void onClick(View v) {
//        int i = v.getId();
//        if (i == R.id.start) {
//            Timber.i("点击开始按钮");
//            if (TextUtils.isEmpty(url)) {
//                Toast.makeText(getContext(), getResources().getString(R.string.no_url), Toast.LENGTH_SHORT).show();
//                return;
//            }
//            if (currentState == CURRENT_STATE_NORMAL || currentState == CURRENT_STATE_ERROR) {
//                if (!url.startsWith("file") && !VideoUtils.isWifi(getContext()) && !WIFI_TIP_DIALOG_SHOWED) {
//                    showWifiDialog();
//                    return;
//                }
//                prepareMediaPlayer();
//            } else if (currentState == CURRENT_STATE_PLAYING) {
//                Timber.d("pauseVideo [" + this.hashCode() + "] ");
//                playerCore.getPlayer().setPlayWhenReady(false);
//                setUiWitStateAndScreen(CURRENT_STATE_PAUSE);
//            } else if (currentState == CURRENT_STATE_PAUSE) {
//                playerCore.getPlayer().setPlayWhenReady(true);
//                setUiWitStateAndScreen(CURRENT_STATE_PLAYING);
//            } else if (currentState == CURRENT_STATE_AUTO_COMPLETE) {
//                prepareMediaPlayer();
//            }
//        } else if (i == R.id.fullscreen) {
//            Timber.i("onClick fullscreen [" + this.hashCode() + "] ");
//            if (currentState == CURRENT_STATE_AUTO_COMPLETE) return;
//            if (currentScreen == SCREEN_WINDOW_FULLSCREEN) {
//                //quit fullscreen
//                backPress();
//            } else {
//                Timber.d("toFullscreenActivity [" + this.hashCode() + "] ");
//                startWindowFullscreen();
//            }
//        } else if (i == R.id.surface_container && currentState == CURRENT_STATE_ERROR) {
//            Timber.i("onClick surfaceContainer State=Error [" + this.hashCode() + "] ");
//            prepareMediaPlayer();
//        }
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
        textureViewContainer.addView(playerCore.getSurfaceView(), layoutParams);
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

        resume();

        prepareMediaPlayer();
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
        return false;
    }

    @Override
    public boolean isPause() {
        return false;
    }

    @Override
    public boolean isPrepare() {
        return false;
    }

    @Override
    public boolean isBuffering() {
        return false;
    }

    public int widthRatio = 0;
    public int heightRatio = 0;

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (currentScreen == SCREEN_WINDOW_FULLSCREEN || currentScreen == SCREEN_WINDOW_TINY) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        if (widthRatio != 0 && heightRatio != 0) {
            int specWidth = MeasureSpec.getSize(widthMeasureSpec);
            int specHeight = (int) ((specWidth * (float) heightRatio) / widthRatio);
            setMeasuredDimension(specWidth, specHeight);

            int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(specWidth, MeasureSpec.EXACTLY);
            int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(specHeight, MeasureSpec.EXACTLY);
            getChildAt(0).measure(childWidthMeasureSpec, childHeightMeasureSpec);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }

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
                controlView.showBuffering(true);
                break;
            case VideoState.CURRENT_STATE_ERROR:
                controlView.showError();
                break;
            case VideoState.CURRENT_STATE_COMPLETE:
                controlView.onComplete();
                break;
        }

        currentState = state;
    }


    /**
     * 准备完成
     */
    public void onPrepared() {
        Timber.d("准备播放完成");

        if (currentState != VideoState.CURRENT_STATE_PREPARING) return;
        if (seekToInAdvance != 0) {
            playerCore.getPlayer().seekTo(seekToInAdvance);
            seekToInAdvance = 0;
        } else {
            int position = VideoUtils.getSavedProgress(getContext(), url);
            if (position != 0) {
                playerCore.getPlayer().seekTo(position);
            }
        }

        setVideoState(VideoState.CURRENT_STATE_PLAYING);
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
        showSupportActionBar(getContext());
    }

    public void onAutoCompletion() {
        //加上这句，避免循环播放video的时候，内存不断飙升。
        Runtime.getRuntime().gc();
        Timber.i("onAutoCompletion " + " [" + this.hashCode() + "] ");
        dismissVolumeDialog();
        dismissProgressDialog();
        dismissBrightnessDialog();

        setVideoState(VideoState.CURRENT_STATE_COMPLETE);

        if (currentScreen == SCREEN_WINDOW_FULLSCREEN) {
            backPress();
        }

        VideoUtils.saveProgress(getContext(), url, 0);
    }

    public void onCompletion() {
//        Timber.i( "onCompletion " + " [" + this.hashCode() + "] ");
//        //save position
//        if (currentState == CURRENT_STATE_PLAYING || currentState == CURRENT_STATE_PAUSE) {
//            int position = getCurrentPositionWhenPlaying();
////            int duration = getDuration();
//            VideoUtils.saveProgress(getContext(), url, position);
//        }
//        cancelProgressTimer();
//        setUiWitStateAndScreen(CURRENT_STATE_NORMAL);
//        // 清理缓存变量
//        textureViewContainer.removeView(JCMediaManager.textureView);
//        JCMediaManager.instance().currentVideoWidth = 0;
//        JCMediaManager.instance().currentVideoHeight = 0;
//
//        AudioManager mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
//        mAudioManager.abandonAudioFocus(onAudioFocusChangeListener);
//        VideoUtils.scanForActivity(getContext()).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//        clearFullscreenLayout();
//        VideoUtils.getActivity(getContext()).setRequestedOrientation(NORMAL_ORIENTATION);
//
//        JCMediaManager.textureView = null;
//        JCMediaManager.savedSurfaceTexture = null;
    }

    //退出全屏和小窗的方法
    public void playOnThisJcvd() {
//        Timber.i( "playOnThisJcvd " + " [" + this.hashCode() + "] ");
//        //1.清空全屏和小窗的jcvd
//        currentState = JCVideoPlayerManager.getSecondFloor().currentState;
//        clearFloatScreen();
//        //2.在本jcvd上播放
//        setUiWitStateAndScreen(currentState);
//        addTextureView();
    }

    public void clearFloatScreen() {
//        VideoUtils.getActivity(getContext()).setRequestedOrientation(NORMAL_ORIENTATION);
//        showSupportActionBar(getContext());
//        JCVideoPlayer currJcvd = JCVideoPlayerManager.getCurrentJcvd();
//        currJcvd.textureViewContainer.removeView(JCMediaManager.textureView);
//        ViewGroup vp = (ViewGroup) (VideoUtils.scanForActivity(getContext()))//.getWindow().getDecorView();
//                .findViewById(Window.ID_ANDROID_CONTENT);
//        vp.removeView(currJcvd);
//        JCVideoPlayerManager.setSecondFloor(null);
    }

    public static long lastAutoFullscreenTime = 0;

    //重力感应的时候调用的函数，
    public void autoFullscreen(float x) {
        if (isCurrentJcvd()
                && currentState == VideoState.CURRENT_STATE_PLAYING
                && currentScreen != SCREEN_WINDOW_FULLSCREEN
                && currentScreen != SCREEN_WINDOW_TINY) {
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

    public void autoQuitFullscreen() {
        if ((System.currentTimeMillis() - lastAutoFullscreenTime) > 2000
                && isCurrentJcvd()
                && currentState == VideoState.CURRENT_STATE_PLAYING
                && currentScreen == SCREEN_WINDOW_FULLSCREEN) {
            lastAutoFullscreenTime = System.currentTimeMillis();
            backPress();
        }
    }


    public void onVideoSizeChanged() {
//        Timber.i( "onVideoSizeChanged " + " [" + this.hashCode() + "] ");
//        if (JCMediaManager.textureView != null) {
//            JCMediaManager.textureView.setVideoSize(JCMediaManager.instance().getVideoSize());
//        }
    }


    public static boolean backPress() {
//        Timber.i( "backPress");
//        if ((System.currentTimeMillis() - CLICK_QUIT_FULLSCREEN_TIME) < FULL_SCREEN_NORMAL_DELAY)
//            return false;
//        if (JCVideoPlayerManager.getSecondFloor() != null) {
//            CLICK_QUIT_FULLSCREEN_TIME = System.currentTimeMillis();
//            JCVideoPlayer jcVideoPlayer = JCVideoPlayerManager.getSecondFloor();
//            jcVideoPlayer.onEvent(jcVideoPlayer.currentScreen == JCVideoPlayerStandard.SCREEN_WINDOW_FULLSCREEN ?
//                    JCUserAction.ON_QUIT_FULLSCREEN :
//                    JCUserAction.ON_QUIT_TINYSCREEN);
//            JCVideoPlayerManager.getFirstFloor().playOnThisJcvd();
//            return true;
//        } else if (JCVideoPlayerManager.getFirstFloor() != null &&
//                (JCVideoPlayerManager.getFirstFloor().currentScreen == SCREEN_WINDOW_FULLSCREEN ||
//                        JCVideoPlayerManager.getFirstFloor().currentScreen == SCREEN_WINDOW_TINY)) {//以前我总想把这两个判断写到一起，这分明是两个独立是逻辑
//            CLICK_QUIT_FULLSCREEN_TIME = System.currentTimeMillis();
//            //直接退出全屏和小窗
//            JCVideoPlayerManager.getCurrentJcvd().currentState = CURRENT_STATE_NORMAL;
//            JCVideoPlayerManager.getFirstFloor().clearFloatScreen();
//            JCMediaManager.instance().releaseMediaPlayer();
//            JCVideoPlayerManager.setFirstFloor(null);
//            return true;
//        }
        return false;
    }

    public void startWindowFullscreen() {
        Timber.i("startWindowFullscreen " + " [" + this.hashCode() + "] ");
        hideSupportActionBar(getContext());
        VideoUtils.getActivity(getContext()).setRequestedOrientation(FULLSCREEN_ORIENTATION);

        ViewGroup vp = (ViewGroup) (VideoUtils.getActivity(getContext()))//.getWindow().getDecorView();
                .findViewById(Window.ID_ANDROID_CONTENT);
        View old = vp.findViewById(FULLSCREEN_ID);
        if (old != null) {
            vp.removeView(old);
        }
////        ((ViewGroup)JCMediaManager.textureView.getParent()).removeView(JCMediaManager.textureView);
//        textureViewContainer.removeView(JCMediaManager.textureView);
//        try {
//            Constructor<JCVideoPlayer> constructor = (Constructor<JCVideoPlayer>) JCVideoPlayer.this.getClass().getConstructor(Context.class);
//            JCVideoPlayer jcVideoPlayer = constructor.newInstance(getContext());
//            jcVideoPlayer.setId(FULLSCREEN_ID);
//            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
//                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
//            vp.addView(jcVideoPlayer, lp);
//            jcVideoPlayer.setUp(url, JCVideoPlayerStandard.SCREEN_WINDOW_FULLSCREEN, objects);
//            jcVideoPlayer.setUiWitStateAndScreen(currentState);
//            jcVideoPlayer.addTextureView();
//            JCVideoPlayerManager.setSecondFloor(jcVideoPlayer);
////            final Animation ra = AnimationUtils.loadAnimation(getContext(), R.anim.start_fullscreen);
////            jcVideoPlayer.setAnimation(ra);
//            CLICK_QUIT_FULLSCREEN_TIME = System.currentTimeMillis();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }


    public void startWindowTiny() {
//        Timber.i( "startWindowTiny " + " [" + this.hashCode() + "] ");
//        if (currentState == CURRENT_STATE_NORMAL || currentState == CURRENT_STATE_ERROR) return;
//        ViewGroup vp = (ViewGroup) (VideoUtils.scanForActivity(getContext()))//.getWindow().getDecorView();
//                .findViewById(Window.ID_ANDROID_CONTENT);
//        View old = vp.findViewById(TINY_ID);
//        if (old != null) {
//            vp.removeView(old);
//        }
//        textureViewContainer.removeView(JCMediaManager.textureView);
//
//        try {
//            Constructor<JCVideoPlayer> constructor = (Constructor<JCVideoPlayer>) JCVideoPlayer.this.getClass().getConstructor(Context.class);
//            JCVideoPlayer jcVideoPlayer = constructor.newInstance(getContext());
//            jcVideoPlayer.setId(TINY_ID);
//            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(400, 400);
//            lp.gravity = Gravity.RIGHT | Gravity.BOTTOM;
//            vp.addView(jcVideoPlayer, lp);
//            jcVideoPlayer.setUp(url, JCVideoPlayerStandard.SCREEN_WINDOW_TINY, objects);
//            jcVideoPlayer.setUiWitStateAndScreen(currentState);
//            jcVideoPlayer.addTextureView();
//            JCVideoPlayerManager.setSecondFloor(jcVideoPlayer);
//        } catch (InstantiationException e) {
//            e.printStackTrace();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
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
                setVideoState(VideoState.CURRENT_STATE_ERROR);
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
                controlView.showBuffering(false);
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
        Timber.d("onFullScreenClick");
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
        return currentScreen;
    }

    @Override
    public int getCurrentState() {
        return currentState;
    }

    @Override
    public void onTouchScreenEnd() {
        Timber.d("onTouchScreenEnd");
    }


    public static AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    releaseAllVideos();
                    Timber.d("AUDIOFOCUS_LOSS [" + this.hashCode() + "]");
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
//                    try {
//                        if (JCMediaManager.instance().mediaPlayer != null &&
//                                JCMediaManager.instance().mediaPlayer.isPlaying()) {
//                            JCMediaManager.instance().mediaPlayer.pause();
//                        }
//                    } catch (IllegalStateException e) {
//                        e.printStackTrace();
//                    }
//                    Timber.d( "AUDIOFOCUS_LOSS_TRANSIENT [" + this.hashCode() + "]");
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    break;
            }
        }
    };


    public void release() {
//        if (url.equals(JCMediaManager.CURRENT_PLAYING_URL) &&
//                (System.currentTimeMillis() - CLICK_QUIT_FULLSCREEN_TIME) > FULL_SCREEN_NORMAL_DELAY) {
//            //在非全屏的情况下只能backPress()
//            if (JCVideoPlayerManager.getSecondFloor() != null &&
//                    JCVideoPlayerManager.getSecondFloor().currentScreen == SCREEN_WINDOW_FULLSCREEN) {//点击全屏
//            } else if (JCVideoPlayerManager.getSecondFloor() == null && JCVideoPlayerManager.getFirstFloor() != null &&
//                    JCVideoPlayerManager.getFirstFloor().currentScreen == SCREEN_WINDOW_FULLSCREEN) {//直接全屏
//            } else {
//                Timber.d( "release [" + this.hashCode() + "]");
//                releaseAllVideos();
//            }
//        }
    }

    //isCurrentJcvd and isCurrenPlayUrl should be two logic methods,isCurrentJcvd is for different jcvd with same
    //url when fullscreen or tiny screen. isCurrenPlayUrl is to find where is myself when back from tiny screen.
    //Sometimes they are overlap.
    public boolean isCurrentJcvd() {//虽然看这个函数很不爽，但是干不掉
//        return JCVideoPlayerManager.getCurrentJcvd() != null
//                && JCVideoPlayerManager.getCurrentJcvd() == this;

        return false;
    }

//    public boolean isCurrenPlayingUrl() {
//        return url.equals(JCMediaManager.CURRENT_PLAYING_URL);
//    }

    public static void releaseAllVideos() {
//        if ((System.currentTimeMillis() - CLICK_QUIT_FULLSCREEN_TIME) > FULL_SCREEN_NORMAL_DELAY) {
//            Timber.d( "releaseAllVideos");
//            JCVideoPlayerManager.completeAll();
//            JCMediaManager.instance().releaseMediaPlayer();
//        }
    }


    public static void startFullscreen(Context context, Class _class, String url, Object... objects) {
//        hideSupportActionBar(context);
//        VideoUtils.getActivity(context).setRequestedOrientation(FULLSCREEN_ORIENTATION);
//        ViewGroup vp = (ViewGroup) (VideoUtils.scanForActivity(context))//.getWindow().getDecorView();
//                .findViewById(Window.ID_ANDROID_CONTENT);
//        View old = vp.findViewById(JCVideoPlayer.FULLSCREEN_ID);
//        if (old != null) {
//            vp.removeView(old);
//        }
//        try {
//            Constructor<JCVideoPlayer> constructor = _class.getConstructor(Context.class);
//            final JCVideoPlayer jcVideoPlayer = constructor.newInstance(context);
//            jcVideoPlayer.setId(JCVideoPlayer.FULLSCREEN_ID);
//            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
//                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
//            vp.addView(jcVideoPlayer, lp);
////            final Animation ra = AnimationUtils.loadAnimation(context, R.anim.start_fullscreen);
////            jcVideoPlayer.setAnimation(ra);
//            jcVideoPlayer.setUp(url, JCVideoPlayerStandard.SCREEN_WINDOW_FULLSCREEN, objects);
//            CLICK_QUIT_FULLSCREEN_TIME = System.currentTimeMillis();
//            jcVideoPlayer.startButton.performClick();
//        } catch (InstantiationException e) {
//            e.printStackTrace();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    public static void hideSupportActionBar(Context context) {
//        if (ACTION_BAR_EXIST) {
//            ActionBar ab = VideoUtils.getActivity(context).getSupportActionBar();
//            if (ab != null) {
//                ab.setShowHideAnimationEnabled(false);
//                ab.hide();
//            }
//        }
//        if (TOOL_BAR_EXIST) {
//            VideoUtils.getActivity(context).getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
//        }
    }

    public static void showSupportActionBar(Context context) {
//        if (ACTION_BAR_EXIST) {
//            ActionBar ab = VideoUtils.getActivity(context).getSupportActionBar();
//            if (ab != null) {
//                ab.setShowHideAnimationEnabled(false);
//                ab.show();
//            }
//        }
//        if (TOOL_BAR_EXIST) {
//            VideoUtils.getActivity(context).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
//        }
    }

    public static class JCAutoFullscreenListener implements SensorEventListener {
        @Override
        public void onSensorChanged(SensorEvent event) {//可以得到传感器实时测量出来的变化值
            final float x = event.values[SensorManager.DATA_X];
            float y = event.values[SensorManager.DATA_Y];
            float z = event.values[SensorManager.DATA_Z];
            //过滤掉用力过猛会有一个反向的大数值
//            if (((x > -15 && x < -10) || (x < 15 && x > 10)) && Math.abs(y) < 1.5) {
//                if ((System.currentTimeMillis() - lastAutoFullscreenTime) > 2000) {
//                    if (JCVideoPlayerManager.getCurrentJcvd() != null) {
//                        JCVideoPlayerManager.getCurrentJcvd().autoFullscreen(x);
//                    }
//                    lastAutoFullscreenTime = System.currentTimeMillis();
//                }
//            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }

    public static void clearSavedProgress(Context context, String url) {
        VideoUtils.clearSavedProgress(context, url);
    }

    public void showWifiDialog() {
    }

    public void showProgressDialog(float deltaX,
                                   String seekTime, long seekTimePosition,
                                   String totalTime, long totalTimeDuration) {
    }

    public void dismissProgressDialog() {

    }

    public void showVolumeDialog(float deltaY, int volumePercent) {

    }

    public void dismissVolumeDialog() {

    }

    public void showBrightnessDialog(int brightnessPercent) {

    }

    public void dismissBrightnessDialog() {

    }

}
