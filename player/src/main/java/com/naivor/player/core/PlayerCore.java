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
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.naivor.player.core.decorate.VideoLoadControl;
import com.naivor.player.core.decorate.VideoRenderersFactory;
import com.naivor.player.core.decorate.VideoTrackSelector;

import java.util.HashSet;
import java.util.Set;

import lombok.NonNull;
import timber.log.Timber;

/**
 * 播放器核心
 * <p>
 * Created by tianlai on 17-7-7.
 */
public final class PlayerCore {

    private Context context;

    //默认
    private VideoRenderersFactory videoRenderersFactory; //渲染工厂
    private VideoTrackSelector videoTrackSelector; //轨道选择器
    private VideoLoadControl videoLoadControll; //加载状态监听

    //自定义
    private RenderersFactory renderersFactory;  //渲染工厂
    private TrackSelector trackSelector; //轨道选择器
    private LoadControl loadControl;  //加载状态监听

    //播放器
    private SimpleExoPlayer player;
    //显示画面的View
    private View surfaceView;

    private MediaSource mediaSource;    //音频数据源
    private boolean haveResetPosition = true;   //是否重置播放位置
    private boolean haveResetState;   //是否重置状态

    //事件监听
    private ExoPlayer.EventListener eventListener;
    private SimpleExoPlayer.VideoListener videoListener;

    //单利
    private static PlayerCore playerCore;

    //list播放用到
    protected Set<OnListVideoPlayListener> listListeners;


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
        this.context = context.getApplicationContext();

        createPlayer(this.context);

        listListeners = new HashSet<>();
    }

    /**
     * 创建播放器
     *
     * @param context
     */
    private void createPlayer(@NonNull Context context) {
        videoRenderersFactory = new VideoRenderersFactory(context);
        videoRenderersFactory.setRenderersFactory(renderersFactory);

        videoTrackSelector = new VideoTrackSelector();
        videoTrackSelector.setTrackSelector(trackSelector);

        videoLoadControll = new VideoLoadControl();
        videoLoadControll.setLoadControl(loadControl);

        player = ExoPlayerFactory.newSimpleInstance(videoRenderersFactory, videoTrackSelector, videoLoadControll);
    }


    /**
     * 通知list监听器，监听新视频播放
     */
    public void notifyPlayNewVideoInList(@NonNull OnListVideoPlayListener listListener) {
        Timber.i("添加list监听器，监听新视频播放");

        for (OnListVideoPlayListener l : listListeners) {
            if (l != null && l != listListener) {
                l.onNewVideo();
                listListeners.remove(l);

                release();
            }
        }

        listListeners.add(listListener);

    }

    /**
     * 准备视频
     */
    public void prepare() {
        Timber.i("准备视频");

        if (player == null) {
            createPlayer(context);
        }

        if (surfaceView != null) {
            if (surfaceView instanceof TextureView) {
                player.setVideoTextureView((TextureView) surfaceView);
            } else if (surfaceView instanceof SurfaceView) {
                player.setVideoSurfaceView((SurfaceView) surfaceView);
            }
        } else {
            throw new NullPointerException("surfaceView can't  be  null");
        }

        if (eventListener != null) {
            player.addListener(eventListener);
        }

        if (videoListener != null) {
            player.setVideoListener(videoListener);
        }

        if (mediaSource != null) {
            player.prepare(mediaSource, haveResetPosition, haveResetState);
        } else {
            throw new NullPointerException("mediaSource can't  be  null,please give a mediaSource to play");
        }
    }

    /**
     * @return
     */
    public SimpleExoPlayer getPlayer() {

        if (player == null) {
            createPlayer(context);
        }

        return player;
    }

    /**
     * 设置监听器
     *
     * @param eventListener
     */
    public void setEventListener(ExoPlayer.EventListener eventListener) {
        if (player != null) {
            if (this.eventListener != null) {
                player.removeListener(this.eventListener);
            }

            if (eventListener != null) {
                player.addListener(eventListener);
            }
        }

        this.eventListener = eventListener;
    }

    /**
     * 设置监听器
     *
     * @param videoListener
     */
    public void setVideoListener(SimpleExoPlayer.VideoListener videoListener) {
        if (player != null) {
            if (this.videoListener != null) {
                player.clearVideoListener(this.videoListener);
            }

            if (eventListener != null) {
                player.setVideoListener(videoListener);
            }
        }

        this.videoListener = videoListener;
    }

    /**
     * 设置加载控制
     *
     * @param loadControl
     */
    public void setLoadControl(LoadControl loadControl) {
        if (videoLoadControll != null) {
            videoLoadControll.setLoadControl(loadControl);
        }

        this.loadControl = loadControl;
    }

    /**
     * 设置渲染工厂
     *
     * @param renderersFactory
     */
    public void setRenderersFactory(@NonNull RenderersFactory renderersFactory) {
        if (videoRenderersFactory != null) {
            videoRenderersFactory.setRenderersFactory(renderersFactory);
        }

        this.renderersFactory = renderersFactory;
    }

    /**
     * 设置轨道选择器
     *
     * @param trackSelector
     */
    public void setTrackSelector(@NonNull TrackSelector trackSelector) {
        if (videoTrackSelector != null) {
            videoTrackSelector.setTrackSelector(trackSelector);
        }

        this.trackSelector = trackSelector;
    }

    /**
     * 释放资源
     */
    public void release() {
        if (player != null) {
            player.release();
        }
        player = null;

        videoRenderersFactory = null;
        videoTrackSelector = null;
        videoLoadControll = null;

        renderersFactory = null;
        trackSelector = null;
        loadControl = null;

        eventListener = null;
        videoListener = null;

        surfaceView = null;
        mediaSource = null;

        if (listListeners != null) {
            listListeners.clear();
        }

    }

    /**
     * 释放资源
     */
    public static void releaseAll() {
        if (playerCore != null) {
            playerCore.release();
        }
    }

    public RenderersFactory getRenderersFactory() {
        return renderersFactory;
    }

    public TrackSelector getTrackSelector() {
        return trackSelector;
    }

    public void setPlayer(@NonNull SimpleExoPlayer player) {
        this.player = player;
    }

    public View getSurfaceView() {
        return surfaceView;
    }

    public void setSurfaceView(@NonNull View surfaceView) {
        this.surfaceView = surfaceView;
    }

    public MediaSource getMediaSource() {
        return mediaSource;
    }

    public void setMediaSource(@NonNull MediaSource mediaSource) {
        this.mediaSource = mediaSource;
    }

    public LoadControl getLoadControl() {
        return loadControl;
    }

    public ExoPlayer.EventListener getEventListener() {
        return eventListener;
    }

    public SimpleExoPlayer.VideoListener getVideoListener() {
        return videoListener;
    }


}
