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

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;

import lombok.NonNull;

/**
 * 播放器核心
 * <p>
 * Created by tianlai on 17-7-7.
 */
public class PlayerCore {

    private Context context;

    private RenderersFactory renderersFactory;  //渲染工厂
    private TrackSelector trackSelector; //轨道选择器

    private SimpleExoPlayer player;

    private View surfaceView;

    private MediaSource mediaSource;    //音频数据源
    private boolean haveResetPosition = true;   //是否重置播放位置
    private boolean haveResetState;   //是否重置状态

    private LoadControl loadControl;  //加载状态监听

    private ExoPlayer.EventListener eventListener;  //事件监听


    private static PlayerCore playerCore;
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

        renderersFactory = new DefaultRenderersFactory(context);
        trackSelector = new DefaultTrackSelector();

        loadControl = new DefaultLoadControl();

        player = ExoPlayerFactory.newSimpleInstance(renderersFactory, trackSelector, loadControl);
    }


    /**
     * 准备视频
     */
    public void prepare() {
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

        if (mediaSource != null) {
            player.prepare(mediaSource, haveResetPosition, haveResetState);
        } else {
            throw new NullPointerException("mediaSource can't  be  null,please give a mediaSource to play");
        }
    }


    public RenderersFactory getRenderersFactory() {
        return renderersFactory;
    }

    public void setRenderersFactory(@NonNull RenderersFactory renderersFactory) {
        this.renderersFactory = renderersFactory;
    }

    public TrackSelector getTrackSelector() {
        return trackSelector;
    }

    public void setTrackSelector(@NonNull TrackSelector trackSelector) {
        this.trackSelector = trackSelector;
    }

    public SimpleExoPlayer getPlayer() {
        return player;
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

    public void setLoadControl(LoadControl loadControl) {
        this.loadControl = loadControl;
    }

    public ExoPlayer.EventListener getEventListener() {
        return eventListener;
    }

    public void setEventListener(ExoPlayer.EventListener eventListener) {
        this.eventListener = eventListener;
    }
}
