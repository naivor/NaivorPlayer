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
import android.view.View;
import android.widget.ImageView;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import timber.log.Timber;


/**
 * 预览
 * <p>
 * Created by naivor on 17-7-19.
 */

public class VideoPreview {
    @Getter
    @Setter
    protected boolean showPreview = true;  //默认使用预览
    @Getter
    @Setter
    protected Bitmap defaultPreview;
    @Getter
    protected ImageView preview;


    public VideoPreview(ImageView videoPreview) {
        this.preview = videoPreview;

    }

    /**
     * 显示预览
     */
    public void showPreview(boolean showFirstFrame) {
        Timber.d("显示预览");
        if (preview != null && showPreview) {

            if (showFirstFrame) {
                Timber.d("显示第一帧");
                setPreviewFromBitmap(defaultPreview);
            }

            preview.setVisibility(View.VISIBLE);
        } else {
            preview.setVisibility(View.GONE);
        }
    }


    /**
     * 隐藏预览
     */
    public void hidePreview() {
        Timber.d("隐藏预览");
        if (preview != null) {
            preview.setVisibility(View.GONE);
        }
    }

    /**
     * 更新预览
     */
    public void updatePreview(@NonNull Bitmap bitmap) {
        Timber.d("更新预览");
        if (preview != null && showPreview) {
            if (defaultPreview == null) {
                defaultPreview = bitmap;
            }
        }
    }


    /**
     * @param bitmap
     * @return
     */
    protected boolean setPreviewFromBitmap(Bitmap bitmap) {

        Timber.d("设置 bitmap 到 preview");

        if (bitmap != null) {
            int bitmapWidth = bitmap.getWidth();
            int bitmapHeight = bitmap.getHeight();
            if (bitmapWidth > 0 && bitmapHeight > 0) {
                preview.setImageBitmap(bitmap);
                preview.setVisibility(View.VISIBLE);
                return true;
            }
        }
        return false;
    }


}
