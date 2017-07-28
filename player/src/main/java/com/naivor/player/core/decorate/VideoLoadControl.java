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

package com.naivor.player.core.decorate;

import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.Allocator;
import com.google.android.exoplayer2.upstream.DefaultAllocator;
import com.google.android.exoplayer2.util.PriorityTaskManager;
import com.google.android.exoplayer2.util.Util;

/**
 * 默认的播放加载控制器
 * <p>
 * Created by naivor on 17-7-28.
 */

public final class VideoLoadControl implements LoadControl {

    public static final int DEFAULT_MIN_BUFFER_MS = 15000;
    public static final int DEFAULT_MAX_BUFFER_MS = 30000;
    public static final int DEFAULT_BUFFER_FOR_PLAYBACK_MS = 2500;
    public static final int DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 5000;

    private static final int ABOVE_HIGH_WATERMARK = 0;
    private static final int BETWEEN_WATERMARKS = 1;
    private static final int BELOW_LOW_WATERMARK = 2;

    private final DefaultAllocator allocator;
    private final PriorityTaskManager priorityTaskManager;

    private final long minBufferUs;
    private final long maxBufferUs;
    private final long bufferForPlaybackUs;
    private final long bufferForPlaybackAfterRebufferUs;
    private int targetBufferSize;
    private boolean isBuffering;

    private LoadControl loadControl;

    public VideoLoadControl() {
        this(new DefaultAllocator(true, Short.MAX_VALUE));
    }

    public VideoLoadControl(DefaultAllocator allocator) {
        this(allocator, DEFAULT_MIN_BUFFER_MS, DEFAULT_MAX_BUFFER_MS,
                DEFAULT_BUFFER_FOR_PLAYBACK_MS, DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS);
    }

    public VideoLoadControl(DefaultAllocator allocator, int minBufferMs, int maxBufferMs, long bufferForPlaybackMs, long bufferForPlaybackAfterRebufferMs) {
        this(allocator, minBufferMs, maxBufferMs, bufferForPlaybackMs, bufferForPlaybackAfterRebufferMs, (PriorityTaskManager) null);
    }

    public VideoLoadControl(DefaultAllocator allocator, int minBufferMs, int maxBufferMs, long bufferForPlaybackMs, long bufferForPlaybackAfterRebufferMs, PriorityTaskManager priorityTaskManager) {
        this.allocator = allocator;
        this.minBufferUs = (long) minBufferMs * 1000L;
        this.maxBufferUs = (long) maxBufferMs * 1000L;
        this.bufferForPlaybackUs = bufferForPlaybackMs * 1000L;
        this.bufferForPlaybackAfterRebufferUs = bufferForPlaybackAfterRebufferMs * 1000L;
        this.priorityTaskManager = priorityTaskManager;
    }

    @Override
    public void onPrepared() {
        if (loadControl != null) {
            loadControl.onPrepared();
        } else {
            this.reset(false);
        }
    }

    @Override
    public void onTracksSelected(Renderer[] renderers, TrackGroupArray trackGroupArray, TrackSelectionArray trackSelectionArray) {
        if (loadControl != null) {
            loadControl.onTracksSelected(renderers, trackGroupArray, trackSelectionArray);
        } else {
            this.targetBufferSize = 0;

            for (int i = 0; i < renderers.length; ++i) {
                if (trackSelectionArray.get(i) != null) {
                    this.targetBufferSize += Util.getDefaultBufferSize(renderers[i].getTrackType());
                }
            }

            this.allocator.setTargetBufferSize(this.targetBufferSize);
        }
    }

    @Override
    public void onStopped() {
        if (loadControl != null) {
            loadControl.onStopped();
        } else {
            this.reset(true);
        }
    }

    @Override
    public void onReleased() {
        if (loadControl != null) {
            loadControl.onReleased();
        } else {
            this.reset(true);
        }
    }

    @Override
    public Allocator getAllocator() {
        if (loadControl != null) {
            return loadControl.getAllocator();
        } else {
            return this.allocator;
        }
    }

    @Override
    public boolean shouldStartPlayback(long bufferedDurationUs, boolean rebuffering) {
        if (loadControl != null) {
            return loadControl.shouldStartPlayback(bufferedDurationUs, rebuffering);
        } else {
            long minBufferDurationUs = rebuffering ? this.bufferForPlaybackAfterRebufferUs : this.bufferForPlaybackUs;
            return minBufferDurationUs <= 0L || bufferedDurationUs >= minBufferDurationUs;
        }
    }

    @Override
    public boolean shouldContinueLoading(long bufferedDurationUs) {
        if (loadControl != null) {
            return loadControl.shouldContinueLoading(bufferedDurationUs);
        } else {
            int bufferTimeState = this.getBufferTimeState(bufferedDurationUs);
            boolean targetBufferSizeReached = this.allocator.getTotalBytesAllocated() >= this.targetBufferSize;
            boolean wasBuffering = this.isBuffering;
            this.isBuffering = bufferTimeState == BELOW_LOW_WATERMARK || bufferTimeState == BETWEEN_WATERMARKS && this.isBuffering && !targetBufferSizeReached;
            if (this.priorityTaskManager != null && this.isBuffering != wasBuffering) {
                if (this.isBuffering) {
                    this.priorityTaskManager.add(0);
                } else {
                    this.priorityTaskManager.remove(0);
                }
            }

            return this.isBuffering;
        }
    }


    /**
     * @param bufferedDurationUs
     * @return
     */
    private int getBufferTimeState(long bufferedDurationUs) {
        return bufferedDurationUs > this.maxBufferUs ? ABOVE_HIGH_WATERMARK : (bufferedDurationUs < this.minBufferUs ? BELOW_LOW_WATERMARK : BETWEEN_WATERMARKS);
    }


    /**
     * @param resetAllocator
     */
    private void reset(boolean resetAllocator) {
        this.targetBufferSize = 0;
        if (this.priorityTaskManager != null && this.isBuffering) {
            this.priorityTaskManager.remove(0);
        }

        this.isBuffering = false;
        if (resetAllocator) {
            this.allocator.reset();
        }

    }

    public LoadControl getLoadControl() {
        return loadControl;
    }

    public void setLoadControl(LoadControl loadControl) {
        this.loadControl = loadControl;
    }
}