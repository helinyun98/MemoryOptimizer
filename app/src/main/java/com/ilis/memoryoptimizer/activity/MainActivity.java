package com.ilis.memoryoptimizer.activity;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.ilis.memoryoptimizer.R;
import com.ilis.memoryoptimizer.adapter.ProcessListAdapter;
import com.ilis.memoryoptimizer.data.ProcessInfo;
import com.ilis.memoryoptimizer.data.ProcessInfoRepository;
import com.ilis.memoryoptimizer.util.OffsetItemDecoration;
import com.ilis.memoryoptimizer.util.ToastUtil;
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends RxAppCompatActivity implements ProcessListView {

    @BindView(R.id.processCount)
    TextView processCount;
    @BindView(R.id.memoryStatus)
    TextView memoryStatus;
    @BindView(R.id.processList)
    RecyclerView processList;
    @BindView(R.id.refreshLayout)
    SwipeRefreshLayout refreshLayout;

    private ProcessListAdapter adapter;
    private LinearLayoutManager layoutManager;
    private PrecessListPresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        presenter = new PrecessListPresenter(ProcessInfoRepository.getInstance(), this);

        refreshLayout.setColorSchemeResources(R.color.colorAccent);
        refreshLayout.setOnRefreshListener(() -> presenter.refresh());
        refreshLayout.post(this::showReloading);

        layoutManager = new LinearLayoutManager(this);
        processList.setLayoutManager(layoutManager);

        int offset = (int) getResources().getDimension(R.dimen.item_offset);
        processList.addItemDecoration(new OffsetItemDecoration<>(offset, layoutManager));
        processList.getItemAnimator().setChangeDuration(0);

        adapter = new ProcessListAdapter(this);
        processList.setAdapter(adapter);
    }

    @OnClick({
            R.id.selectAll,
            R.id.selectOthers,
            R.id.killAllSelect})
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.selectAll:
                presenter.selectAll();
                break;
            case R.id.selectOthers:
                presenter.selectOther();
                break;
            case R.id.killAllSelect:
                presenter.cleanupAllSelected();
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        presenter.refresh();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        presenter.destroy();
    }

    @Override
    public void showReloading() {
        refreshLayout.setRefreshing(true);
    }

    @Override
    public void hideReloading() {
        refreshLayout.setRefreshing(false);
    }

    @Override
    public void showProcess(List<ProcessInfo> infoList) {
        adapter.setData(infoList);
    }

    @Override
    public void notifyListChange() {
        adapter.notifyListChange();
    }

    @Override
    public void showMemStatus(String MemStatus) {
        String formatMemStatus = String.format(getString(R.string.memory_status), MemStatus);
        memoryStatus.setText(formatMemStatus);
    }

    @Override
    public void showProcessCount(int count) {
        String formatCount = String.format(getString(R.string.process_count), count);
        processCount.setText(formatCount);
    }

    @Override
    public void showToast(String text) {
        ToastUtil.showToast(text);
    }
}
