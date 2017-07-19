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
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.naivor.adapter.AdapterOperator;
import com.naivor.adapter.RecyAdapter;
import com.naivor.adapter.RecyHolder;
import com.naivor.sample.R;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * MainActivity 的适配器
 * <p>
 * Created by naivor on 17-7-19.
 */

public class MainRecyAdapter extends RecyAdapter<String> {


    public MainRecyAdapter(Context context) {
        super(context);
    }

    @Override
    public RecyclerView.ViewHolder createHolder(View view, int i) {
        return new MainRecyHolder(view);
    }

    @Override
    public int getLayoutRes(int i) {
        return R.layout.item_recycler_main_layout;
    }

    /**
     * View 容器
     */
    public static class MainRecyHolder extends RecyHolder<String> {

        @BindView(R.id.tv_name)
        TextView tvName;

        public MainRecyHolder(View itemView) {
            super(itemView);

            ButterKnife.bind(this, itemView);

            registerClick(itemView);
        }

        @Override
        public void bindData(AdapterOperator<String> operator, int position, String itemData) {
            super.bindData(operator, position, itemData);

            if (!TextUtils.isEmpty(itemData)) {
                tvName.setText(itemData);
            }
        }
    }

}
