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


import android.content.Context;
import android.os.Handler;

import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.metadata.MetadataRenderer;
import com.google.android.exoplayer2.text.TextRenderer;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

/**
 * 渲染工厂
 * <p>
 * Created by naivor on 17-7-28.
 */

public final class VideoRenderersFactory extends DefaultRenderersFactory {

    private RenderersFactory renderersFactory;

    public RenderersFactory getRenderersFactory() {
        return renderersFactory;
    }

    @Override
    public Renderer[] createRenderers(Handler eventHandler, VideoRendererEventListener videoRendererEventListener,
                                      AudioRendererEventListener audioRendererEventListener,
                                      TextRenderer.Output textRendererOutput, MetadataRenderer.Output metadataRendererOutput) {

        if (renderersFactory != null) {
            return renderersFactory.createRenderers(eventHandler, videoRendererEventListener,
                    audioRendererEventListener, textRendererOutput, metadataRendererOutput);
        } else {
            return super.createRenderers(eventHandler, videoRendererEventListener,
                    audioRendererEventListener, textRendererOutput, metadataRendererOutput);
        }
    }

    public void setRenderersFactory(RenderersFactory renderersFactory) {
        this.renderersFactory = renderersFactory;
    }

    public VideoRenderersFactory(Context context) {
        super(context);
    }
}
