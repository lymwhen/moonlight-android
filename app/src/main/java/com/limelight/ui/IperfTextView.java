package com.limelight.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

public class IperfTextView extends TextView {

    public IperfTextView(Context context) {
        super(context);
        init(null, 0);
    }

    public IperfTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public IperfTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        setFocusable(true);
        setClickable(true);
    }
}