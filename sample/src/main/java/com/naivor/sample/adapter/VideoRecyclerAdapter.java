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

package com.naivor.sample.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.naivor.adapter.AdapterOperator;
import com.naivor.adapter.RecyAdapter;
import com.naivor.adapter.RecyHolder;
import com.naivor.player.VideoPlayer;
import com.naivor.player.constant.ScreenState;
import com.naivor.sample.R;
import com.naivor.sample.data.VideoUrl;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by naivor on 17-7-24.
 */

public class VideoRecyclerAdapter extends RecyAdapter<VideoUrl> {

    public VideoRecyclerAdapter(Context context) {
        super(context);
    }

    @Override
    public RecyclerView.ViewHolder createHolder(View view, int i) {
        return new VideoRecyclerHolder(view);
    }

    @Override
    public int getLayoutRes(int i) {
        return R.layout.item_video;
    }

    public static class VideoRecyclerHolder extends RecyHolder<VideoUrl> {

        @BindView(R.id.videoPlayer)
        VideoPlayer videoPlayer;

        public VideoRecyclerHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
        }

        @Override
        public void bindData(AdapterOperator<VideoUrl> operator, int position, VideoUrl itemData) {
            super.bindData(operator, position, itemData);

            videoPlayer.setScreenState(ScreenState.SCREEN_LAYOUT_LIST);
            videoPlayer.setUp(itemData.getUrl(), itemData.getName());

        }
    }
}
