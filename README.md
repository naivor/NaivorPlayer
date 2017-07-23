 ![naivorplayer_cover](https://github.com/naivor/NaivorPlayer/blob/master/doc/naivorplayer_cover.png)
## **简介**

NaivorPlayer  是一个Android平台上面的视频播放库，基于Exoplayer进行封装，提供美观的操作界面和简单易用调用接口，其界面参考 [**JieCaoVideoPlayer**](https://github.com/lipangit/JieCaoVideoPlayer) ,设计时方法和属性多采用protect，便于扩展，支持多种视频播放格式（ MP4, M4A, FMP4, WebM, MKV, MP3, Ogg, WAV, MPEG-TS, MPEG-PS, FLV 和 ADTS (AAC)，以及DASH，HLS）。





## **使用**

1. 将 naivorplayer 加入项目

   ```
   compile 'com.naivor:player:1.0.1'
   ```

2. 添加view (宽高按需要)

   ```
    <com.naivor.player.VideoPlayer
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
     1.videoPlayer.getPreviewView() //能拿到显示封面的ImageView，给它加载图片就是了
     2.videoPlayer.setPreviewImage(bitmap); //将封面的Bitmap传入
     ```

   * 开启小窗播放

     ```
      videoPlayer.startWindowTiny();  //退出小窗调用 videoPlayer.backOriginWindow();
     ```




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



 ## Copyright

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
```



