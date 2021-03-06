package com.view.kimdata;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ScrollView;

public class NoteVerticalScrollView extends ScrollView {
    private boolean m_isLockScroll;

    public NoteVerticalScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);

        m_isLockScroll = false;
    }

    public void setLockScroll(boolean lock) {
        m_isLockScroll = lock;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (m_isLockScroll) {
            return false;
        }

        return super.onInterceptTouchEvent(ev);
    }
}
