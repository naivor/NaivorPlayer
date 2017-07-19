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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.ImageView;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.id3.ApicFrame;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;

import lombok.Getter;
import lombok.Setter;


/**
 * 预览
 * <p>
 * Created by naivor on 17-7-19.
 */

public class VideoPreview {
    @Getter
    @Setter
    protected boolean usePreview = true;  //默认使用预览
    @Getter
    @Setter
    protected Bitmap defaultPreview;
    @Getter
    protected ImageView preview;
    protected AspectRatioFrameLayout contentFrame;

    protected ExoPlayer player;

    public VideoPreview(AspectRatioFrameLayout contentFrame, ImageView videoPreview, ExoPlayer player) {
        this.contentFrame = contentFrame;
        this.preview = videoPreview;
        this.player = player;

    }

    /**
     * 更新预览
     */
    public void updatePreview() {
        if (player == null || preview == null) {
            return;
        }

        TrackSelectionArray selections = player.getCurrentTrackSelections();
        for (int i = 0; i < selections.length; i++) {
            if (player.getRendererType(i) == C.TRACK_TYPE_VIDEO && selections.get(i) != null) {
                // Video enabled so artwork must be hidden. If the shutter is closed, it will be opened in
                // onRenderedFirstFrame().
                hidePreview();
                return;
            }
        }

        // Display artwork if enabled and available, else hide it.
        if (usePreview) {
            for (int i = 0; i < selections.length; i++) {
                TrackSelection selection = selections.get(i);
                if (selection != null) {
                    for (int j = 0; j < selection.length(); j++) {
                        Metadata metadata = selection.getFormat(j).metadata;
                        if (metadata != null && setPreviewFromMetadata(metadata)) {
                            return;
                        }
                    }
                }
            }
            if (setPreviewFromBitmap(defaultPreview)) {
                return;
            }
        }
        // Preview disabled or unavailable.
        hidePreview();
    }

    /**
     * @param metadata
     * @return
     */
    protected boolean setPreviewFromMetadata(Metadata metadata) {
        for (int i = 0; i < metadata.length(); i++) {
            Metadata.Entry metadataEntry = metadata.get(i);
            if (metadataEntry instanceof ApicFrame) {
                byte[] bitmapData = ((ApicFrame) metadataEntry).pictureData;
                Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.length);
                return setPreviewFromBitmap(bitmap);
            }
        }
        return false;
    }

    /**
     * @param bitmap
     * @return
     */
    protected boolean setPreviewFromBitmap(Bitmap bitmap) {
        if (bitmap != null) {
            int bitmapWidth = bitmap.getWidth();
            int bitmapHeight = bitmap.getHeight();
            if (bitmapWidth > 0 && bitmapHeight > 0) {
                if (contentFrame != null) {
                    contentFrame.setAspectRatio((float) bitmapWidth / bitmapHeight);
                }
                preview.setImageBitmap(bitmap);
                preview.setVisibility(View.VISIBLE);
                return true;
            }
        }
        return false;
    }

    /**
     * 隐藏预览
     */
    public void hidePreview() {
        if (preview != null) {
            preview.setImageResource(android.R.color.transparent); // Clears any bitmap reference.
            preview.setVisibility(View.INVISIBLE);
        }
    }
}
