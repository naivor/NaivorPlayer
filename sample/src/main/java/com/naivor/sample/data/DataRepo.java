package com.naivor.sample.data;

import android.content.Context;

import com.naivor.sample.R;

import java.util.List;
import java.util.Random;

import lombok.NonNull;

import static android.R.attr.versionName;
import static android.R.attr.x;

/**
 * 数据仓库
 * <p>
 * Created by naivor on 17-7-16.
 */

public final class DataRepo {

    private Context context;

    private static DataRepo dataRepo;

    private String[] videoUrls;
    private String[] videoNames;

    private DataRepo(@NonNull Context context) {
        this.context = context.getApplicationContext();

        videoUrls = context.getResources().getStringArray(R.array.videoUrls);
        videoNames = context.getResources().getStringArray(R.array.videoNames);
    }

    /**
     * 单利
     *
     * @param context
     * @return
     */
    public static DataRepo get(@NonNull Context context) {
        if (dataRepo == null) {
            synchronized (DataRepo.class) {
                if (dataRepo == null) {
                    dataRepo = new DataRepo(context);
                }
            }
        }

        return dataRepo;
    }


    /**
     * 获取播放视频
     *
     * @return
     */
    public VideoUrl getVideoUrl() {
        int size = Math.min(videoNames.length, videoUrls.length);

        int index = -1;

        while (size <= index || index < 0) {
            if (size == 0) {
                return null;
            }

            index = new Random().nextInt(size);
        }

        return new VideoUrl(videoUrls[index], videoNames[index]);

    }
}
