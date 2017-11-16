package com.voodoo.sunpos;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by Voodoo on 16.11.2017.
 */

public class imagePosition extends View {
    Paint p;
    Rect rect;

    public imagePosition(Context context, AttributeSet attrs) {
        super(context, attrs);

        p = new Paint();
        rect = new Rect();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // заливка канвы цветом
        canvas.drawARGB(80, 102, 204, 255);

        // настройка кисти
        // красный цвет
        p.setColor(Color.RED);
        // толщина линии = 10
        p.setStrokeWidth(10);
        // рисуем круг с центром в (100,200), радиус = 50
        canvas.drawCircle(100, 200, 50, p);
    }
}
