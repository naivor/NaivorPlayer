package com.naivor.sample.activity.category;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.naivor.sample.R;
import com.naivor.sample.activity.MainActivity;
import com.naivor.sample.activity.category.list.ListVideoActivity;
import com.naivor.sample.activity.category.list.RecyclerViewVideoActivity;
import com.naivor.sample.utils.ToastUtil;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.naivor.sample.activity.MainActivity.EXTRA;


/**
 *
 */
public class ListActivity extends AppCompatActivity {

    @BindView(R.id.btn_listview)
    Button btnListview;
    @BindView(R.id.btn_recyclerview)
    Button btnRecyclerview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_list);
        ButterKnife.bind(this);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayUseLogoEnabled(false);
        actionBar.setTitle(getIntent().getStringExtra(EXTRA));

    }


    /**
     * 点击监听
     *
     * @param v
     */
    @OnClick({
            R.id.btn_listview, R.id.btn_recyclerview})
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_listview:
                startActivity(new Intent(this, ListVideoActivity.class).putExtra(EXTRA,
                        btnListview.getText().toString()));
                break;
            case R.id.btn_recyclerview:
                startActivity(new Intent(this, RecyclerViewVideoActivity.class).putExtra(EXTRA,
                        btnRecyclerview.getText().toString()));
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
}
