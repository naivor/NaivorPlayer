package com.naivor.sample.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;

import com.naivor.sample.R;
import com.naivor.sample.activity.category.ExoPlayerActivity;
import com.naivor.sample.activity.category.ListActivity;
import com.naivor.sample.activity.category.NaivorPlayerActivity;
import com.naivor.sample.utils.ToastUtil;

import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * 主界面
 */
public class MainActivity extends AppCompatActivity {
    public static final String EXTRA = "title";

    @BindView(R.id.btn_exo)
    Button btnExo;
    @BindView(R.id.btn_naivor)
    Button btnNaivor;
    @BindView(R.id.btn_list)
    Button btnList;
    @BindView(R.id.btn_ui)
    Button btnUi;
    @BindView(R.id.btn_words)
    Button btnWords;
    @BindView(R.id.btn_multiSource)
    Button btnMultiSource;
    @BindView(R.id.btn_download)
    Button btnDownload;
    @BindView(R.id.btn_danmu)
    Button btnDanmu;


    private boolean isExitApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }

    /**
     * 点击监听
     *
     * @param v
     */
    @OnClick({R.id.btn_exo, R.id.btn_naivor, R.id.btn_list, R.id.btn_ui, R.id.btn_words,
            R.id.btn_multiSource, R.id.btn_download, R.id.btn_danmu})
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_exo:
                startActivity(new Intent(this, ExoPlayerActivity.class).putExtra(EXTRA, btnExo.getText().toString()));
                break;
            case R.id.btn_naivor:
                startActivity(new Intent(this, NaivorPlayerActivity.class).putExtra(EXTRA, btnNaivor.getText().toString()));
                break;
            case R.id.btn_list:
                startActivity(new Intent(this, ListActivity.class).putExtra(EXTRA, btnList.getText().toString()));
                break;
            case R.id.btn_ui:
            case R.id.btn_words:
            case R.id.btn_multiSource:
            case R.id.btn_download:
            case R.id.btn_danmu:
            default:
                ToastUtil.show("尽请期待！");
                break;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Timer timer = new Timer();
            if (isExitApp == false) {
                isExitApp = true;
                ToastUtil.show("再按一次退出");
                timer.schedule(new TimerTask() {

                    @Override
                    public void run() {
                        isExitApp = false;
                    }
                }, 2000);
            } else {
                finish();
            }
        }

        return false;
    }
}
