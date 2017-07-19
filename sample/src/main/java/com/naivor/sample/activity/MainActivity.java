package com.naivor.sample.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.View;

import com.naivor.adapter.AdapterOperator;
import com.naivor.sample.R;
import com.naivor.sample.activity.category.ExoPlayerActivity;
import com.naivor.sample.activity.category.ListActivity;
import com.naivor.sample.activity.category.NaivorPlayerActivity;
import com.naivor.sample.adapter.MainRecyAdapter;
import com.naivor.sample.utils.ToastUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * 主界面
 */
public class MainActivity extends AppCompatActivity {
    public static final String EXTRA = "title";
    private Context context;

    @BindView(R.id.rv_content)
    RecyclerView rvContent;

    private MainRecyAdapter mainRecyAdapter;

    private boolean isExitApp;

    private int[] itemNames = {R.string.txt_btn_exo,
            R.string.txt_btn_naivor,
            R.string.txt_btn_list,
            R.string.txt_btn_ui,
            R.string.txt_btn_words,
            R.string.txt_btn_multiSource,
            R.string.txt_btn_download,
            R.string.txt_btn_danmu};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        context = getApplicationContext();

        rvContent.setLayoutManager(new LinearLayoutManager(context));

        mainRecyAdapter = new MainRecyAdapter(context);
        mainRecyAdapter.setInnerListener(new AdapterOperator.InnerListener<String>() {
            @Override
            public void onClick(View view, String itemData, int postition) {
                switch (postition) {
                    case 0:
                        startActivity(new Intent(MainActivity.this, ExoPlayerActivity.class).putExtra(EXTRA, itemData));
                        break;
                    case 1:
                        startActivity(new Intent(MainActivity.this, NaivorPlayerActivity.class).putExtra(EXTRA, itemData));
                        break;
                    case 2:
                        startActivity(new Intent(MainActivity.this, ListActivity.class).putExtra(EXTRA, itemData));
                        break;
                    case 3:
                    case 4:
                    case 5:
                    case 6:
                    case 7:
                    default:
                        ToastUtil.show("尽请期待！");
                        break;
                }
            }
        });


        rvContent.setAdapter(mainRecyAdapter);
        mainRecyAdapter.setItems(createItems());

    }


    /**
     * 生成items
     *
     * @return
     */
    private List<String> createItems() {
        List<String> items = new ArrayList<>();

        if (itemNames != null) {
            for (int i = 0; i < itemNames.length; i++) {
                items.add(context.getString(itemNames[i]));
            }
        }

        return items;
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
