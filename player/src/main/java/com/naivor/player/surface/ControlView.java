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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.SystemClock;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.SeekBar;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Util;
import com.naivor.player.R;
import com.naivor.player.constant.ScreenState;
import com.naivor.player.constant.VideoState;
import com.naivor.player.controll.PlayController;
import com.naivor.player.controll.PositionController;

import java.util.Arrays;
import java.util.Formatter;
import java.util.Locale;

import lombok.NonNull;
import timber.log.Timber;


/**
 * 控制栏
 */
public class ControlView extends FrameLayout implements PlayController, PositionController, View.OnTouchListener {

    public static final int DEFAULT_FAST_FORWARD_MS = 5000;  //默认快进5秒
    public static final int DEFAULT_REWIND_MS = 5000;  //默认快退5秒

    public static final int DEFAULT_SHOW_TIMEOUT_MS = 3000;  //控制栏默认3秒无操作隐藏

    public static final int MAX_WINDOWS_FOR_MULTI_WINDOW_TIME_BAR = 100;
    public static final long MAX_POSITION_FOR_SEEK_TO_PREVIOUS = 3000;

    protected ExoPlayer player;

    protected ComponentListener componentListener;  //监听器

    protected ControlViewHolder viewHolder;  //控制栏按钮容器
    protected OnControllViewListener onControllViewListener;  //控制栏监听器

    protected StringBuilder formatBuilder;
    protected Formatter formatter;
    protected Timeline.Period period;
    protected Timeline.Window window;

    protected boolean isAttachedToWindow;
    protected boolean showMultiWindowTimeBar;
    protected boolean multiWindowTimeBar;
    protected boolean scrubbing;
    protected int rewindMs;
    protected int fastForwardMs;
    protected int showTimeoutMs;
    protected long hideAtMs;
    protected long[] adBreakTimesMs;

    //更新播放进度
    protected final Runnable updateProgressAction = new Runnable() {
        @Override
        public void run() {
            updateProgress();
        }
    };

    //隐藏控制栏
    protected final Runnable hideAction = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    //控制栏触碰事件处理器
    protected ControlTouchProcessor controlTouchProcessor;


    public ControlView(Context context) {
        this(context, null);
    }

    public ControlView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ControlView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        View.inflate(context, getLayoutRes(), this);
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);

        viewHolder = new ControlViewHolder(this);


        rewindMs = DEFAULT_REWIND_MS;
        fastForwardMs = DEFAULT_FAST_FORWARD_MS;
        showTimeoutMs = DEFAULT_SHOW_TIMEOUT_MS;

        period = new Timeline.Period();
        window = new Timeline.Window();
        formatBuilder = new StringBuilder();
        formatter = new Formatter(formatBuilder, Locale.getDefault());
        adBreakTimesMs = new long[0];
        componentListener = new ComponentListener();

        controlTouchProcessor = new ControlTouchProcessor(context);

        if (viewHolder.timeBar != null) {
            viewHolder.timeBar.setOnSeekBarChangeListener(componentListener);
        }

        if (viewHolder.playBtn != null) {
            viewHolder.playBtn.setOnClickListener(componentListener);
        }

        if (viewHolder.fullScreenBtn != null) {
            viewHolder.fullScreenBtn.setOnClickListener(componentListener);
        }

        if (viewHolder.backBtn != null) {
            viewHolder.backBtn.setOnClickListener(componentListener);
        }

        if (viewHolder.tinyExitBtn != null) {
            viewHolder.tinyExitBtn.setOnClickListener(componentListener);
        }

        if (viewHolder.tinyCloseBtn != null) {
            viewHolder.tinyCloseBtn.setOnClickListener(componentListener);
        }

        setOnTouchListener(this);
    }

    /**
     * 获取布局Id
     *
     * @return
     */
    @LayoutRes
    public int getLayoutRes() {
        return R.layout.control_view_base;
    }


    /**
     * 绑定播放器
     *
     * @param player
     */
    public void setPlayer(ExoPlayer player) {
        if (this.player == player) {
            return;
        }

        this.player = player;

        updateAll();
    }


    @Override
    public boolean isShown() {
        return viewHolder.isShown();
    }

    /**
     * 显示控制界面
     */
    public void show() {
        Timber.d("显示控制界面");

        if (!viewHolder.isShown()) {
            viewHolder.show();
            if (onControllViewListener != null) {
                onControllViewListener.onVisibilityChange(viewHolder.buttomLayout.getVisibility());
            }
            updateAll();
        }

        // 超时隐藏
        hideAfterTimeout();
    }

    /**
     * 设置播放视频的标题
     *
     * @param videoTitle
     */
    public void setVideoTitle(@NonNull String videoTitle) {
        if (viewHolder.videoTitle != null) {
            viewHolder.videoTitle.setText(videoTitle);
        }
    }


    /**
     * 隐藏控制界面
     */
    public void hide() {
        Timber.d("隐藏控制界面");

        if (viewHolder.isShown()) {
            boolean hidePlayButton = false;
            if (player != null) {
                hidePlayButton = player.getPlayWhenReady() && !shouldHidePlayBtn();
            }
            viewHolder.hide(hidePlayButton);
            if (onControllViewListener != null) {
                onControllViewListener.onVisibilityChange(viewHolder.buttomLayout.getVisibility());
            }
            removeCallbacks(updateProgressAction);
            removeCallbacks(hideAction);
            hideAtMs = C.TIME_UNSET;
        }
    }


    /**
     * 是否可见
     *
     * @return
     */
    public boolean isBottomVisible() {
        if (viewHolder.buttomLayout != null) {
            return viewHolder.buttomLayout.getVisibility() == VISIBLE;
        }

        return false;
    }

    /**
     * 更新屏幕状态
     *
     * @param state
     */
    public void updateScreenState(@ScreenState.ScreenStateValue int state) {
        Timber.d("更新屏幕状态：%s", ScreenState.getScreenStateName(state));

        if (viewHolder != null) {
            viewHolder.updateScreenState(state);
        }
    }

    /**
     * 更新播放状态
     *
     * @param state
     */
    public void updateVideoState(@VideoState.VideoStateValue int state) {
        Timber.d("更新播放状态：%s", VideoState.getVideoStateName(state));

        if (viewHolder != null) {
            viewHolder.updateVideoState(state);
        }
    }

    /**
     * 控制栏超时隐藏
     */
    protected void hideAfterTimeout() {
        Timber.d("超时隐藏");

        removeCallbacks(hideAction);
        if (showTimeoutMs > 0) {
            hideAtMs = SystemClock.uptimeMillis() + showTimeoutMs;
            if (isAttachedToWindow) {
                postDelayed(hideAction, showTimeoutMs);
            }
        } else {
            hideAtMs = C.TIME_UNSET;
        }
    }


    /**
     * 是否播放错误
     *
     * @return
     */
    protected boolean shouldHidePlayBtn() {
        boolean hide = false;

        if (onControllViewListener != null) {
            hide = onControllViewListener.getVideoState() != VideoState.CURRENT_STATE_PLAYING;
        }

        return hide;
    }

    /**
     * 全部更新
     */
    public void updateAll() {
        Timber.d("更新控制界面");

        updateNavigation();
        updateProgress();
    }


    /**
     * 更新导航（广告？）
     */
    public void updateNavigation() {
        if (!isBottomVisible() || !isAttachedToWindow || player == null) {
            return;
        }
        Timeline timeline = player.getCurrentTimeline();
        boolean haveNonEmptyTimeline = timeline != null && !timeline.isEmpty();
        boolean isSeekable = false;
        if (haveNonEmptyTimeline) {
            int windowIndex = player.getCurrentWindowIndex();
            timeline.getWindow(windowIndex, window);
            isSeekable = window.isSeekable;
            if (timeline.getPeriod(player.getCurrentPeriodIndex(), period).isAd) {
                // Always hide player controls during ads.
                hide();
            }
        }

        if (viewHolder.timeBar != null) {
            viewHolder.timeBar.setEnabled(isSeekable);
        }
    }

    /**
     * 更新进度条模式
     */
    public void updateTimeBarMode() {
        if (player == null) {
            return;
        }
        multiWindowTimeBar = showMultiWindowTimeBar
                && canShowMultiWindowTimeBar(player.getCurrentTimeline(), period);
    }

    /**
     * 更新播放进度
     */
    public void updateProgress() {

        long position = 0;
        long bufferedPosition = 0;
        long duration = 0;
        if (player != null) {
            if (multiWindowTimeBar) {
                Timeline timeline = player.getCurrentTimeline();
                int windowCount = timeline.getWindowCount();
                int periodIndex = player.getCurrentPeriodIndex();
                long positionUs = 0;
                long bufferedPositionUs = 0;
                long durationUs = 0;
                boolean isInAdBreak = false;
                boolean isPlayingAd = false;
                int adBreakCount = 0;
                for (int i = 0; i < windowCount; i++) {
                    timeline.getWindow(i, window);
                    for (int j = window.firstPeriodIndex; j <= window.lastPeriodIndex; j++) {
                        if (timeline.getPeriod(j, period).isAd) {
                            isPlayingAd |= j == periodIndex;
                            if (!isInAdBreak) {
                                isInAdBreak = true;
                                if (adBreakCount == adBreakTimesMs.length) {
                                    adBreakTimesMs = Arrays.copyOf(adBreakTimesMs,
                                            adBreakTimesMs.length == 0 ? 1 : adBreakTimesMs.length * 2);
                                }
                                adBreakTimesMs[adBreakCount++] = C.usToMs(durationUs);
                            }
                        } else {
                            isInAdBreak = false;
                            long periodDurationUs = period.getDurationUs();
                            Assertions.checkState(periodDurationUs != C.TIME_UNSET);
                            long periodDurationInWindowUs = periodDurationUs;
                            if (j == window.firstPeriodIndex) {
                                periodDurationInWindowUs -= window.positionInFirstPeriodUs;
                            }
                            if (i < periodIndex) {
                                positionUs += periodDurationInWindowUs;
                                bufferedPositionUs += periodDurationInWindowUs;
                            }
                            durationUs += periodDurationInWindowUs;
                        }
                    }
                }
                position = C.usToMs(positionUs);
                bufferedPosition = C.usToMs(bufferedPositionUs);
                duration = C.usToMs(durationUs);
                if (!isPlayingAd) {
                    position += player.getCurrentPosition();
                    bufferedPosition += player.getBufferedPosition();
                }
                if (componentListener != null) {
                    componentListener.setAdBreakTimesMs(adBreakTimesMs, adBreakCount);
                }
            } else {
                position = player.getCurrentPosition();
                bufferedPosition = player.getBufferedPosition();
                duration = player.getDuration();
            }
        }
        if (viewHolder.durationView != null) {
            viewHolder.durationView.setText(Util.getStringForTime(formatBuilder, formatter, duration));
        }
        if (viewHolder.positionView != null && !scrubbing) {
            viewHolder.positionView.setText(Util.getStringForTime(formatBuilder, formatter, position));
        }
        if (viewHolder.timeBar != null && !scrubbing) {
            int progress = duration == 0 ? 0 : (int) (position * 100 / duration);
            int bufferedProgress = duration == 0 ? 0 : (int) (bufferedPosition * 100 / duration);

            Timber.v("更新进度：%s,缓冲进度：%s", progress, bufferedProgress);

            if (isBottomVisible() && isAttachedToWindow) {
                viewHolder.timeBar.setProgress(progress);
                viewHolder.timeBar.setSecondaryProgress(bufferedProgress);
            }

            if (onControllViewListener != null) {
                onControllViewListener.onProgress(progress, bufferedProgress);
            }
        }

        // Cancel any pending updates and schedule a new one if necessary.
        removeCallbacks(updateProgressAction);
        int playbackState = player == null ? ExoPlayer.STATE_IDLE : player.getPlaybackState();
        if (playbackState != ExoPlayer.STATE_IDLE && playbackState != ExoPlayer.STATE_ENDED) {
            long delayMs;
            if (player.getPlayWhenReady() && playbackState == ExoPlayer.STATE_READY) {
                delayMs = 1000 - (position % 1000);
                if (delayMs < 200) {
                    delayMs += 1000;
                }
            } else {
                delayMs = 1000;
            }
            postDelayed(updateProgressAction, delayMs);
        }
    }


    /**
     * 按钮显示半透明的不可点击或不透明的可点击状态
     *
     * @param enabled
     * @param view
     */
    public void setButtonEnabled(boolean enabled, View view) {
        if (view == null) {
            return;
        }
        view.setEnabled(enabled);
        if (Util.SDK_INT >= 11) {
            setViewAlphaV11(view, enabled ? 1f : 0.3f);
            view.setVisibility(VISIBLE);
        } else {
            view.setVisibility(enabled ? VISIBLE : INVISIBLE);
        }
    }

    /**
     * @param view
     * @param alpha
     */
    @TargetApi(11)
    protected void setViewAlphaV11(View view, float alpha) {
        view.setAlpha(alpha);
    }

    @Override
    public void start() {

        if (onControllViewListener != null) {
            onControllViewListener.requestPrepareSourceData();
        }

        if (player != null) {
            player.setPlayWhenReady(true);
        }
    }

    @Override
    public void pause() {
        if (player != null) {
            player.setPlayWhenReady(false);
        }
    }

    @Override
    public void resume() {
        if (player != null) {
            player.setPlayWhenReady(true);
        }
    }

    @Override
    public void previous() {
        if (player != null) {
            Timeline timeline = player.getCurrentTimeline();
            if (timeline.isEmpty()) {
                return;
            }
            int windowIndex = player.getCurrentWindowIndex();
            timeline.getWindow(windowIndex, window);
            if (windowIndex > 0 && (player.getCurrentPosition() <= MAX_POSITION_FOR_SEEK_TO_PREVIOUS
                    || (window.isDynamic && !window.isSeekable))) {
                seekTo(windowIndex - 1, C.TIME_UNSET);
            } else {
                seekTo(0);
            }
        }
    }

    @Override
    public void next() {
        if (player != null) {
            Timeline timeline = player.getCurrentTimeline();
            if (timeline.isEmpty()) {
                return;
            }
            int windowIndex = player.getCurrentWindowIndex();
            if (windowIndex < timeline.getWindowCount() - 1) {
                seekTo(windowIndex + 1, C.TIME_UNSET);
            } else if (timeline.getWindow(windowIndex, window, false).isDynamic) {
                seekTo(windowIndex, C.TIME_UNSET);
            }
        }
    }

    @Override
    public void stop() {
        if (player != null) {
            player.stop();
        }
    }

    @Override
    public void rePlay() {
        stop();
        start();
    }

    /**
     * 快退
     */
    public void rewind() {
        backward(rewindMs);
    }

    /**
     * 快进
     */
    public void fastForward() {
        fastward(fastForwardMs);
    }

    @Override
    public void seekTo(long positionMs) {
        if (player != null) {
            seekTo(player.getCurrentWindowIndex(), positionMs);
        }
    }

    @Override
    public void fastward(long millisecond) {
        if (player != null) {
            if (millisecond <= 0) {
                return;
            }
            seekTo(Math.min(player.getCurrentPosition() + millisecond, player.getDuration()));
        }
    }

    @Override
    public void backward(long millisecond) {
        if (player != null) {
            if (millisecond <= 0) {
                return;
            }
            seekTo(Math.max(player.getCurrentPosition() - millisecond, 0));
        }
    }

    @Override
    public long getCurrentDuration() {
        if (player != null) {
            return player.getCurrentPosition();
        }
        return 0L;
    }

    @Override
    public long getTotalDuration() {
        if (player != null) {
            return player.getDuration();
        }
        return 0L;
    }

    /**
     * 快进
     *
     * @param windowIndex
     * @param positionMs
     */
    protected void seekTo(int windowIndex, long positionMs) {
        if (player != null) {
            player.seekTo(windowIndex, positionMs);
        }
    }

    /**
     * 快进
     *
     * @param timebarPositionMs
     */
    protected void seekToTimebarPosition(long timebarPositionMs) {
        if (multiWindowTimeBar) {
            Timeline timeline = player.getCurrentTimeline();
            int windowCount = timeline.getWindowCount();
            long remainingMs = timebarPositionMs;
            for (int i = 0; i < windowCount; i++) {
                timeline.getWindow(i, window);
                for (int j = window.firstPeriodIndex; j <= window.lastPeriodIndex; j++) {
                    if (!timeline.getPeriod(j, period).isAd) {
                        long periodDurationMs = period.getDurationMs();
                        if (periodDurationMs == C.TIME_UNSET) {
                            // Should never happen as canShowMultiWindowTimeBar is true.
                            throw new IllegalStateException();
                        }
                        if (j == window.firstPeriodIndex) {
                            periodDurationMs -= window.getPositionInFirstPeriodMs();
                        }
                        if (i == windowCount - 1 && j == window.lastPeriodIndex
                                && remainingMs >= periodDurationMs) {
                            // Seeking past the end of the last window should seek to the end of the timeline.
                            seekTo(i, window.getDurationMs());
                            return;
                        }
                        if (remainingMs < periodDurationMs) {
                            seekTo(i, period.getPositionInWindowMs() + remainingMs);
                            return;
                        }
                        remainingMs -= periodDurationMs;
                    }
                }
            }
        } else {
            Timber.d("从 %s 开始播放", timebarPositionMs);

            seekTo(timebarPositionMs);
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        isAttachedToWindow = true;

        if (hideAtMs != C.TIME_UNSET) {
            long delayMs = hideAtMs - SystemClock.uptimeMillis();
            if (delayMs <= 0) {
                hide();
            } else {
                postDelayed(hideAction, delayMs);
            }
        }
        updateAll();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        isAttachedToWindow = false;
        removeCallbacks(updateProgressAction);
        removeCallbacks(hideAction);
    }


    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        boolean handled = dispatchMediaKeyEvent(event) || super.dispatchKeyEvent(event);

        Timber.d("收到触摸事件");

        if (handled) {
            show();
        }
        return handled;
    }


    /**
     * 多媒体按键事件处理
     *
     * @param event
     * @return
     */
    public boolean dispatchMediaKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (player == null || !isHandledMediaKey(keyCode)) {
            return false;
        }
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                    fastForward();
                    break;
                case KeyEvent.KEYCODE_MEDIA_REWIND:
                    rewind();
                    break;
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    player.setPlayWhenReady(!player.getPlayWhenReady());
                    break;
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                    resume();
                    break;
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    pause();
                    break;
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    next();
                    break;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    previous();
                    break;
                default:
                    break;
            }
        }
        show();
        return true;
    }

    /**
     * 是否是媒体按键
     *
     * @param keyCode
     * @return
     */
    @SuppressLint("InlinedApi")
    protected static boolean isHandledMediaKey(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD
                || keyCode == KeyEvent.KEYCODE_MEDIA_REWIND
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY
                || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE
                || keyCode == KeyEvent.KEYCODE_MEDIA_NEXT
                || keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS;
    }


    /**
     * @param showMultiWindowTimeBar
     */
    public void setShowMultiWindowTimeBar(boolean showMultiWindowTimeBar) {
        this.showMultiWindowTimeBar = showMultiWindowTimeBar;
        updateTimeBarMode();
    }

    public void setOnControllViewListener(OnControllViewListener onControllViewListener) {
        this.onControllViewListener = onControllViewListener;
    }

    /**
     * @param rewindMs
     */
    public void setRewindIncrementMs(int rewindMs) {
        this.rewindMs = rewindMs;
        updateNavigation();
    }


    /**
     * @param fastForwardMs
     */
    public void setFastForwardIncrementMs(int fastForwardMs) {
        this.fastForwardMs = fastForwardMs;
        updateNavigation();
    }


    public int getShowTimeoutMs() {
        return showTimeoutMs;
    }


    public void setShowTimeoutMs(int showTimeoutMs) {
        this.showTimeoutMs = showTimeoutMs;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        boolean processTouchEvent = controlTouchProcessor.processTouchEvent(motionEvent, onControllViewListener, scrubbing);
        if (!processTouchEvent) {
            show();
        }
        return processTouchEvent;
    }

    /**
     * @param timeline
     * @param period
     * @return
     */
    protected static boolean canShowMultiWindowTimeBar(Timeline timeline, Timeline.Period period) {
        if (timeline.getWindowCount() > MAX_WINDOWS_FOR_MULTI_WINDOW_TIME_BAR) {
            return false;
        }
        int periodCount = timeline.getPeriodCount();
        for (int i = 0; i < periodCount; i++) {
            timeline.getPeriod(i, period);
            if (!period.isAd && period.durationUs == C.TIME_UNSET) {
                return false;
            }
        }
        return true;
    }

    /**
     * 监听器，监听播放事件，点击事件，拖动事件
     */
    protected final class ComponentListener implements SeekBar.OnSeekBarChangeListener,
            OnClickListener {

        /**
         * @param adBreakTimesMs
         * @param adBreakCount
         */
        void setAdBreakTimesMs(@Nullable long[] adBreakTimesMs, int adBreakCount) {

        }

        @Override
        public void onClick(View view) {

            boolean isPause = false;
            boolean isComplete = false;
            boolean isOrigin = false;

            if (onControllViewListener != null) {
                onControllViewListener.onclick(view);

                int state = onControllViewListener.getVideoState();

                isPause = state == VideoState.CURRENT_STATE_PAUSE;
                isComplete = state == VideoState.CURRENT_STATE_COMPLETE;
                isOrigin = state == VideoState.CURRENT_STATE_ORIGIN;
            }

            if (viewHolder.playBtn == view) {
                if (isOrigin) {
                    start();
                } else if (isComplete) {
                    rePlay();
                } else if (player != null && player.getPlayWhenReady()) {
                    pause();
                } else if (isPause) {
                    resume();
                }

            } else if (viewHolder.fullScreenBtn == view) {
                if (onControllViewListener != null) {
                    onControllViewListener.onFullScreenClick();
                }
            }

            hideAfterTimeout();
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            if (viewHolder.positionView != null && b) {
                viewHolder.positionView.setText(Util.getStringForTime(formatBuilder, formatter, getPositionTimeMs(i)));
            }
        }

        /**
         * @param progress
         * @return
         */
        protected long getPositionTimeMs(int progress) {
            if (player != null) {
                return player.getDuration() * progress / 100;
            }

            return 0;
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            removeCallbacks(hideAction);
            scrubbing = true;
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            scrubbing = false;
            if (player != null) {
                int progress = seekBar.getProgress();
                long positionTimeMs = getPositionTimeMs(progress);
                seekToTimebarPosition(positionTimeMs);

                Timber.d("快进到：%s, 开始时间：%s", progress, positionTimeMs);
            }
            hideAfterTimeout();
        }
    }


}
