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
import android.view.View;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.naivor.adapter.AdapterOperator;
import com.naivor.adapter.ListAdapter;
import com.naivor.adapter.ListHolder;
import com.naivor.player.VideoPlayer;
import com.naivor.player.constant.ScreenState;
import com.naivor.sample.R;
import com.naivor.sample.data.DataRepo;
import com.naivor.sample.data.VideoUrl;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by naivor on 17-7-24.
 */

public class VideoListAdapter extends ListAdapter<VideoUrl> {

    public VideoListAdapter(Context context) {
        super(context);
    }

    @Override
    public ListHolder<VideoUrl> onCreateViewHolder(View view, int i) {
        return new VideoListHolder(view);
    }

    @Override
    public int getLayoutRes(int i) {
        return R.layout.item_video;
    }


    public static class VideoListHolder extends ListHolder<VideoUrl> {

        @BindView(R.id.videoPlayer)
        VideoPlayer videoPlayer;
        @BindView(R.id.tv_name)
        TextView tvName;

        public VideoListHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);
        }

        @Override
        public void bindData(AdapterOperator<VideoUrl> operator, int position, VideoUrl itemData) {
            super.bindData(operator, position, itemData);

            tvName.setText(itemData.getName());

            //加载封面
            Glide.with(context)
                    .load(DataRepo.VIDEO_COVER)
                    .override(320, 240)
                    .into(videoPlayer.getPreviewView());

            videoPlayer.setScreenState(ScreenState.SCREEN_LAYOUT_LIST);
            videoPlayer.setUp(itemData.getUrl(), itemData.getName());

        }
    }
}
