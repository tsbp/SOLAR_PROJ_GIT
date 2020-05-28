package com.voodoo.solar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.ImageView;

public class Compass2 extends ImageView {

    private Paint p;
    private int w, h;
    public static double elevation, azimuth;

    public Compass2(Context context,  AttributeSet attrs) {
        super(context, attrs);
        p = new Paint();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //super.onDraw(canvas);
        w = getWidth();
        h = getHeight();

        p.setColor(Color.BLACK);
        p.setStyle(Paint.Style.FILL);
        p.setStrokeWidth(5);
        canvas.drawRect(10, 10, w-10, h-10, p);

        p.setColor(0xff69fff0);
        p.setStrokeWidth(5);
        p.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(w/2, h / 2, w/2 - 50, p);
    }
}
