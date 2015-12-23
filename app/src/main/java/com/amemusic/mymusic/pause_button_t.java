package com.amemusic.mymusic;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.widget.Button;

/**
 * Created by klentz on 12/22/15.
 */
public class pause_button_t extends Button {

    public pause_button_t(Context context, AttributeSet attrs) {
        super(context, attrs);

    }

    @Override
    protected void onDraw(Canvas canvas) {

        Paint paint = new Paint();
        paint.setColor(0xffddbb00);
        paint.setStrokeWidth(10);
        paint.setStyle(Paint.Style.FILL);
        int w = getWidth();
        int h = getHeight();

        canvas.drawRect(w * 0.15f, h * 0.25f, w * 0.35f, h * 0.75f, paint);
        canvas.drawRect(w * 0.65f, h * 0.25f, w * 0.85f, h * 0.75f, paint);
     }
}
