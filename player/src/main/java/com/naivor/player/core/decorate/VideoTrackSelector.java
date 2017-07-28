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


import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.RendererCapabilities;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectorResult;

/**
 * 轨道选择器
 * <p>
 * Created by naivor on 17-7-28.
 */

public final class VideoTrackSelector extends TrackSelector {

    private DefaultTrackSelector defaultTrackSelector;

    private TrackSelector trackSelector;

    public VideoTrackSelector() {
        defaultTrackSelector = new DefaultTrackSelector();
    }

    @Override
    public TrackSelectorResult selectTracks(RendererCapabilities[] rendererCapabilities,
                                            TrackGroupArray trackGroupArray) throws ExoPlaybackException {
        if (trackSelector != null) {
            return trackSelector.selectTracks(rendererCapabilities, trackGroupArray);
        } else {
            return defaultTrackSelector.selectTracks(rendererCapabilities, trackGroupArray);
        }
    }

    @Override
    public void onSelectionActivated(Object o) {
        if (trackSelector != null) {
            trackSelector.onSelectionActivated(o);
        } else {
            defaultTrackSelector.onSelectionActivated(o);
        }
    }

    public TrackSelector getTrackSelector() {
        return trackSelector;
    }

    public void setTrackSelector(TrackSelector trackSelector) {
        this.trackSelector = trackSelector;
    }
}
