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
import android.media.MediaPlayer;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.naivor.player.constant.OrientationState;
import com.naivor.player.constant.ScreenState;
import com.naivor.player.controll.VideoController;
import com.naivor.player.core.PlayerCore;
import com.naivor.player.utils.VideoUtils;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import lombok.Getter;
import timber.log.Timber;

import static android.content.ContentValues.TAG;

/**
 * 播放器类
 * <p>
 * Created by tianlai on 17-7-6.
 */

@TargetApi(16)
public class VideoPlayer extends FrameLayout implements View.OnClickListener, SeekBar.OnSeekBarChangeListener, View.OnTouchListener, VideoController {

    //控制界面的控件
    @Getter
    private ImageView startButton;
    @Getter
    private SeekBar progressBar;
    @Getter
    private ImageView fullscreenButton;
    @Getter
    private TextView currentTimeTextView, totalTimeTextView;
    @Getter
    private AspectRatioFrameLayout textureViewContainer;
    @Getter
    private ViewGroup topContainer, bottomContainer;

    private PlayerCore playerCore;


    public static boolean ACTION_BAR_EXIST = true;
    public static boolean TOOL_BAR_EXIST = true;
    public static int FULLSCREEN_ORIENTATION = ActivityInfo.SCREEN_ORIENTATION_SENSOR;
    public static int NORMAL_ORIENTATION = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
    public static boolean SAVE_PROGRESS = true;

    public static boolean WIFI_TIP_DIALOG_SHOWED = false;

    public static final int FULLSCREEN_ID = 33797;
    public static final int TINY_ID = 33798;
    public static final int THRESHOLD = 80;
    public static final int FULL_SCREEN_NORMAL_DELAY = 300;
    public static long CLICK_QUIT_FULLSCREEN_TIME = 0;

    public static final int SCREEN_LAYOUT_NORMAL = 0;
    public static final int SCREEN_LAYOUT_LIST = 1;

    public static final int SCREEN_WINDOW_FULLSCREEN = 2;
    public static final int SCREEN_WINDOW_TINY = 3;

    public static final int CURRENT_STATE_NORMAL = 0;
    public static final int CURRENT_STATE_PREPARING = 1;
    public static final int CURRENT_STATE_PLAYING = 2;
    public static final int CURRENT_STATE_PLAYING_BUFFERING_START = 3;
    public static final int CURRENT_STATE_PAUSE = 5;
    public static final int CURRENT_STATE_AUTO_COMPLETE = 6;
    public static final int CURRENT_STATE_ERROR = 7;

    public static int BACKUP_PLAYING_BUFFERING_STATE = -1;

    public int currentState = -1;
    public int currentScreen = -1;
    public boolean loop = false;
    public Map<String, String> headData;

    public String url = "";
    public Object[] objects = null;
    public int seekToInAdvance = 0;

    protected static Timer UPDATE_PROGRESS_TIMER;

    protected int mScreenWidth;
    protected int mScreenHeight;
    protected AudioManager mAudioManager;
    protected Handler mHandler;
    protected ProgressTimerTask mProgressTimerTask;

    protected boolean mTouchingProgressBar;
    protected float mDownX;
    protected float mDownY;
    protected boolean mChangeVolume;
    protected boolean mChangePosition;
    protected boolean mChangeBrightness;
    protected int mGestureDownPosition;
    protected int mGestureDownVolume;
    protected float mGestureDownBrightness;
    protected int mSeekTimePosition;


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
        View.inflate(context, getLayoutId(), this);

        startButton = (ImageView) findViewById(R.id.start);
        fullscreenButton = (ImageView) findViewById(R.id.fullscreen);
        progressBar = (SeekBar) findViewById(R.id.bottom_seek_progress);
        currentTimeTextView = (TextView) findViewById(R.id.current);
        totalTimeTextView = (TextView) findViewById(R.id.total);
        bottomContainer = (ViewGroup) findViewById(R.id.layout_bottom);
        textureViewContainer = (AspectRatioFrameLayout) findViewById(R.id.surface_container);
        topContainer = (ViewGroup) findViewById(R.id.layout_top);

        startButton.setOnClickListener(this);
        fullscreenButton.setOnClickListener(this);
        progressBar.setOnSeekBarChangeListener(this);
        bottomContainer.setOnClickListener(this);
        textureViewContainer.setOnClickListener(this);
        textureViewContainer.setOnTouchListener(this);

//        mScreenWidth = getContext().getResources().getDisplayMetrics().widthPixels;
//        mScreenHeight = getContext().getResources().getDisplayMetrics().heightPixels;
//        mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
//        mHandler = new Handler();


        playerCore = PlayerCore.instance(getContext());
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
        setUiWitStateAndScreen(CURRENT_STATE_NORMAL);
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.start) {
            Log.i(TAG, "onClick start [" + this.hashCode() + "] ");
            if (TextUtils.isEmpty(url)) {
                Toast.makeText(getContext(), getResources().getString(R.string.no_url), Toast.LENGTH_SHORT).show();
                return;
            }
            if (currentState == CURRENT_STATE_NORMAL || currentState == CURRENT_STATE_ERROR) {
                if (!url.startsWith("file") && !VideoUtils.isWifi(getContext()) && !WIFI_TIP_DIALOG_SHOWED) {
                    showWifiDialog();
                    return;
                }
                prepareMediaPlayer();
            } else if (currentState == CURRENT_STATE_PLAYING) {
                Log.d(TAG, "pauseVideo [" + this.hashCode() + "] ");
                playerCore.getPlayer().setPlayWhenReady(false);
                setUiWitStateAndScreen(CURRENT_STATE_PAUSE);
            } else if (currentState == CURRENT_STATE_PAUSE) {
                playerCore.getPlayer().setPlayWhenReady(true);
                setUiWitStateAndScreen(CURRENT_STATE_PLAYING);
            } else if (currentState == CURRENT_STATE_AUTO_COMPLETE) {
                prepareMediaPlayer();
            }
        } else if (i == R.id.fullscreen) {
            Log.i(TAG, "onClick fullscreen [" + this.hashCode() + "] ");
            if (currentState == CURRENT_STATE_AUTO_COMPLETE) return;
            if (currentScreen == SCREEN_WINDOW_FULLSCREEN) {
                //quit fullscreen
                backPress();
            } else {
                Log.d(TAG, "toFullscreenActivity [" + this.hashCode() + "] ");
                startWindowFullscreen();
            }
        } else if (i == R.id.surface_container && currentState == CURRENT_STATE_ERROR) {
            Log.i(TAG, "onClick surfaceContainer State=Error [" + this.hashCode() + "] ");
            prepareMediaPlayer();
        }
    }

    public void prepareMediaPlayer() {
        Log.d(TAG, "prepareMediaPlayer [" + this.hashCode() + "] ");
        initTextureView();
        addTextureView();
        VideoUtils.getActivity(getContext()).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        JCMediaManager.CURRENT_PLAYING_URL = url;
        JCMediaManager.CURRENT_PLING_LOOP = loop;
        JCMediaManager.MAP_HEADER_DATA = headData;


        setUiWitStateAndScreen(CURRENT_STATE_PREPARING);
        JCVideoPlayerManager.setFirstFloor(this);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        int id = v.getId();
        if (id == R.id.surface_container) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Log.i(TAG, "onTouch surfaceContainer actionDown [" + this.hashCode() + "] ");
                    mTouchingProgressBar = true;

                    mDownX = x;
                    mDownY = y;
                    mChangeVolume = false;
                    mChangePosition = false;
                    mChangeBrightness = false;
                    break;
                case MotionEvent.ACTION_MOVE:
                    Log.i(TAG, "onTouch surfaceContainer actionMove [" + this.hashCode() + "] ");
                    float deltaX = x - mDownX;
                    float deltaY = y - mDownY;
                    float absDeltaX = Math.abs(deltaX);
                    float absDeltaY = Math.abs(deltaY);
                    if (currentScreen == SCREEN_WINDOW_FULLSCREEN) {
                        if (!mChangePosition && !mChangeVolume && !mChangeBrightness) {
                            if (absDeltaX > THRESHOLD || absDeltaY > THRESHOLD) {
                                cancelProgressTimer();
                                if (absDeltaX >= THRESHOLD) {
                                    // 全屏模式下的CURRENT_STATE_ERROR状态下,不响应进度拖动事件.
                                    // 否则会因为mediaplayer的状态非法导致App Crash
                                    if (currentState != CURRENT_STATE_ERROR) {
                                        mChangePosition = true;
                                        mGestureDownPosition = getCurrentPositionWhenPlaying();
                                    }
                                } else {
                                    //如果y轴滑动距离超过设置的处理范围，那么进行滑动事件处理
                                    if (mDownX < mScreenWidth * 0.5f) {//左侧改变亮度
                                        mChangeBrightness = true;
                                        WindowManager.LayoutParams lp = VideoUtils.getActivity(getContext()).getWindow().getAttributes();
                                        if (lp.screenBrightness < 0) {
                                            try {
                                                mGestureDownBrightness = Settings.System.getInt(getContext().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
                                                Log.i(TAG, "current system brightness: " + mGestureDownBrightness);
                                            } catch (Settings.SettingNotFoundException e) {
                                                e.printStackTrace();
                                            }
                                        } else {
                                            mGestureDownBrightness = lp.screenBrightness * 255;
                                            Log.i(TAG, "current activity brightness: " + mGestureDownBrightness);
                                        }
                                    } else {//右侧改变声音
                                        mChangeVolume = true;
                                        mGestureDownVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                                    }
                                }
                            }
                        }
                    }
                    if (mChangePosition) {
                        int totalTimeDuration = getDuration();
                        mSeekTimePosition = (int) (mGestureDownPosition + deltaX * totalTimeDuration / mScreenWidth);
                        if (mSeekTimePosition > totalTimeDuration)
                            mSeekTimePosition = totalTimeDuration;
                        String seekTime = VideoUtils.formateTime(mSeekTimePosition);
                        String totalTime = VideoUtils.formateTime(totalTimeDuration);

                        showProgressDialog(deltaX, seekTime, mSeekTimePosition, totalTime, totalTimeDuration);
                    }
                    if (mChangeVolume) {
                        deltaY = -deltaY;
                        int max = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                        int deltaV = (int) (max * deltaY * 3 / mScreenHeight);
                        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mGestureDownVolume + deltaV, 0);
                        //dialog中显示百分比
                        int volumePercent = (int) (mGestureDownVolume * 100 / max + deltaY * 3 * 100 / mScreenHeight);
                        showVolumeDialog(-deltaY, volumePercent);
                    }

                    if (mChangeBrightness) {
                        deltaY = -deltaY;
                        int deltaV = (int) (255 * deltaY * 3 / mScreenHeight);
                        WindowManager.LayoutParams params = VideoUtils.getActivity(getContext()).getWindow().getAttributes();
                        if (((mGestureDownBrightness + deltaV) / 255) >= 1) {//这和声音有区别，必须自己过滤一下负值
                            params.screenBrightness = 1;
                        } else if (((mGestureDownBrightness + deltaV) / 255) <= 0) {
                            params.screenBrightness = 0.01f;
                        } else {
                            params.screenBrightness = (mGestureDownBrightness + deltaV) / 255;
                        }
                        VideoUtils.getActivity(getContext()).getWindow().setAttributes(params);
                        //dialog中显示百分比
                        int brightnessPercent = (int) (mGestureDownBrightness * 100 / 255 + deltaY * 3 * 100 / mScreenHeight);
                        showBrightnessDialog(brightnessPercent);
//                        mDownY = y;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    Log.i(TAG, "onTouch surfaceContainer actionUp [" + this.hashCode() + "] ");
                    mTouchingProgressBar = false;
                    dismissProgressDialog();
                    dismissVolumeDialog();
                    dismissBrightnessDialog();
                    if (mChangePosition) {
                        JCMediaManager.instance().mediaPlayer.seekTo(mSeekTimePosition);
                        int duration = getDuration();
                        int progress = mSeekTimePosition * 100 / (duration == 0 ? 1 : duration);
                        progressBar.setProgress(progress);
                    }
                    if (mChangeVolume) {

                    }
                    startProgressTimer();
                    break;
            }
        }
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

    public void setUiWitStateAndScreen(int state) {
        currentState = state;
        switch (currentState) {
            case CURRENT_STATE_NORMAL:
                cancelProgressTimer();
                if (isCurrentJcvd()) {//这个if是无法取代的，否则进入全屏的时候会releaseMediaPlayer
                    JCMediaManager.instance().releaseMediaPlayer();
                }
                break;
            case CURRENT_STATE_PREPARING:
                resetProgressAndTime();
                break;
            case CURRENT_STATE_PLAYING:
            case CURRENT_STATE_PAUSE:
            case CURRENT_STATE_PLAYING_BUFFERING_START:
                startProgressTimer();
                break;
            case CURRENT_STATE_ERROR:
                cancelProgressTimer();
                break;
            case CURRENT_STATE_AUTO_COMPLETE:
                cancelProgressTimer();
                progressBar.setProgress(100);
                currentTimeTextView.setText(totalTimeTextView.getText());
                break;
        }
    }

    public void startProgressTimer() {
        cancelProgressTimer();
        UPDATE_PROGRESS_TIMER = new Timer();
        mProgressTimerTask = new ProgressTimerTask();
        UPDATE_PROGRESS_TIMER.schedule(mProgressTimerTask, 0, 300);
    }

    public void cancelProgressTimer() {
        if (UPDATE_PROGRESS_TIMER != null) {
            UPDATE_PROGRESS_TIMER.cancel();
        }
        if (mProgressTimerTask != null) {
            mProgressTimerTask.cancel();
        }
    }

    public void onPrepared() {
        Log.i(TAG, "onPrepared " + " [" + this.hashCode() + "] ");

        if (currentState != CURRENT_STATE_PREPARING) return;
        if (seekToInAdvance != 0) {
            JCMediaManager.instance().mediaPlayer.seekTo(seekToInAdvance);
            seekToInAdvance = 0;
        } else {
            int position = VideoUtils.getSavedProgress(getContext(), url);
            if (position != 0) {
                JCMediaManager.instance().mediaPlayer.seekTo(position);
            }
        }
        startProgressTimer();
        setUiWitStateAndScreen(CURRENT_STATE_PLAYING);
    }

    public void clearFullscreenLayout() {
        ViewGroup vp = (ViewGroup) (VideoUtils.scanForActivity(getContext()))//.getWindow().getDecorView();
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
        Log.i(TAG, "onAutoCompletion " + " [" + this.hashCode() + "] ");
        dismissVolumeDialog();
        dismissProgressDialog();
        dismissBrightnessDialog();
        cancelProgressTimer();
        setUiWitStateAndScreen(CURRENT_STATE_AUTO_COMPLETE);

        if (currentScreen == SCREEN_WINDOW_FULLSCREEN) {
            backPress();
        }
        VideoUtils.saveProgress(getContext(), url, 0);
    }

    public void onCompletion() {
        Log.i(TAG, "onCompletion " + " [" + this.hashCode() + "] ");
        //save position
        if (currentState == CURRENT_STATE_PLAYING || currentState == CURRENT_STATE_PAUSE) {
            int position = getCurrentPositionWhenPlaying();
//            int duration = getDuration();
            VideoUtils.saveProgress(getContext(), url, position);
        }
        cancelProgressTimer();
        setUiWitStateAndScreen(CURRENT_STATE_NORMAL);
        // 清理缓存变量
        textureViewContainer.removeView(JCMediaManager.textureView);
        JCMediaManager.instance().currentVideoWidth = 0;
        JCMediaManager.instance().currentVideoHeight = 0;

        AudioManager mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.abandonAudioFocus(onAudioFocusChangeListener);
        VideoUtils.scanForActivity(getContext()).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        clearFullscreenLayout();
        VideoUtils.getActivity(getContext()).setRequestedOrientation(NORMAL_ORIENTATION);

        JCMediaManager.textureView = null;
        JCMediaManager.savedSurfaceTexture = null;
    }

    //退出全屏和小窗的方法
    public void playOnThisJcvd() {
        Log.i(TAG, "playOnThisJcvd " + " [" + this.hashCode() + "] ");
        //1.清空全屏和小窗的jcvd
        currentState = JCVideoPlayerManager.getSecondFloor().currentState;
        clearFloatScreen();
        //2.在本jcvd上播放
        setUiWitStateAndScreen(currentState);
        addTextureView();
    }

    public void clearFloatScreen() {
        VideoUtils.getActivity(getContext()).setRequestedOrientation(NORMAL_ORIENTATION);
        showSupportActionBar(getContext());
        JCVideoPlayer currJcvd = JCVideoPlayerManager.getCurrentJcvd();
        currJcvd.textureViewContainer.removeView(JCMediaManager.textureView);
        ViewGroup vp = (ViewGroup) (VideoUtils.scanForActivity(getContext()))//.getWindow().getDecorView();
                .findViewById(Window.ID_ANDROID_CONTENT);
        vp.removeView(currJcvd);
        JCVideoPlayerManager.setSecondFloor(null);
    }

    public static long lastAutoFullscreenTime = 0;

    //重力感应的时候调用的函数，
    public void autoFullscreen(float x) {
        if (isCurrentJcvd()
                && currentState == CURRENT_STATE_PLAYING
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
                && currentState == CURRENT_STATE_PLAYING
                && currentScreen == SCREEN_WINDOW_FULLSCREEN) {
            lastAutoFullscreenTime = System.currentTimeMillis();
            backPress();
        }
    }

    public void onSeekComplete() {

    }

    public void onError(int what, int extra) {
        Log.e(TAG, "onError " + what + " - " + extra + " [" + this.hashCode() + "] ");
        if (what != 38 && extra != -38) {
            setUiWitStateAndScreen(CURRENT_STATE_ERROR);
            if (isCurrentJcvd()) {
                JCMediaManager.instance().releaseMediaPlayer();
            }
        }
    }


    public void onInfo(int what, int extra) {
        Log.d(TAG, "onInfo what - " + what + " extra - " + extra);
        if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
            if (currentState == CURRENT_STATE_PLAYING_BUFFERING_START) return;
            BACKUP_PLAYING_BUFFERING_STATE = currentState;
            setUiWitStateAndScreen(CURRENT_STATE_PLAYING_BUFFERING_START);//没这个case
            Log.d(TAG, "MEDIA_INFO_BUFFERING_START");
        } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
            if (BACKUP_PLAYING_BUFFERING_STATE != -1) {
                setUiWitStateAndScreen(BACKUP_PLAYING_BUFFERING_STATE);
                BACKUP_PLAYING_BUFFERING_STATE = -1;
            }
            Log.d(TAG, "MEDIA_INFO_BUFFERING_END");
        }
    }

    public void onVideoSizeChanged() {
        Log.i(TAG, "onVideoSizeChanged " + " [" + this.hashCode() + "] ");
        if (JCMediaManager.textureView != null) {
            JCMediaManager.textureView.setVideoSize(JCMediaManager.instance().getVideoSize());
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        Log.i(TAG, "bottomProgress onStartTrackingTouch [" + this.hashCode() + "] ");
        cancelProgressTimer();
        ViewParent vpdown = getParent();
        while (vpdown != null) {
            vpdown.requestDisallowInterceptTouchEvent(true);
            vpdown = vpdown.getParent();
        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        Log.i(TAG, "bottomProgress onStopTrackingTouch [" + this.hashCode() + "] ");
        startProgressTimer();
        ViewParent vpup = getParent();
        while (vpup != null) {
            vpup.requestDisallowInterceptTouchEvent(false);
            vpup = vpup.getParent();
        }
        if (currentState != CURRENT_STATE_PLAYING &&
                currentState != CURRENT_STATE_PAUSE) return;
        int time = seekBar.getProgress() * getDuration() / 100;
        JCMediaManager.instance().mediaPlayer.seekTo(time);
        Log.i(TAG, "seekTo " + time + " [" + this.hashCode() + "] ");
    }

    public static boolean backPress() {
        Log.i(TAG, "backPress");
        if ((System.currentTimeMillis() - CLICK_QUIT_FULLSCREEN_TIME) < FULL_SCREEN_NORMAL_DELAY)
            return false;
        if (JCVideoPlayerManager.getSecondFloor() != null) {
            CLICK_QUIT_FULLSCREEN_TIME = System.currentTimeMillis();
            JCVideoPlayer jcVideoPlayer = JCVideoPlayerManager.getSecondFloor();
            jcVideoPlayer.onEvent(jcVideoPlayer.currentScreen == JCVideoPlayerStandard.SCREEN_WINDOW_FULLSCREEN ?
                    JCUserAction.ON_QUIT_FULLSCREEN :
                    JCUserAction.ON_QUIT_TINYSCREEN);
            JCVideoPlayerManager.getFirstFloor().playOnThisJcvd();
            return true;
        } else if (JCVideoPlayerManager.getFirstFloor() != null &&
                (JCVideoPlayerManager.getFirstFloor().currentScreen == SCREEN_WINDOW_FULLSCREEN ||
                        JCVideoPlayerManager.getFirstFloor().currentScreen == SCREEN_WINDOW_TINY)) {//以前我总想把这两个判断写到一起，这分明是两个独立是逻辑
            CLICK_QUIT_FULLSCREEN_TIME = System.currentTimeMillis();
            //直接退出全屏和小窗
            JCVideoPlayerManager.getCurrentJcvd().currentState = CURRENT_STATE_NORMAL;
            JCVideoPlayerManager.getFirstFloor().clearFloatScreen();
            JCMediaManager.instance().releaseMediaPlayer();
            JCVideoPlayerManager.setFirstFloor(null);
            return true;
        }
        return false;
    }

    public void startWindowFullscreen() {
        Log.i(TAG, "startWindowFullscreen " + " [" + this.hashCode() + "] ");
        hideSupportActionBar(getContext());
        VideoUtils.getActivity(getContext()).setRequestedOrientation(FULLSCREEN_ORIENTATION);

        ViewGroup vp = (ViewGroup) (VideoUtils.scanForActivity(getContext()))//.getWindow().getDecorView();
                .findViewById(Window.ID_ANDROID_CONTENT);
        View old = vp.findViewById(FULLSCREEN_ID);
        if (old != null) {
            vp.removeView(old);
        }
//        ((ViewGroup)JCMediaManager.textureView.getParent()).removeView(JCMediaManager.textureView);
        textureViewContainer.removeView(JCMediaManager.textureView);
        try {
            Constructor<JCVideoPlayer> constructor = (Constructor<JCVideoPlayer>) JCVideoPlayer.this.getClass().getConstructor(Context.class);
            JCVideoPlayer jcVideoPlayer = constructor.newInstance(getContext());
            jcVideoPlayer.setId(FULLSCREEN_ID);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            vp.addView(jcVideoPlayer, lp);
            jcVideoPlayer.setUp(url, JCVideoPlayerStandard.SCREEN_WINDOW_FULLSCREEN, objects);
            jcVideoPlayer.setUiWitStateAndScreen(currentState);
            jcVideoPlayer.addTextureView();
            JCVideoPlayerManager.setSecondFloor(jcVideoPlayer);
//            final Animation ra = AnimationUtils.loadAnimation(getContext(), R.anim.start_fullscreen);
//            jcVideoPlayer.setAnimation(ra);
            CLICK_QUIT_FULLSCREEN_TIME = System.currentTimeMillis();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void startWindowTiny() {
        Log.i(TAG, "startWindowTiny " + " [" + this.hashCode() + "] ");
        if (currentState == CURRENT_STATE_NORMAL || currentState == CURRENT_STATE_ERROR) return;
        ViewGroup vp = (ViewGroup) (VideoUtils.scanForActivity(getContext()))//.getWindow().getDecorView();
                .findViewById(Window.ID_ANDROID_CONTENT);
        View old = vp.findViewById(TINY_ID);
        if (old != null) {
            vp.removeView(old);
        }
        textureViewContainer.removeView(JCMediaManager.textureView);

        try {
            Constructor<JCVideoPlayer> constructor = (Constructor<JCVideoPlayer>) JCVideoPlayer.this.getClass().getConstructor(Context.class);
            JCVideoPlayer jcVideoPlayer = constructor.newInstance(getContext());
            jcVideoPlayer.setId(TINY_ID);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(400, 400);
            lp.gravity = Gravity.RIGHT | Gravity.BOTTOM;
            vp.addView(jcVideoPlayer, lp);
            jcVideoPlayer.setUp(url, JCVideoPlayerStandard.SCREEN_WINDOW_TINY, objects);
            jcVideoPlayer.setUiWitStateAndScreen(currentState);
            jcVideoPlayer.addTextureView();
            JCVideoPlayerManager.setSecondFloor(jcVideoPlayer);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void seekTo(long millisecond) {
        if (playerCore != null) {
            SimpleExoPlayer player = playerCore.getPlayer();
            long target;
            long duration = player.getDuration();

            if (millisecond < 0) {
                target = 0;
            } else if (millisecond > duration) {
                target = duration;
            } else {
                target = millisecond;
            }
            player.seekTo(target);
        }
    }

    @Override
    public void fastward(long millisecond) {
        if (playerCore != null) {
            if (millisecond <= 0) {
                return;
            }

            SimpleExoPlayer player = playerCore.getPlayer();

            seekTo(Math.min(player.getCurrentPosition() + millisecond, player.getDuration()));
        }
    }

    @Override
    public void backward(long millisecond) {
        if (playerCore != null) {
            if (millisecond <= 0) {
                return;
            }

            SimpleExoPlayer player = playerCore.getPlayer();

            seekTo(Math.max(player.getCurrentPosition() - millisecond, 0));
        }
    }

    @Override
    public long getCurrentDuration() {
        if (playerCore != null) {
            return playerCore.getPlayer().getCurrentPosition();
        }
        return 0;
    }

    @Override
    public long getTotalDuration() {
        if (playerCore != null) {
            return playerCore.getPlayer().getDuration();
        }
        return 0;
    }

    @Override
    public void setOrientation(@ScreenState.ScreenStateType int windowType, @OrientationState.OrientationType int orientation) {

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

    public class ProgressTimerTask extends TimerTask {
        @Override
        public void run() {
            if (currentState == CURRENT_STATE_PLAYING || currentState == CURRENT_STATE_PAUSE || currentState == CURRENT_STATE_PLAYING_BUFFERING_START) {
//                Log.v(TAG, "onProgressUpdate " + position + "/" + duration + " [" + this.hashCode() + "] ");
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        setProgressAndText();
                    }
                });
            }
        }
    }

    public int getCurrentPositionWhenPlaying() {
        int position = 0;
        if (JCMediaManager.instance().mediaPlayer == null)
            return position;//这行代码不应该在这，如果代码和逻辑万无一失的话，心头之恨呐
        if (currentState == CURRENT_STATE_PLAYING ||
                currentState == CURRENT_STATE_PAUSE ||
                currentState == CURRENT_STATE_PLAYING_BUFFERING_START) {
            try {
                position = JCMediaManager.instance().mediaPlayer.getCurrentPosition();
            } catch (IllegalStateException e) {
                e.printStackTrace();
                return position;
            }
        }
        return position;
    }

    public int getDuration() {
        int duration = 0;
        if (JCMediaManager.instance().mediaPlayer == null) return duration;
        try {
            duration = JCMediaManager.instance().mediaPlayer.getDuration();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return duration;
        }
        return duration;
    }

    public void setProgressAndText() {
        int position = getCurrentPositionWhenPlaying();
        int duration = getDuration();
        int progress = position * 100 / (duration == 0 ? 1 : duration);
        if (!mTouchingProgressBar) {
            if (progress != 0) progressBar.setProgress(progress);
        }
        if (position != 0) currentTimeTextView.setText(VideoUtils.formateTime(position));
        totalTimeTextView.setText(VideoUtils.formateTime(duration));
    }

    public void setBufferProgress(int bufferProgress) {
        if (bufferProgress != 0) progressBar.setSecondaryProgress(bufferProgress);
    }

    public void resetProgressAndTime() {
        progressBar.setProgress(0);
        progressBar.setSecondaryProgress(0);
        currentTimeTextView.setText(VideoUtils.formateTime(0));
        totalTimeTextView.setText(VideoUtils.formateTime(0));
    }

    public static AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    releaseAllVideos();
                    Log.d(TAG, "AUDIOFOCUS_LOSS [" + this.hashCode() + "]");
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    try {
                        if (JCMediaManager.instance().mediaPlayer != null &&
                                JCMediaManager.instance().mediaPlayer.isPlaying()) {
                            JCMediaManager.instance().mediaPlayer.pause();
                        }
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                    Log.d(TAG, "AUDIOFOCUS_LOSS_TRANSIENT [" + this.hashCode() + "]");
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    break;
            }
        }
    };

    @Override
    public void start() {
        if (playerCore != null) {
            playerCore.getPlayer().setPlayWhenReady(true);
        }
    }

    @Override
    public void pause() {
        if (playerCore != null) {
            playerCore.getPlayer().setPlayWhenReady(false);
        }
    }

    @Override
    public void resume() {
        start();
    }

    @Override
    public void previous() {
//        if (playerCore!=null) {
//            Timeline timeline = player.getCurrentTimeline();
//            if (timeline.isEmpty()) {
//                return;
//            }
//            int windowIndex = player.getCurrentWindowIndex();
//            timeline.getWindow(windowIndex, window);
//            if (windowIndex > 0 && (player.getCurrentPosition() <= MAX_POSITION_FOR_SEEK_TO_PREVIOUS
//                    || (window.isDynamic && !window.isSeekable))) {
//                seekTo(windowIndex - 1, C.TIME_UNSET);
//            } else {
//                seekTo(0);
//            }
//        }
    }

    @Override
    public void next() {
//        Timeline timeline = player.getCurrentTimeline();
//        if (timeline.isEmpty()) {
//            return;
//        }
//        int windowIndex = player.getCurrentWindowIndex();
//        if (windowIndex < timeline.getWindowCount() - 1) {
//            seekTo(windowIndex + 1, C.TIME_UNSET);
//        } else if (timeline.getWindow(windowIndex, window, false).isDynamic) {
//            seekTo(windowIndex, C.TIME_UNSET);
//        }
    }

    @Override
    public void stop() {

    }

    @Override
    public void rePlay() {

    }

    public void release() {
        if (url.equals(JCMediaManager.CURRENT_PLAYING_URL) &&
                (System.currentTimeMillis() - CLICK_QUIT_FULLSCREEN_TIME) > FULL_SCREEN_NORMAL_DELAY) {
            //在非全屏的情况下只能backPress()
            if (JCVideoPlayerManager.getSecondFloor() != null &&
                    JCVideoPlayerManager.getSecondFloor().currentScreen == SCREEN_WINDOW_FULLSCREEN) {//点击全屏
            } else if (JCVideoPlayerManager.getSecondFloor() == null && JCVideoPlayerManager.getFirstFloor() != null &&
                    JCVideoPlayerManager.getFirstFloor().currentScreen == SCREEN_WINDOW_FULLSCREEN) {//直接全屏
            } else {
                Log.d(TAG, "release [" + this.hashCode() + "]");
                releaseAllVideos();
            }
        }
    }

    //isCurrentJcvd and isCurrenPlayUrl should be two logic methods,isCurrentJcvd is for different jcvd with same
    //url when fullscreen or tiny screen. isCurrenPlayUrl is to find where is myself when back from tiny screen.
    //Sometimes they are overlap.
    public boolean isCurrentJcvd() {//虽然看这个函数很不爽，但是干不掉
        return JCVideoPlayerManager.getCurrentJcvd() != null
                && JCVideoPlayerManager.getCurrentJcvd() == this;
    }

//    public boolean isCurrenPlayingUrl() {
//        return url.equals(JCMediaManager.CURRENT_PLAYING_URL);
//    }

    public static void releaseAllVideos() {
        if ((System.currentTimeMillis() - CLICK_QUIT_FULLSCREEN_TIME) > FULL_SCREEN_NORMAL_DELAY) {
            Log.d(TAG, "releaseAllVideos");
            JCVideoPlayerManager.completeAll();
            JCMediaManager.instance().releaseMediaPlayer();
        }
    }


    public static void startFullscreen(Context context, Class _class, String url, Object... objects) {
        hideSupportActionBar(context);
        VideoUtils.getActivity(context).setRequestedOrientation(FULLSCREEN_ORIENTATION);
        ViewGroup vp = (ViewGroup) (VideoUtils.scanForActivity(context))//.getWindow().getDecorView();
                .findViewById(Window.ID_ANDROID_CONTENT);
        View old = vp.findViewById(JCVideoPlayer.FULLSCREEN_ID);
        if (old != null) {
            vp.removeView(old);
        }
        try {
            Constructor<JCVideoPlayer> constructor = _class.getConstructor(Context.class);
            final JCVideoPlayer jcVideoPlayer = constructor.newInstance(context);
            jcVideoPlayer.setId(JCVideoPlayer.FULLSCREEN_ID);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            vp.addView(jcVideoPlayer, lp);
//            final Animation ra = AnimationUtils.loadAnimation(context, R.anim.start_fullscreen);
//            jcVideoPlayer.setAnimation(ra);
            jcVideoPlayer.setUp(url, JCVideoPlayerStandard.SCREEN_WINDOW_FULLSCREEN, objects);
            CLICK_QUIT_FULLSCREEN_TIME = System.currentTimeMillis();
            jcVideoPlayer.startButton.performClick();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void hideSupportActionBar(Context context) {
        if (ACTION_BAR_EXIST) {
            ActionBar ab = VideoUtils.getActivity(context).getSupportActionBar();
            if (ab != null) {
                ab.setShowHideAnimationEnabled(false);
                ab.hide();
            }
        }
        if (TOOL_BAR_EXIST) {
            VideoUtils.getActivity(context).getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    public static void showSupportActionBar(Context context) {
        if (ACTION_BAR_EXIST) {
            ActionBar ab = VideoUtils.getActivity(context).getSupportActionBar();
            if (ab != null) {
                ab.setShowHideAnimationEnabled(false);
                ab.show();
            }
        }
        if (TOOL_BAR_EXIST) {
            VideoUtils.getActivity(context).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    public static class JCAutoFullscreenListener implements SensorEventListener {
        @Override
        public void onSensorChanged(SensorEvent event) {//可以得到传感器实时测量出来的变化值
            final float x = event.values[SensorManager.DATA_X];
            float y = event.values[SensorManager.DATA_Y];
            float z = event.values[SensorManager.DATA_Z];
            //过滤掉用力过猛会有一个反向的大数值
            if (((x > -15 && x < -10) || (x < 15 && x > 10)) && Math.abs(y) < 1.5) {
                if ((System.currentTimeMillis() - lastAutoFullscreenTime) > 2000) {
                    if (JCVideoPlayerManager.getCurrentJcvd() != null) {
                        JCVideoPlayerManager.getCurrentJcvd().autoFullscreen(x);
                    }
                    lastAutoFullscreenTime = System.currentTimeMillis();
                }
            }
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
                                   String seekTime, int seekTimePosition,
                                   String totalTime, int totalTimeDuration) {
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
