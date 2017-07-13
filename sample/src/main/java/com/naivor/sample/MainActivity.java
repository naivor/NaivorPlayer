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

//        String testUrl = "http://video.dispatch.tc.qq.com/34724189/w0023sfqizt.p201.1.mp4?vkey=CABB2A0EAEF93A105CE65F52BA50A9C175BFA8BD7714DEA5875A1F7B7F4A6F104EABF4A48F60DCC72D909CF509F2006449D771835864ECA0456684068370ACE34AE3F1807FAE422D8990EEFD9ABF2CB963C92ABD6CB1D52BFA2D400EC770A5782528C780D21C52E84DDEB85FB8DBD0803D8579F98113419C";
//        String testUrl = "http://hot.vrs.sohu.com/ipad3874103_4600163287712_6085424.m3u8?vid=3874103&uid=1499842155122279&plat=17&SOHUSVP=zt8zITGGb3zeVh7Ic_g70Np5ZZT7JnJBMDQf7ti0dMM&pt=2&prod=h5&pg=1&eye=0&cv=1.0.0&qd=68000&src=11070001&ca=4&cateCode=115&_c=1&appid=tv";
        String testUrl = "http://video.jiecao.fm/11/23/xin/%E5%81%87%E4%BA%BA.mp4";
        Timber.d("测试地址：%s", testUrl);
        videoPlayer.setUp(testUrl, VideoPlayer.SCREEN_LAYOUT_NORMAL, "测试测试");

//        videoPlayer.start();
    }


    @Override
    protected void onResume() {
        super.onResume();

        videoPlayer.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        videoPlayer.pause();
    }

    @Override
    protected void onStop() {
        super.onStop();

        videoPlayer.stop();
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
