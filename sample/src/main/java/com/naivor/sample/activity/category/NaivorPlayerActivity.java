package com.naivor.sample.activity.category;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.bumptech.glide.Glide;
import com.naivor.player.VideoPlayer;
import com.naivor.player.constant.ScreenState;
import com.naivor.sample.R;
import com.naivor.sample.activity.MainActivity;
import com.naivor.sample.data.DataRepo;
import com.naivor.sample.data.VideoUrl;
import com.naivor.sample.utils.ToastUtil;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

/**
 * NaivorPlayer 的使用例子
 */
public class NaivorPlayerActivity extends AppCompatActivity {

    @BindView(R.id.videoPlayer)
    VideoPlayer videoPlayer;
    @BindView(R.id.btn_tiny)
    Button btnTiny;
    @BindView(R.id.edt_url)
    EditText edtUrl;
    @BindView(R.id.btn_play)
    Button btnPlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_navor_player);
        ButterKnife.bind(this);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayUseLogoEnabled(false);
        actionBar.setTitle(getIntent().getStringExtra(MainActivity.EXTRA));


        VideoUrl videoUrl = DataRepo.get(getApplicationContext()).getVideoUrl();

        if (videoUrl != null) {
            Timber.d("测试地址：%s", videoUrl.toString());

            //自动缓冲
//            videoPlayer.setAutoPrepare(true);

            //加载封面
            Glide.with(this)
                    .load(DataRepo.VIDEO_COVER)
                    .override(320, 240)
                    .into(videoPlayer.getPreviewView());

            //设置播放源
            videoPlayer.setUp(videoUrl.getUrl(), videoUrl.getName());

//            videoPlayer.setScreenState(ScreenState.SCREEN_WINDOW_FULLSCREEN_LOCK);

            //开始播放
            videoPlayer.start();
        }
    }

    /**
     * @param v
     */
    @OnClick({R.id.btn_tiny, R.id.btn_play})
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_tiny:
                if (videoPlayer.getScreenState() == ScreenState.SCREEN_WINDOW_TINY) {
                    videoPlayer.backOriginWindow();
                } else {
                    videoPlayer.startWindowTiny();
                }
                break;
            case R.id.btn_play:

                String text = edtUrl.getText().toString();
                if (TextUtils.isEmpty(text)) {
                    ToastUtil.show("请输入播放地址！");
                } else {
                    Timber.d("播放地址：%s", text);
                    videoPlayer.setUp(text, "输入的播放地址");
                    videoPlayer.start();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onResume() {
        super.onResume();

        videoPlayer.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        videoPlayer.onPause();
    }

    @Override
    protected void onDestroy() {
        videoPlayer.release();
        super.onDestroy();
    }


    @Override
    public void onBackPressed() {

        if (!videoPlayer.backPress()) {
            super.onBackPressed();
        }

    }
}
