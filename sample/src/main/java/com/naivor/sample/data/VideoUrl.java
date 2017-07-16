package com.naivor.sample.data;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 视频播放源
 * <p>
 * Created by naivor on 17-7-16.
 */

@AllArgsConstructor
@Data
public class VideoUrl {
    private String url;
    private String name;
}
