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

package com.naivor.player;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.ListView;

import com.naivor.player.constant.VideoState;
import com.naivor.player.core.PlayerCore;
import com.naivor.player.utils.VideoUtils;

import java.lang.ref.SoftReference;

import lombok.NonNull;
import timber.log.Timber;

/**
 * 在List中播放Video的辅助类
 * <p>
 * Created by naivor on 17-7-27.
 */

public final class ListVideoHelper {
    protected static SoftReference<VideoPlayer> reference;

    protected static Context context;

    //是否开启小窗当播放器滑出屏幕的时候（仅在list中有用）
    protected static boolean tinyWhenOutScreen = false;
    protected static boolean playInList = false;


    protected static VideoPlayer playingPlayerInList;
    protected static int playingPlayerInListPosition;
    protected static String playingUrl;
    protected static String playingName;
    @VideoState.VideoStateValue
    protected static int playingState;

    protected static int firstPos;
    protected static int lastPos;

    protected static int tinyWindowPosition;

    private ListVideoHelper() {
    }

    /**
     * 初始化
     *
     * @param listView
     */
    public static void init(@NonNull final ViewGroup listView) {

        tinyWindowPosition = -1;
        firstPos = lastPos;
        lastPos = tinyWindowPosition;

        Context activityContext = listView.getContext();

        if (activityContext != null) {
            context = activityContext.getApplicationContext();
            Activity activity = VideoUtils.getActivity(activityContext);
            if (activity != null && tinyWhenOutScreen) {
                VideoPlayer videoPlayer = (VideoPlayer) activity.findViewById(R.id.naivor_list_tiny_window);

                if (videoPlayer == null) {
                    View rootView = activity.findViewById(Window.ID_ANDROID_CONTENT);

                    if (rootView != null) {
                        activity.getLayoutInflater().inflate(R.layout.list_tiny_window, (ViewGroup) rootView, true);
                        videoPlayer = (VideoPlayer) activity.findViewById(R.id.naivor_list_tiny_window);

                    }
                }

                reference = new SoftReference<>(videoPlayer);

            }

            listView.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
                @Override
                public void onScrollChanged() {
                    if (listView instanceof RecyclerView) {
                        processRecyclerScroll((RecyclerView) listView);
                    } else if (listView instanceof ListView) {
                        processListViewScroll((ListView) listView);
                    } else {
                        processOtherScroll(listView);
                    }
                }
            });
        }
    }

    /**
     * 处理Video列表为ListView的滑动
     *
     * @param listView
     */
    protected static void processListViewScroll(ListView listView) {
        int first = listView.getFirstVisiblePosition();
        int last = listView.getLastVisiblePosition();
        int visibleCount = listView.getChildCount();

        Timber.v("ListView 滑动了,子控件数：%s，顶部：%s：%s,底部：%s :%s （原：新）", visibleCount,
                firstPos, first, lastPos, last);

        processScrollEvent(listView, first, last, visibleCount);

    }

    /**
     * @param listView
     */

    protected static void processRecyclerScroll(RecyclerView listView) {
        RecyclerView.LayoutManager layoutManager = listView.getLayoutManager();

        if (layoutManager instanceof LinearLayoutManager) {
            LinearLayoutManager linearLayoutManager = (LinearLayoutManager) layoutManager;

            int first = linearLayoutManager.findFirstVisibleItemPosition();
            int last = linearLayoutManager.findLastVisibleItemPosition();

            int visibleCount = listView.getChildCount();
            Timber.v("RecyclerView 滑动了,子控件数：%s，顶部：%s：%s,底部：%s :%s （原：新）",
                    visibleCount, firstPos, first, lastPos, last);

            processScrollEvent(listView, first, last, visibleCount);
        }
    }

    /**
     * 其他容器控件中播放 Video 的处理，扩展用
     *
     * @param listView
     */
    protected static void processOtherScroll(ViewGroup listView) {
        Timber.v(" 其他容器控件中播放 Video 的处理，待扩展 ");
    }

    /**
     * 处理滑动事件
     *
     * @param listView
     * @param first
     * @param last
     * @param visibleCount
     */
    protected static void processScrollEvent(ViewGroup listView, int first, int last, int visibleCount) {
        if (!isOrigin(first)) {

            int bottomChildPosition = visibleCount - 1;

            if (first > firstPos || last > lastPos) {    //上滑，并且顶部上一个滑出屏幕，或者底部下一个滑入屏幕

                if (first > firstPos) {     // 顶部上一个滑出屏幕
                    processTopScrollOut(firstPos);
                    updatePlayingPlayerInList(listView.getChildAt(0), first);
                }

                if (last > lastPos) {   // 底部下一个滑入屏幕
                    processBottomScrollIn(listView.getChildAt(bottomChildPosition), last);
                }

            } else if (first < firstPos || last < lastPos) { //下滑，并且顶部上一个滑入屏幕，或者底部下一个滑出屏幕

                if (first < firstPos) {      // 顶部上一个滑入屏幕
                    processTopScrollIn(listView.getChildAt(0), first);
                }

                if (last < lastPos) {   // 底部下一个滑出屏幕
                    processBottomScrollOut(lastPos);
                    updatePlayingPlayerInList(listView.getChildAt(bottomChildPosition), last);
                }
            } else {
                updatePlayingPlayerInList(listView.getChildAt(0), first);  // 顶部

                if (bottomChildPosition > 0) {
                    updatePlayingPlayerInList(listView.getChildAt(bottomChildPosition), last);   // 底部
                }
            }
        }

        firstPos = first;
        lastPos = last;
    }

    /**
     * 更新list中正在播放的Player
     *
     * @param childAt
     * @param postion
     */
    protected static void updatePlayingPlayerInList(View childAt, int postion) {
        Timber.v("更新list中正在播放的Player");

        if (postion != playingPlayerInListPosition || playingPlayerInList == null) {
            VideoPlayer listPlayer = findVideoPlayer(childAt);
            if (listPlayer != null && listPlayer.isVideoInPlayState()) {

                Timber.d("list中正在播放的Player位置：%s", postion);

                playingPlayerInList = listPlayer;
                playingPlayerInListPosition = postion;
                playingUrl = listPlayer.getUrl();
                playingName = listPlayer.getVideoName();
                playingState = playingPlayerInList.getVideoState();
            }
        }
    }

    /**
     * 是否初始状态
     *
     * @param first
     * @return
     */
    protected static boolean isOrigin(int first) {
        return firstPos == 0 && lastPos == 0 && first == 0;
    }


    /**
     * 底部滑入
     *
     * @param childAt
     */
    private static void processBottomScrollIn(View childAt, int position) {
        Timber.v("底部滑入,位置：%s", position);

        processScrollIn(childAt, position);
    }

    /**
     * 顶部滑出
     *
     * @param position
     */
    private static void processBottomScrollOut(int position) {
        Timber.v("底部滑出,位置：%s", position);
        processScrollOut(position);
    }


    /**
     * 顶部滑入
     *
     * @param childAt
     */
    private static void processTopScrollIn(View childAt, int position) {
        Timber.v("顶部滑入,位置：%s", position);

        processScrollIn(childAt, position);

    }

    /**
     * 顶部滑出
     *
     * @param position
     */
    private static void processTopScrollOut(int position) {
        Timber.v("顶部滑出,位置：%s", position);

        processScrollOut(position);
    }

    /**
     * 处理滑出
     *
     * @param position
     */
    private static void processScrollOut(int position) {
        Timber.v("处理滑出");

        if (playingPlayerInList != null && position == playingPlayerInListPosition) {
            if (tinyWhenOutScreen) {
                VideoPlayer tinyPlayer = reference.get();
                if (tinyPlayer != null) {
                    VideoPlayer.swapVideoPlayer(playingPlayerInList, tinyPlayer, playingState, playingUrl, playingName);

                    Timber.i("打开小窗");
                    tinyWindowPosition = position;
                }
            } else {
                Timber.i("停止播放");

                playingPlayerInList.stopAndReset();
                PlayerCore.instance(context).release();

                tinyWindowPosition = -1;
            }
        }
    }

    /**
     * 处理滑入
     *
     * @param childAt
     */
    private static void processScrollIn(View childAt, int position) {
        Timber.v("处理滑入");

        if (tinyWindowPosition >= 0 && tinyWhenOutScreen) {
            VideoPlayer tinyPlayer = reference.get();
            if (tinyPlayer != null) {
                if (tinyPlayer.isShown() && tinyWindowPosition == position) {
                    VideoPlayer listPlayer = findVideoPlayer(childAt);
                    if (listPlayer != null && tinyPlayer.isVideoInPlayState()) {
                        VideoPlayer.swapVideoPlayer(tinyPlayer, listPlayer, tinyPlayer.getVideoState(), null, null);

                        // 记录新的播放位置
                        playingPlayerInList = listPlayer;
                        playingPlayerInListPosition = position;
                        playingUrl = listPlayer.getUrl();
                        playingName = listPlayer.getVideoName();
                        playingState = playingPlayerInList.getVideoState();
                    }

                    tinyPlayer.stopAndReset();

                    Timber.i("退出小窗");
                    tinyWindowPosition = -1;
                }
            }
        }
    }

    /**
     * 获取itemview 中的 VideoPlayer
     *
     * @param childAt
     * @return
     */
    protected static VideoPlayer findVideoPlayer(View childAt) {
        if (childAt instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) childAt;
            return (VideoPlayer) viewGroup.findViewWithTag(VideoPlayer.TAG);
        } else if (childAt instanceof VideoPlayer) {
            return (VideoPlayer) childAt;
        }

        return null;
    }

    /**
     * 播放新视频的时候，退出小窗
     */
    public static void stopWhenNewVideoPlay() {
        Timber.d("播放新视频，退出小窗");

        if (tinyWindowPosition >= 0 && tinyWhenOutScreen) {
            VideoPlayer tinyPlayer = reference.get();
            if (tinyPlayer != null && tinyPlayer.getVisibility() == View.VISIBLE) {

                tinyPlayer.stopAndReset();

                Timber.i("退出小窗");
                tinyWindowPosition = -1;
            }
        }
    }

    /**
     * 清理资源
     */
    public static void release() {
        reference.clear();
        reference = null;
        tinyWhenOutScreen = false;
        playInList = false;
        playingPlayerInList = null;
        playingUrl = null;
        playingName = null;

    }

    public static boolean isTinyWhenOutScreen() {
        return tinyWhenOutScreen;
    }

    public static void setTinyWhenOutScreen(boolean tinyWhenOutScreen) {
        ListVideoHelper.tinyWhenOutScreen = tinyWhenOutScreen;
    }

    public static boolean isPlayInList() {
        return playInList;
    }

    public static void setPlayInList(boolean playInList) {
        ListVideoHelper.playInList = playInList;
    }
}
