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
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Util;
import com.naivor.player.R;
import com.naivor.player.constant.VideoState;
import com.naivor.player.controll.PlayController;
import com.naivor.player.controll.PositionController;

import java.util.Arrays;
import java.util.Formatter;
import java.util.Locale;

import timber.log.Timber;


/**
 * 控制栏
 */
public class ControlView extends FrameLayout implements PlayController, PositionController, View.OnTouchListener {

    public static final int DEFAULT_FAST_FORWARD_MS = 15000;
    public static final int DEFAULT_REWIND_MS = 5000;
    public static final int DEFAULT_SHOW_TIMEOUT_MS = 5000;


    public static final int MAX_WINDOWS_FOR_MULTI_WINDOW_TIME_BAR = 100;

    private static final long MAX_POSITION_FOR_SEEK_TO_PREVIOUS = 3000;


    private final ComponentListener componentListener;

    private ControlViewHolder viewHolder;

    private final StringBuilder formatBuilder;
    private final Formatter formatter;
    private final Timeline.Period period;
    private final Timeline.Window window;

    private ExoPlayer player;
    private OnControllViewListener onControllViewListener;

    private boolean isAttachedToWindow;
    private boolean showMultiWindowTimeBar;
    private boolean multiWindowTimeBar;
    private boolean scrubbing;
    private int rewindMs;
    private int fastForwardMs;
    private int showTimeoutMs;
    private long hideAtMs;
    private long[] adBreakTimesMs;

    private final Runnable updateProgressAction = new Runnable() {
        @Override
        public void run() {
            updateProgress();
        }
    };

    private final Runnable hideAction = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    private ControlTouchProsser controlTouchProsser;


    public ControlView(Context context) {
        this(context, null);
    }

    public ControlView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ControlView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        rewindMs = DEFAULT_REWIND_MS;
        fastForwardMs = DEFAULT_FAST_FORWARD_MS;
        showTimeoutMs = DEFAULT_SHOW_TIMEOUT_MS;

        period = new Timeline.Period();
        window = new Timeline.Window();
        formatBuilder = new StringBuilder();
        formatter = new Formatter(formatBuilder, Locale.getDefault());
        adBreakTimesMs = new long[0];
        componentListener = new ComponentListener();

        View.inflate(context, getLayoutRes(), this);
        setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);

        viewHolder = new ControlViewHolder(this);

        controlTouchProsser = new ControlTouchProsser(context);

        if (viewHolder.timeBar != null) {
            viewHolder.timeBar.setOnSeekBarChangeListener(componentListener);
        }

        if (viewHolder.playButton != null) {
            viewHolder.playButton.setOnClickListener(componentListener);
        }

        if (viewHolder.fullScreenButton != null) {
            viewHolder.fullScreenButton.setOnClickListener(componentListener);
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
     * @return
     */
    public ExoPlayer getPlayer() {
        return player;
    }

    /**
     * @param player
     */
    public void setPlayer(ExoPlayer player) {
        if (this.player == player) {
            return;
        }
        if (this.player != null) {
            this.player.removeListener(componentListener);
        }
        this.player = player;
        if (player != null) {
            player.addListener(componentListener);
        }
        updateAll();
    }


    public void setShowMultiWindowTimeBar(boolean showMultiWindowTimeBar) {
        this.showMultiWindowTimeBar = showMultiWindowTimeBar;
        updateTimeBarMode();
    }

    public void setOnControllViewListener(OnControllViewListener onControllViewListener) {
        this.onControllViewListener = onControllViewListener;
    }

    public void setRewindIncrementMs(int rewindMs) {
        this.rewindMs = rewindMs;
        updateNavigation();
    }


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
     * 显示缓冲
     *
     * @param isThumb
     */
    public void showBuffering(boolean isThumb) {
        if (viewHolder != null) {
            viewHolder.showThumb(isThumb);
        }
    }

    /**
     * 显示播放错误
     */
    public void onError() {
        Timber.d("播放出错");

        stop();

        if (viewHolder != null) {
            viewHolder.showPlayButton(R.drawable.jc_click_error_selector);
        }
    }

    /**
     * 播放完成
     */
    public void onComplete() {
        Timber.d("播放完成");

        stop();

        if (viewHolder != null) {
            viewHolder.showPlayButton(R.drawable.jc_click_play_selector);
        }
    }

    /**
     * 隐藏控制界面
     */
    public void hide() {
        Timber.d("隐藏控制界面");

        if (viewHolder.isShown()) {
            boolean hidePlayButton = player.getPlayWhenReady() && !shouldHidePlayBtn();
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
    public boolean isVisible() {
        if (viewHolder.buttomLayout != null) {
            return viewHolder.buttomLayout.getVisibility() == VISIBLE;
        }

        return false;
    }

    /**
     * 改变全屏按钮状态
     *
     * @param full
     */
    public void setFullBtnState(boolean full) {
        if (viewHolder != null) {
            if (full) {
                viewHolder.showFullScreenButton(R.drawable.jc_shrink);
            } else {
                viewHolder.showFullScreenButton(R.drawable.jc_enlarge);

            }
        }
    }

    /**
     * 控制栏超时隐藏
     */
    private void hideAfterTimeout() {
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
     * 全部更新
     */
    private void updateAll() {
        Timber.d("更新控制界面");

        updatePlayPauseButton();
        updateNavigation();
        updateProgress();
    }

    /**
     * 播放按钮状态更新
     */
    private void updatePlayPauseButton() {

        Timber.d("更新播放按钮");

        if (!isVisible() || !isAttachedToWindow) {
            return;
        }

        if (viewHolder.playButton != null) {

            if (player != null && player.getPlayWhenReady()
                    && player.getPlaybackState() == ExoPlayer.STATE_READY) {
                viewHolder.playButton.setImageResource(R.drawable.jc_click_pause_selector);
            } else {
                viewHolder.playButton.setImageResource(R.drawable.jc_click_play_selector);
            }
        }

    }

    /**
     * 是否播放错误
     *
     * @return
     */
    private boolean shouldHidePlayBtn() {
        boolean hide = false;

        if (onControllViewListener != null) {
            hide = onControllViewListener.getCurrentState() != VideoState.CURRENT_STATE_PLAYING;
        }

        return hide;
    }

    /**
     * 更新导航（广告？）
     */
    private void updateNavigation() {
        if (!isVisible() || !isAttachedToWindow) {
            return;
        }
        Timeline timeline = player != null ? player.getCurrentTimeline() : null;
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
    private void updateTimeBarMode() {
        if (player == null) {
            return;
        }
        multiWindowTimeBar = showMultiWindowTimeBar
                && canShowMultiWindowTimeBar(player.getCurrentTimeline(), period);
    }

    /**
     * 更新播放进度
     */
    private void updateProgress() {
        if (!isVisible() || !isAttachedToWindow) {
            return;
        }

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
            int progress = (int) (position * 100 / duration);
            int bufferedProgress = (int) (bufferedPosition * 100 / duration);

            Timber.v("更新进度：%s,缓冲进度：%s", progress, bufferedProgress);

            viewHolder.timeBar.setProgress(progress);
            viewHolder.timeBar.setSecondaryProgress(bufferedProgress);

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

    @TargetApi(11)
    private void setViewAlphaV11(View view, float alpha) {
        view.setAlpha(alpha);
    }

    @Override
    public void start() {
        player.setPlayWhenReady(true);

        if (onControllViewListener != null) {
            onControllViewListener.prepareSourceData();
        }
    }

    @Override
    public void pause() {
        player.setPlayWhenReady(false);
    }

    @Override
    public void resume() {
        player.setPlayWhenReady(true);
    }

    @Override
    public void previous() {
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

    @Override
    public void next() {
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

    @Override
    public void stop() {

        player.stop();
    }

    @Override
    public void rePlay() {
        stop();
        start();
    }

    private void rewind() {
        backward(rewindMs);
    }

    private void fastForward() {
        fastward(fastForwardMs);
    }

    @Override
    public void seekTo(long positionMs) {
        seekTo(player.getCurrentWindowIndex(), positionMs);
    }

    @Override
    public void fastward(long millisecond) {
        if (millisecond <= 0) {
            return;
        }
        seekTo(Math.min(player.getCurrentPosition() + millisecond, player.getDuration()));
    }

    @Override
    public void backward(long millisecond) {
        if (millisecond <= 0) {
            return;
        }
        seekTo(Math.max(player.getCurrentPosition() - millisecond, 0));
    }

    @Override
    public long getCurrentDuration() {
        return player.getCurrentPosition();
    }

    @Override
    public long getTotalDuration() {
        return player.getDuration();
    }

    /**
     * 快进
     *
     * @param windowIndex
     * @param positionMs
     */
    private void seekTo(int windowIndex, long positionMs) {
        player.seekTo(windowIndex, positionMs);
    }

    /**
     * 快进
     *
     * @param timebarPositionMs
     */
    private void seekToTimebarPosition(long timebarPositionMs) {
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

    @SuppressLint("InlinedApi")
    private static boolean isHandledMediaKey(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD
                || keyCode == KeyEvent.KEYCODE_MEDIA_REWIND
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY
                || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE
                || keyCode == KeyEvent.KEYCODE_MEDIA_NEXT
                || keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS;
    }


    /**
     * @param timeline
     * @param period
     * @return
     */
    private static boolean canShowMultiWindowTimeBar(Timeline timeline, Timeline.Period period) {
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

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        boolean processTouchEvent = controlTouchProsser.processTouchEvent(motionEvent, onControllViewListener, scrubbing);
        if (!processTouchEvent) {
            show();
        }
        return processTouchEvent;
    }

    /**
     * 监听器，监听播放事件，点击事件，拖动事件
     */
    private final class ComponentListener implements ExoPlayer.EventListener, SeekBar.OnSeekBarChangeListener,
            OnClickListener {

        void setAdBreakTimesMs(@Nullable long[] adBreakTimesMs, int adBreakCount) {

        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {

            switch (playbackState) {
                case ExoPlayer.STATE_IDLE:
                    return;
                case ExoPlayer.STATE_BUFFERING:
                    showBuffering(true);
                    break;
                case ExoPlayer.STATE_READY:
                    showBuffering(false);
                    break;
                case ExoPlayer.STATE_ENDED:
                    onComplete();
                    break;
            }

            updatePlayPauseButton();
            updateProgress();
        }

        @Override
        public void onPositionDiscontinuity() {
            updateNavigation();
            updateProgress();
        }

        @Override
        public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
            // Do nothing.
        }

        @Override
        public void onTimelineChanged(Timeline timeline, Object manifest) {
            updateNavigation();
            updateTimeBarMode();
            updateProgress();
        }

        @Override
        public void onLoadingChanged(boolean isLoading) {
            // Do nothing.
        }

        @Override
        public void onTracksChanged(TrackGroupArray tracks, TrackSelectionArray selections) {
            // Do nothing.
        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            onError();
        }

        @Override
        public void onClick(View view) {
            boolean isPause = false;
            boolean isComplete = false;

            if (onControllViewListener != null) {
                onControllViewListener.onclick(view);

                int state = onControllViewListener.getCurrentState();
                isPause = state == VideoState.CURRENT_STATE_PAUSE;
                isComplete = state == VideoState.CURRENT_STATE_COMPLETE;
            }

            if (player != null) {
                if (viewHolder.playButton == view) {
                    if (isComplete) {
                        rePlay();
                    } else if (player.getPlayWhenReady()) {
                        pause();
                    } else {
                        if (isPause) {
                            view.setVisibility(GONE);
                        }

                        resume();
                    }

                } else if (viewHolder.fullScreenButton == view) {
                    if (onControllViewListener != null) {
                        onControllViewListener.onFullScreenClick();
                    }
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

        private long getPositionTimeMs(int progress) {
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
