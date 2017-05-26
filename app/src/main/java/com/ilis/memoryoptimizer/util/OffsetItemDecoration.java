package com.ilis.memoryoptimizer.util;

import android.graphics.Rect;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.GridLayoutManager.SpanSizeLookup;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;

@SuppressWarnings("WeakerAccess")
public class OffsetItemDecoration<VH extends ViewHolder> extends RecyclerView.ItemDecoration {

    private int offset;
    private int centerOffset;
    private LinearLayoutManager layoutManager;

    public OffsetItemDecoration(int offset, LinearLayoutManager layoutManager) {
        this.offset = offset;
        this.centerOffset = offset;
        this.layoutManager = layoutManager;
    }

    public OffsetItemDecoration(int offset, int centerOffset, LinearLayoutManager layoutManager) {
        this.offset = offset;
        this.centerOffset = centerOffset;
        this.layoutManager = layoutManager;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        try {
            VH viewHolder = (VH) parent.getChildViewHolder(view);
            offsetItem(outRect, viewHolder);
        } catch (ClassCastException ignore) {
        }
    }

    protected void offsetItem(Rect outRect, VH viewHolder) {
        if (layoutManager instanceof GridLayoutManager) {
            int spanCount = ((GridLayoutManager) layoutManager).getSpanCount();
            SpanSizeLookup spanSizeLookup = ((GridLayoutManager) layoutManager).getSpanSizeLookup();

            if (layoutManager.getReverseLayout()) {
                if (spanSizeLookup.getSpanGroupIndex(getViewHolderPosition(viewHolder), spanCount) == 0) {
                    outRect.bottom = getOffset();
                }
                outRect.top = getOffset();
            } else {
                if (spanSizeLookup.getSpanGroupIndex(getViewHolderPosition(viewHolder), spanCount) == 0) {
                    outRect.top = getOffset();
                }
                outRect.bottom = getOffset();
            }

            if (spanSizeLookup.getSpanIndex(getViewHolderPosition(viewHolder), spanCount) == 0) {
                outRect.left = getOffset();
                outRect.right = getCenterOffset() / 2;
            } else if (spanSizeLookup.getSpanIndex(getViewHolderPosition(viewHolder), spanCount) == spanCount - 1) {
                outRect.left = getCenterOffset() / 2;
                outRect.right = getOffset();
            } else {
                outRect.left = getCenterOffset() / 2;
                outRect.right = getCenterOffset() / 2;
            }
        } else {
            if (layoutManager.getReverseLayout()) {
                if (getViewHolderPosition(viewHolder) == 0) {
                    outRect.bottom = getOffset();
                }
                outRect.top = getOffset();
            } else {
                if (getViewHolderPosition(viewHolder) == 0) {
                    outRect.top = getOffset();
                }
                outRect.bottom = getOffset();
            }

            outRect.left = getOffset();
            outRect.right = getOffset();
        }
    }

    protected int getOffset() {
        return offset;
    }

    protected int getCenterOffset() {
        return centerOffset;
    }

    protected int getViewHolderPosition(VH viewHolder) {
        return viewHolder.getAdapterPosition();
    }
}
