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

package com.naivor.player.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;
import com.naivor.player.BuildConfig;
import com.naivor.player.core.PlayerCore;

import static com.google.android.exoplayer2.C.TYPE_DASH;
import static com.google.android.exoplayer2.C.TYPE_HLS;
import static com.google.android.exoplayer2.C.TYPE_OTHER;
import static com.google.android.exoplayer2.C.TYPE_SS;

/**
 * 播放源工具类
 * <p>
 * Created by tianlai on 17-7-7.
 */

public final class SourceUtils {

    protected static Handler mainHandler = new Handler();

    private SourceUtils() {
    }

    /**
     * 视频类型
     *
     * @param uri
     * @return
     */
    public static int getVideoType(@NonNull Uri uri) {

        String path = uri.getPath();

        if (!TextUtils.isEmpty(path)) {
            if (path.contains(".mpd")) {
                return C.TYPE_DASH;
            } else if (path.contains(".ism") || path.contains(".isml") || path.contains(".ism/manifest") || path.contains(".isml/manifest")) {
                return C.TYPE_SS;
            } else if (path.contains(".m3u8")) {
                return C.TYPE_HLS;
            }
        }

        return C.TYPE_OTHER;

    }


    /**
     * 多媒体数据源
     *
     * @param context
     * @param uri
     * @return
     */
    public static MediaSource buildMediaSource(@NonNull Context context, @NonNull Uri uri) {
        int type = getVideoType(uri);

        DataSource.Factory mediaDataSourceFactory = buildDataSourceFactory(context, true);


        EventLogger eventLogger = null;
        if (BuildConfig.DEBUG) {   //打印调试日志
            TrackSelector trackSelector = PlayerCore.instance(context).getTrackSelector();
            if (trackSelector != null && trackSelector instanceof MappingTrackSelector) {
                eventLogger = new EventLogger((MappingTrackSelector) trackSelector);
            }
        }

        switch (type) {
            case TYPE_SS:
                return new SsMediaSource(uri, buildDataSourceFactory(context, false),
                        new DefaultSsChunkSource.Factory(mediaDataSourceFactory), mainHandler, eventLogger);
            case TYPE_DASH:
                return new DashMediaSource(uri, buildDataSourceFactory(context, false),
                        new DefaultDashChunkSource.Factory(mediaDataSourceFactory), mainHandler, eventLogger);
            case TYPE_HLS:
                return new HlsMediaSource(uri, mediaDataSourceFactory, mainHandler, eventLogger);
            case TYPE_OTHER:
                return new ExtractorMediaSource(uri, mediaDataSourceFactory, new DefaultExtractorsFactory(), mainHandler,
                        eventLogger);
            default:
                throw new IllegalStateException("Unsupported type: " + type);
        }
    }

    /**
     * 带宽
     *
     * @return
     */
    public static DefaultBandwidthMeter buildBandwidthMeter() {
        return new DefaultBandwidthMeter();
    }

    /**
     * 数据工厂
     *
     * @param useBandwidthMeter
     * @return
     */
    public static DataSource.Factory buildDataSourceFactory(@NonNull Context context, boolean useBandwidthMeter) {
        return buildDataSourceFactory(context, useBandwidthMeter ? buildBandwidthMeter() : null);
    }

    /**
     * 数据工厂
     *
     * @param context
     * @param bandwidthMeter
     * @return
     */
    public static DataSource.Factory buildDataSourceFactory(@NonNull Context context, DefaultBandwidthMeter bandwidthMeter) {
        return new DefaultDataSourceFactory(context, bandwidthMeter,
                buildHttpDataSourceFactory(context, bandwidthMeter));
    }

    /**
     * 网络数据工厂
     *
     * @param context
     * @param bandwidthMeter
     * @return
     */
    public static HttpDataSource.Factory buildHttpDataSourceFactory(@NonNull Context context, DefaultBandwidthMeter bandwidthMeter) {
        return new DefaultHttpDataSourceFactory(getUserAgent(context), bandwidthMeter);
    }


    /**
     * 代理
     *
     * @param context
     * @return
     */
    public static String getUserAgent(@NonNull Context context) {
        ApplicationInfo info = context.getApplicationInfo();
        if (info != null) {
            return Util.getUserAgent(context, info.name);
        }

        return "";
    }
}
