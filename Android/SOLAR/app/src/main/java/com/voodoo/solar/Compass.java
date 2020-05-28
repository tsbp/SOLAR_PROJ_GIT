package com.voodoo.solar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.widget.ImageView;
import android.util.AttributeSet;

public class Compass  extends ImageView {

    private Paint p;
    private int w, h;
    public static double elevation, azimuth;

    //***********************************************************************************************

    public Compass(Context context, AttributeSet attrs) {
        super(context, attrs);
        p = new Paint();
    }


    @Override
    protected void onDraw(Canvas canvas) {
        //super.onDraw(canvas);

        w = getWidth();
        h = getHeight();

        int a;
        if (w >= h) a =h;
        else a = w;
        int compassSize = a/2 - 50;

        p.setColor(Color.BLACK);
        p.setStyle(Paint.Style.FILL);
        p.setStrokeWidth(compassSize / 20);
        canvas.drawRect(10, 10, w-10, h-10, p);

        p.setColor(0xff69fff0);
        p.setStrokeWidth(compassSize / 20);
        p.setStyle(Paint.Style.STROKE);


        canvas.drawCircle(w/2, h / 2, compassSize, p);



        p.setColor(Color.RED);
        // толщина линии = 10
        p.setStrokeWidth(5);
        double ang = /*Math.toDegrees*/(elevation);
        int lineLength = (int)(ang * compassSize / 90);

        double angle = /*Math.toDegrees*/(azimuth);
        angle = 180 - angle;
        angle = angle * Math.PI / 180;

        int startX = w / 2;
        int startY = h / 2;
        int endX   = (int)(w / 2 + lineLength * Math.sin(angle));
        int endY   = (int)(h / 2 + lineLength * Math.cos(angle));
        canvas.drawLine(startX, startY, endX, endY, p);


        int sunSize = compassSize/12;
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.YELLOW);
        canvas.drawCircle(endX, endY, sunSize, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(sunSize/5);
        p.setColor(Color.rgb(0xff, 0xA5, 0x00));
        canvas.drawCircle(endX, endY, sunSize+2, p);
        p.setColor(Color.RED);
        canvas.drawCircle(endX, endY, sunSize+8, p);
        p.setColor(Color.rgb(0xb2, 0x22, 0x22));
        canvas.drawCircle(endX, endY, sunSize+16, p);
    }


}
