package com.naivor.sample.activity.category.list;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;

import com.naivor.player.VideoPlayer;
import com.naivor.sample.R;
import com.naivor.sample.adapter.VideoRecyclerAdapter;
import com.naivor.sample.data.DataRepo;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.naivor.sample.activity.MainActivity.EXTRA;


/**
 * RecyclerView 中播放视频
 */
public class RecyclerViewVideoActivity extends AppCompatActivity {
    private Context context;

    @BindView(R.id.rv_content)
    RecyclerView rvContent;

    private VideoRecyclerAdapter recyclerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycler_view_video);
        ButterKnife.bind(this);

        context = getApplicationContext();

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayUseLogoEnabled(false);
        actionBar.setTitle(getIntent().getStringExtra(EXTRA));

        // 设置在List中播放视频，list容器view，是否滑出屏幕自动小窗播放
        VideoPlayer.playVideoInList(rvContent, true);

        rvContent.setLayoutManager(new LinearLayoutManager(context));
        recyclerAdapter = new VideoRecyclerAdapter(context);
        rvContent.setAdapter(recyclerAdapter);

        recyclerAdapter.setItems(DataRepo.get(context).getVideoUrls());

    }


    @Override
    public void onBackPressed() {

        if (VideoPlayer.onBackPressed()) {
            return;
        }
        super.onBackPressed();
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
    protected void onDestroy() {
        super.onDestroy();
        VideoPlayer.releaseAll();
    }
}
