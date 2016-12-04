package com.ilis.memoryoptimizer.activity;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.ilis.memoryoptimizer.R;
import com.ilis.memoryoptimizer.adapter.ProcessListAdapter;
import com.ilis.memoryoptimizer.modle.ProcessInfo;
import com.ilis.memoryoptimizer.util.ProcessInfoDiff;
import com.ilis.memoryoptimizer.util.ProcessInfoProvider;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.processCount)
    TextView processCount;
    @BindView(R.id.memoryStatus)
    TextView memoryStatus;
    @BindView(R.id.processList)
    RecyclerView processList;
    @BindView(R.id.refreshLayout)
    SwipeRefreshLayout refreshLayout;
    @BindView(R.id.listLabel)
    TextView listLabel;

    private List<ProcessInfo> processInfo = new ArrayList<>();
    private ProcessListAdapter adapter;

    private Observable<List<ProcessInfo>> updateAction =
            PublishSubject.create(new ObservableOnSubscribe<List<ProcessInfo>>() {
                @Override
                public void subscribe(ObservableEmitter<List<ProcessInfo>> e) throws Exception {
                    ProcessInfoProvider.updateProcessInfo(getBaseContext());
                    e.onNext(ProcessInfoProvider.getProcessInfo());
                    e.onComplete();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        initViews();
        setListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshLayout.post(new Runnable() {
            @Override
            public void run() {
                refreshLayout.setRefreshing(true);
                loadProcessInfo();
            }
        });
    }

    private void initViews() {
        refreshLayout.setColorSchemeResources(R.color.colorAccent);
        processList.setLayoutManager(new LinearLayoutManager(this));
        processList.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                outRect.left = (int) getResources().getDimension(R.dimen.item_offset);
                outRect.right = (int) getResources().getDimension(R.dimen.item_offset);
                outRect.bottom = (int) getResources().getDimension(R.dimen.item_offset);
            }
        });
        adapter = new ProcessListAdapter(this, processInfo);
        processList.setAdapter(adapter);
    }


    private void setListener() {
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadProcessInfo();
            }
        });

        processList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });
    }

    @OnClick({
            R.id.checkAll,
            R.id.checkOthers,
            R.id.killAll,
    })
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.checkAll:
                int size = processInfo.size();
                for (int i = 0; i < size; i++) {
                    ProcessInfo info = processInfo.get(i);
                    if (!info.isChecked()) {
                        info.setChecked(true);
                        adapter.notifyItemChanged(i);
                    }
                }
                break;
            case R.id.checkOthers:
                for (ProcessInfo info : processInfo) {
                    info.setChecked(!info.isChecked());
                }
                adapter.notifyItemRangeChanged(0, processInfo.size());
                break;
            case R.id.killAll:
                if (refreshLayout.isRefreshing()) {
                    return;
                }
                refreshLayout.setRefreshing(true);
                ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                for (ProcessInfo info : processInfo) {
                    if (info.getPackName().equals(getPackageName())) {
                        continue;
                    }
                    if (info.isChecked()) {
                        am.killBackgroundProcesses(info.getPackName());
                    }
                }
                loadProcessInfo();
                break;
        }
    }

    private void loadProcessInfo() {
        updateAction
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<List<ProcessInfo>>() {
                    @Override
                    public void accept(List<ProcessInfo> infos) throws Exception {
                        DiffUtil.DiffResult diffResult =
                                DiffUtil.calculateDiff(new ProcessInfoDiff(infos, processInfo));
                        processInfo.clear();
                        processInfo.addAll(infos);
                        diffResult.dispatchUpdatesTo(adapter);
                        processCount.setText(
                                String.format(
                                        getString(R.string.process_count),
                                        ProcessInfoProvider.getProcessCount()
                                ));
                        memoryStatus.setText(
                                String.format(
                                        getString(R.string.memory_status),
                                        ProcessInfoProvider.getSystemMemStatus()
                                ));
                        refreshLayout.setRefreshing(false);
                    }
                });
    }
}
