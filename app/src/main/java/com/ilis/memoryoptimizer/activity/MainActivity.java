package com.ilis.memoryoptimizer.activity;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import com.ilis.memoryoptimizer.R;
import com.ilis.memoryoptimizer.adapter.ProcessListAdapter;
import com.ilis.memoryoptimizer.modle.ProcessInfo;
import com.ilis.memoryoptimizer.util.ProcessInfoDiff;
import com.ilis.memoryoptimizer.util.ProcessInfoProvider;
import com.trello.rxlifecycle2.android.ActivityEvent;
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends RxAppCompatActivity {

    @BindView(R.id.processCount)
    TextView processCount;
    @BindView(R.id.memoryStatus)
    TextView memoryStatus;
    @BindView(R.id.processList)
    RecyclerView processList;
    @BindView(R.id.refreshLayout)
    SwipeRefreshLayout refreshLayout;

    private List<ProcessInfo> processInfo = new ArrayList<>();
    private ProcessListAdapter adapter;
    private LinearLayoutManager layoutManager;

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
        bindProcessList();
    }

    private void initViews() {
        refreshLayout.setColorSchemeResources(R.color.colorAccent);
        layoutManager = new LinearLayoutManager(this);
        processList.setLayoutManager(layoutManager);
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
                ProcessInfoProvider.update();
            }
        });
        refreshLayout.getViewTreeObserver()
                .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        refreshLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        refreshLayout.setRefreshing(true);
                        ProcessInfoProvider.update();
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
                adapter.notifyDataSetChanged();
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
                ProcessInfoProvider.update();
                break;
        }
    }

    private void bindProcessList() {
        ProcessInfoProvider.getProvider()
                .compose(this.<List<ProcessInfo>>bindUntilEvent(ActivityEvent.PAUSE))
                .observeOn(Schedulers.computation())
                .doOnNext(new Consumer<List<ProcessInfo>>() {
                    @Override
                    public void accept(List<ProcessInfo> newInfo) throws Exception {
                        for (ProcessInfo info : processInfo) {
                            if (info.isChecked()) {
                                for (ProcessInfo newOne : newInfo) {
                                    if (info.getPackName().equals(newOne.getPackName())) {
                                        newOne.setChecked(info.isChecked());
                                        break;
                                    }
                                }
                            }
                        }
                    }
                })
                .map(new Function<List<ProcessInfo>, DiffUtil.DiffResult>() {
                    @Override
                    public DiffUtil.DiffResult apply(List<ProcessInfo> newInfo) throws Exception {
                        DiffUtil.DiffResult diffResult =
                                DiffUtil.calculateDiff(new ProcessInfoDiff(newInfo, processInfo));
                        processList.setLayoutFrozen(true);
                        processInfo.clear();
                        processInfo.addAll(newInfo);
                        return diffResult;
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<DiffUtil.DiffResult>() {
                    @Override
                    public void accept(DiffUtil.DiffResult diffResult) throws Exception {
                        refreshLayout.setRefreshing(false);
                        processList.setLayoutFrozen(false);
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
                    }
                });
    }
}
