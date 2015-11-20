package com.amemusic.mymusic;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.widget.Button;

/**
 * Created by klentz on 11/22/15.
 */
public class play_button_t extends Button {

    /**
     * Created by klentz on 11/21/15.
     */
    public play_button_t(Context context, AttributeSet attrs) {
        super(context, attrs);

    }

    @Override
    protected void onDraw(Canvas canvas) {

        Paint paint = new Paint();
        paint.setColor(0xff22bb22);
        paint.setStrokeWidth(10);
        paint.setStyle(Paint.Style.FILL);
        int w = getWidth();
        int h = getHeight();

        Path path = new Path();
        path.setFillType(Path.FillType.EVEN_ODD);
        path.moveTo(w * 0.25f, h * 0.25f);
        path.lineTo(w * 0.75f, h * 0.50f);
        path.lineTo(w * 0.25f, h * 0.75f);
        path.lineTo(w * 0.25f, h * 0.25f);
        path.close();

        canvas.drawPath(path, paint);
    }

}
