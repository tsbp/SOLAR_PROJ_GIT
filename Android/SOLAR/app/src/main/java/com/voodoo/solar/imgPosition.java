package com.voodoo.solar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by Voodoo on 16.11.2017.
 */

public class imgPosition extends View {
    Paint p;
    Rect rect;

    public static double azimuth;
    public static double elevation;

    public imgPosition(Context context, AttributeSet attrs) {
        super(context, attrs);

        p = new Paint();
        rect = new Rect();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // заливка канвы цветом
        canvas.drawARGB(0, 0, 0, 0);

        int AREA_WIDTH = canvas.getWidth();
        int AREA_HEIGH = canvas.getHeight();

        // настройка кисти
        // красный цвет
        p.setColor(Color.RED);
        // толщина линии = 10
        p.setStrokeWidth(3);
        // рисуем круг с центром в (100,200), радиус = 50
        //canvas.drawCircle(AREA_WIDTH / 2, AREA_HEIGH / 2, 10, p);

        double ang = elevation;
        int lineLength = AREA_WIDTH/2 -(int)(ang * AREA_WIDTH/(2*90));

        double angle = azimuth;
        angle = 180 - angle;
        angle = angle * Math.PI / 180;

        int startX = AREA_WIDTH / 2;
        int startY = AREA_HEIGH / 2;
        int endX   = (int)(AREA_WIDTH / 2 + lineLength * Math.sin(angle));
        int endY   = (int)(AREA_HEIGH / 2 + lineLength * Math.cos(angle));
        canvas.drawLine(startX, startY, endX, endY, p);

        p.setColor(Color.YELLOW);
        canvas.drawCircle(endX, endY, 15, p);

        p.setTextSize(15);
        p.setColor(Color.BLUE);
        String str = String.format("%.2f", elevation) + ", " + String.format("%.2f", azimuth);
        canvas.drawText(str, 15, (int)(AREA_HEIGH / 2.5), p);

    }
}

