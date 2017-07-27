package com.naivor.sample.activity.category.list;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.ListView;

import com.naivor.player.VideoPlayer;
import com.naivor.sample.R;
import com.naivor.sample.adapter.VideoListAdapter;
import com.naivor.sample.data.DataRepo;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.naivor.sample.activity.MainActivity.EXTRA;


/**
 * Listview 中播放视频
 */
public class ListVideoActivity extends AppCompatActivity {
    private Context context;

    @BindView(R.id.lv_content)
    ListView lvContent;

    private VideoListAdapter listAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_video);
        ButterKnife.bind(this);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayUseLogoEnabled(false);
        actionBar.setTitle(getIntent().getStringExtra(EXTRA));

        VideoPlayer.openTinyWhenOutScreen(this);

        context = getApplicationContext();
        listAdapter = new VideoListAdapter(context);

        lvContent.setAdapter(listAdapter);

        listAdapter.setItems(DataRepo.get(context).getVideoUrls());
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
}
