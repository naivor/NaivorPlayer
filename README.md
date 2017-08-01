 ![naivorplayer_cover](https://github.com/naivor/NaivorPlayer/blob/master/doc/naivorplayer_cover.png)

![palform](https://img.shields.io/badge/palform-android-orange.svg)    ![luanguage](https://img.shields.io/badge/luanguage-java-09BCA4.svg)   [![Build Status](https://travis-ci.org/naivor/NaivorPlayer.svg?branch=master)](https://travis-ci.org/naivor/NaivorPlayer)    ![release](https://img.shields.io/badge/release-1.1.2-green.svg)     [![API](https://img.shields.io/badge/API-16%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=16)        <a href="http://www.methodscount.com/?lib=com.naivor%3Aplayer%3A1.1.1"><img src="https://img.shields.io/badge/Size-193 KB-e91e63.svg"/></a>      ![license](https://img.shields.io/badge/license-Apache%202.0-BC0962.svg)      ![author](https://img.shields.io/badge/%E4%BD%9C%E8%80%85-naivor-blue.svg)


## **简介**

NaivorPlayer  是一个Android平台上面的视频播放库，基于Exoplayer进行封装，提供美观的操作界面和简单易用调用接口，其界面参考 [**JieCaoVideoPlayer**](https://github.com/lipangit/JieCaoVideoPlayer) ,设计时方法和属性多采用protect，便于扩展，支持多种视频播放格式（ MP4, M4A, FMP4, WebM, MKV, MP3, Ogg, WAV, MPEG-TS, MPEG-PS, FLV 和 ADTS (AAC)，以及DASH，HLS）。





## **使用**

1. 将 naivorplayer 加入项目

   ```
   compile 'com.naivor:player:1.1.2'
   ```

2. 添加view (宽高按需要)

   ```
    <VideoPlayerNewVideoPlay
           android:id="@+id/videoPlayer"
           android:layout_width="match_parent"
           android:layout_height="240dp" />
   ```

3. 设置播放的url

   ```
      //设置播放源
      videoPlayer.setUp(url,ScreenState.SCREEN_LAYOUT_ORIGIN,"视频名字");
   ```

4. 播放

   ```
   videoPlayer.start(); //调用后立即播放，也可以不用这步，点击播放按钮
   ```

5. 其他

   * 设置数据源后马上缓冲

     ```
     videoPlayer.setAutoPrepare(true);
     ```

   * 视频封面

     ```
     //一般来说开启自动缓冲，就会把视频的第一帧作为封面，亦可以自己设置封面，有下面两种方式
     1. videoPlayer.getPreviewView() //能拿到显示封面的ImageView，给它加载图片就是了

     2. videoPlayer.setPreviewImage(bitmap); //将封面的Bitmap传入
     ```

   * 开启小窗播放

     ```
      videoPlayer.startWindowTiny();  //退出小窗调用 videoPlayer.backOriginWindow();
     ```

   * 在List中播放

     ```
     // 在onCreate 方法中 设置：
     1. VideoPlayer.playVideoInList(listview,false); //这里的listview既可是ListView,也可以是RecyclerView（用LinearLayoutManager）
      
     2. VideoPlayer.playVideoInList(listview,true); // 正在播放的视频滑出屏幕时自动开启小窗继续播放
      
     ....
      
     //在onDestroy方法中
     VideoPlayer.releaseAll();
     ```

   * 可自定义小窗大小

     ```
     VideoPlayer.setTinyWidth(tinyWidth);
     VideoPlayer.setTinyHeight(tinyHeight);
     ```

     ​



## **效果**

* 正常

  ​				![normal_play](https://github.com/naivor/NaivorPlayer/blob/master/doc/normal_play.gif)

* 全屏

  * 横屏

    ​			![normal_play](https://github.com/naivor/NaivorPlayer/blob/master/doc/full_screen_landscap.gif)

  * 竖屏

    ​			![normal_play](https://github.com/naivor/NaivorPlayer/blob/master/doc/full_screen_portrait.gif)

* 小窗

  ​				![normal_play](https://github.com/naivor/NaivorPlayer/blob/master/doc/tiny_screen.gif)

* 列表

  * Listview

    ![play_video_in_ListView](https://github.com/naivor/NaivorPlayer/blob/master/doc/play_video_in_ListView.gif) ![play_video_in_ListView_tiny_when_scrollOut](https://github.com/naivor/NaivorPlayer/blob/master/doc/play_video_in_ListView_tiny_when_scrollOut.gif)

  * RecyclerView

    ![play_video_in_RecyclerView](https://github.com/naivor/NaivorPlayer/blob/master/doc/play_video_in_RecyclerView.gif) ![play_video_in_RecyclerView_tiny_when_scrollOut](https://github.com/naivor/NaivorPlayer/blob/master/doc/play_video_in_RecyclerView_tiny_when_scrollOut.gif)



## **结构**

来一张截图吧，就不放UML了，大家有兴趣就自己看源码吧

![snapchot_struct](https://github.com/naivor/NaivorPlayer/blob/master/doc/snapchot_struct.png)





## 自定义UI

整理中，待续 ...





## **未来计划**

* 播放视频显示字幕

* 多视频源切换

* 边下边播

* 集成弹幕

* ......






## **最后**

开发这个库的初衷是感觉Android缺乏一个美观，易用，支持格式多的视频播放库，如果大家对此项目感兴趣，欢迎贡献代码和意见



 ## License

```
Copyright (c) 2017. Naivor.All rights reserved. 

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```



