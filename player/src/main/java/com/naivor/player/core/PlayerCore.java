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
package com.naivor.player.core;

import android.content.Context;
import android.graphics.Point;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;

import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.Allocator;

import lombok.NonNull;

import static android.content.ContentValues.TAG;

/**
 * 播放器核心
 * <p>
 * Created by tianlai on 17-7-7.
 */
public class PlayerCore implements ExoPlayer.EventListener, LoadControl {

    private Context context;

    private RenderersFactory renderersFactory;  //渲染工厂
    private TrackSelector trackSelector; //轨道选择器

    private SimpleExoPlayer player;

    private static PlayerCore playerCore;

    private View surfaceView;

    private static MediaSource mediaSource;    //音频数据源
    private boolean haveResetPosition = true;   //是否重置播放位置
    private boolean haveResetState;   //是否重置状态

    private int currentVideoWidth = 0;
    private int currentVideoHeight = 0;

    private static final int HANDLER_PREPARE = 0;
    private static final int HANDLER_RELEASE = 2;

    HandlerThread mMediaHandlerThread;
    MediaHandler mMediaHandler;
    Handler mainThreadHandler;

    /**
     * 单例
     *
     * @param context
     * @return
     */
    public static PlayerCore instance(@NonNull Context context) {
        if (playerCore == null) {
            synchronized (PlayerCore.class) {
                if (playerCore == null) {
                    playerCore = new PlayerCore(context);
                }
            }
        }

        return playerCore;
    }

    private PlayerCore(@NonNull Context context) {
        this.context = context;

        mMediaHandlerThread = new HandlerThread(TAG);
        mMediaHandlerThread.start();
        mMediaHandler = new MediaHandler((mMediaHandlerThread.getLooper()));
        mainThreadHandler = new Handler();

        renderersFactory = new DefaultRenderersFactory(context);
        trackSelector = new DefaultTrackSelector();
    }

    public Point getVideoSize() {
        if (currentVideoWidth != 0 && currentVideoHeight != 0) {
            return new Point(currentVideoWidth, currentVideoHeight);
        } else {
            return null;
        }
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object o) {

    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroupArray, TrackSelectionArray trackSelectionArray) {

    }

    @Override
    public void onLoadingChanged(boolean b) {

    }

    @Override
    public void onPlayerStateChanged(boolean b, int i) {

    }

    @Override
    public void onPlayerError(ExoPlaybackException e) {

    }

    @Override
    public void onPositionDiscontinuity() {

    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

    }

    @Override
    public void onPrepared() {

    }

    @Override
    public void onTracksSelected(Renderer[] renderers, TrackGroupArray trackGroupArray, TrackSelectionArray trackSelectionArray) {

    }

    @Override
    public void onStopped() {

    }

    @Override
    public void onReleased() {

    }

    @Override
    public Allocator getAllocator() {
        return null;
    }

    @Override
    public boolean shouldStartPlayback(long l, boolean b) {
        return false;
    }

    @Override
    public boolean shouldContinueLoading(long l) {
        return false;
    }

    public class MediaHandler extends Handler {
        public MediaHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case HANDLER_PREPARE:
                    initPlayer();
                    break;
                case HANDLER_RELEASE:
                    player.release();
                    break;
            }
        }

    }

    /**
     * 初始化播放器
     */
    private void initPlayer() {
        try {
            currentVideoWidth = 0;
            currentVideoHeight = 0;

            player = ExoPlayerFactory.newSimpleInstance(renderersFactory, trackSelector, this);

            if (surfaceView instanceof TextureView) {
                player.setVideoTextureView((TextureView) surfaceView);
            } else if (surfaceView instanceof SurfaceView) {
                player.setVideoSurfaceView((SurfaceView) surfaceView);
            }
            player.addListener(this);

            player.prepare(mediaSource, haveResetPosition, haveResetState);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 准备视频
     */
    public void prepare() {
        release();
        Message msg = new Message();
        msg.what = HANDLER_PREPARE;
        mMediaHandler.sendMessage(msg);
    }

    /**
     * 释放资源
     */
    public void release() {
        Message msg = new Message();
        msg.what = HANDLER_RELEASE;
        mMediaHandler.sendMessage(msg);
    }

    public RenderersFactory getRenderersFactory() {
        return renderersFactory;
    }

    public void setRenderersFactory(RenderersFactory renderersFactory) {
        this.renderersFactory = renderersFactory;
    }

    public TrackSelector getTrackSelector() {
        return trackSelector;
    }

    public void setTrackSelector(TrackSelector trackSelector) {
        this.trackSelector = trackSelector;
    }

    public SimpleExoPlayer getPlayer() {
        return player;
    }

    public void setPlayer(SimpleExoPlayer player) {
        this.player = player;
    }

    public View getSurfaceView() {
        return surfaceView;
    }

    public void setSurfaceView(View surfaceView) {
        this.surfaceView = surfaceView;
    }
}
