package com.voodoo.solar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

/**
 * Created by Voodoo on 16.11.2017.
 */

public class imgPosition extends ImageView {
    Paint p;

    public static double azimuth;
    public static double elevation;

    public imgPosition(Context context, AttributeSet attrs) {
        super(context, attrs);

        p = new Paint();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // заливка канвы цветом
//        canvas.drawARGB(0, 0, 0, 0);

        int AREA_WIDTH = canvas.getWidth();
        int AREA_HEIGH = AREA_WIDTH/*canvas.getHeight()*/;

        // настройка кисти
        // красный цвет
        p.setColor(Color.RED);
        // толщина линии = 10
        p.setStrokeWidth(5);
        // рисуем круг с центром в (100,200), радиус = 50
        //canvas.drawCircle(AREA_WIDTH / 2, AREA_HEIGH / 2, 10, p);

        double ang = /*Math.toDegrees*/(elevation);
        int lineLength = (int)(ang * AREA_WIDTH/(2*90));

        double angle = /*Math.toDegrees*/(azimuth);
        angle = 180 - angle;
        angle = angle * Math.PI / 180;

        int startX = AREA_WIDTH / 2;
        int startY = AREA_HEIGH / 2;
        int endX   = (int)(AREA_WIDTH / 2 + lineLength * Math.sin(angle));
        int endY   = (int)(AREA_HEIGH / 2 + lineLength * Math.cos(angle));
        canvas.drawLine(startX, startY, endX, endY, p);



        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.YELLOW);
        canvas.drawCircle(endX, endY, 20, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(8);
        p.setColor(Color.rgb(0xff, 0xA5, 0x00));
        canvas.drawCircle(endX, endY, 20+2, p);
        p.setColor(Color.RED);
        canvas.drawCircle(endX, endY, 20+8, p);
        p.setColor(Color.rgb(0xb2, 0x22, 0x22));
        canvas.drawCircle(endX, endY, 20+16, p);

//        p.setTextSize(15);
//        p.setColor(Color.BLUE);
//        String str = String.format("%.2f", ang) + ", " + String.format("%.2f", angle);
//        canvas.drawText(str, 15, (int)(AREA_HEIGH / 2.5), p);

    }
}

