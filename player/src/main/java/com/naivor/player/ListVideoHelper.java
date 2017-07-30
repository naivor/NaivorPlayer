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
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.ListView;

import com.naivor.player.R;
import com.naivor.player.VideoPlayer;
import com.naivor.player.constant.VideoState;
import com.naivor.player.utils.VideoUtils;

import java.lang.ref.SoftReference;

import lombok.NonNull;
import timber.log.Timber;

/**
 * Created by naivor on 17-7-27.
 */

public final class ListVideoHelper {
    protected static SoftReference<VideoPlayer> reference;

    protected static VideoPlayer playingPlayerInList;
    protected static int playingPlayerInListPosition;
    protected static String playingUrl;
    protected static String playingName;
    @VideoState.VideoStateValue
    protected static int playingState;

    protected static int firstPos;
    protected static int lastPos;

    protected static int tinyWindowPosition;

    /**
     * 初始化
     *
     * @param listView
     */
    public static void init(@NonNull final ViewGroup listView) {

        firstPos = lastPos = tinyWindowPosition = -1;

        Context context = listView.getContext();

        if (context != null) {
            Activity activity = VideoUtils.getActivity(context);
            if (activity != null) {
                VideoPlayer videoPlayer = (VideoPlayer) activity.findViewById(R.id.naivor_list_tiny_window);

                if (videoPlayer == null) {
                    View rootView = activity.findViewById(Window.ID_ANDROID_CONTENT);

                    if (rootView != null) {
                        activity.getLayoutInflater().inflate(R.layout.list_tiny_window, (ViewGroup) rootView, true);
                        videoPlayer = (VideoPlayer) activity.findViewById(R.id.naivor_list_tiny_window);

                    }
                }

                reference = new SoftReference<>(videoPlayer);


                listView.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
                    @Override
                    public void onScrollChanged() {
                        if (listView instanceof RecyclerView) {
                            processRecyclerScroll((RecyclerView) listView);
                        } else if (listView instanceof ListView) {
                            processListViewScroll((ListView) listView);
                        }
                    }
                });

            }
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


        Timber.v("ListView 滑动了,子控件数：%s，顶部：%s：%s,底部：%s :%s （原：新）", listView.getChildCount(), firstPos, first, lastPos, last);

        if (!isOrigin(first)) {

            int bottomChildPosition = listView.getChildCount() - 1;

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
            Timber.v("处理滑出 111  %s", VideoState.getVideoStateName(playingPlayerInList.getVideoState()));
            VideoPlayer tinyPlayer = reference.get();
            if (tinyPlayer != null) {
                Timber.v("处理滑出 333");

                tinyPlayer.setUpListTiny(playingUrl, playingName);
                VideoPlayer.swapVideoPlayer(playingPlayerInList, tinyPlayer, playingState);

                Timber.i("打开小窗");
                tinyWindowPosition = position;
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

        if (tinyWindowPosition >= 0) {
            VideoPlayer tinyPlayer = reference.get();
            if (tinyPlayer != null) {
                Timber.v("处理滑入 111  %s,%s", tinyWindowPosition, position);
                if (tinyPlayer.isShown() && tinyWindowPosition == position) {
                    Timber.v("处理滑入 222");
                    VideoPlayer listPlayer = findVideoPlayer(childAt);
                    if (listPlayer != null && tinyPlayer.isVideoInPlayState()) {
                        Timber.v("处理滑入 333 ,%s,%s", tinyWindowPosition, position);
                        VideoPlayer.swapVideoPlayer(tinyPlayer, listPlayer, tinyPlayer.getVideoState());
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
     * @param listView
     */

    protected static void processRecyclerScroll(RecyclerView listView) {

    }


    /**
     * 播放新视频的时候，退出小窗
     */
    public static void stopWhenNewVideoPlay() {
        Timber.d("播放新视频，退出小窗");

        if (tinyWindowPosition >= 0) {
            VideoPlayer tinyPlayer = reference.get();
            if (tinyPlayer != null && tinyPlayer.getVisibility() == View.VISIBLE) {

                tinyPlayer.stopAndReset();

                Timber.i("退出小窗");
                tinyWindowPosition = -1;
            }
        }
    }
}
