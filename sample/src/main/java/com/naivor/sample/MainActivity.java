package com.naivor.sample;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.naivor.player.VideoPlayer;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    private VideoPlayer videoPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_main);

        videoPlayer = (VideoPlayer) findViewById(R.id.videoPlayer);

        String testUrl = "http://video.jiecao.fm/11/23/xin/%E5%81%87%E4%BA%BA.mp4";
        Timber.d("测试地址：%s", testUrl);
        videoPlayer.setUp(testUrl, VideoPlayer.SCREEN_WINDOW_FULLSCREEN, "测试测试");

        videoPlayer.start();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.exoplayer:
                startActivity(new Intent(this, ExoPlayerActivity.class));
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
