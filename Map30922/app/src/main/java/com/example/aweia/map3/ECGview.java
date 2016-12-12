package com.example.aweia.map3;

/**
 * Created by aweia on 2015/12/28.
 */
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;



public class ECGview extends View {


    int Max_x = 700;
    int level = 300;
    int compression = 3;

    Queue<Point> path = new LinkedList<>();                 //存放傳進來的點
    ArrayList<Point> draw_point = new ArrayList<>();              //儲存這次要畫的所有點與點

    int x = 0;
    Handler Inva_handler;       //繪圖也算一個在非主UI改主UI的事件，因此也需要使用Handler

    public ECGview(Context context) {
        super(context);
        Inva_handler = new Handler();
    }

    public Runnable runnable = new Runnable() {
        @Override
        public void run() {
            invalidate();
        }
    };

    public void setLevel(int level) {
        this.level += level;
    }

    public void setCompression(int compression) {
        if ((this.compression + compression) != 0)
            this.compression += compression;
    }

    public void addValue(int data) {
        //   path.offer(new Point(x += 2, 300 - (data / 2)));     //將傳進來的資料放到path柱列裡面，再做一些運算
        draw_point.add(new Point(x += 2, level - (data / compression)));     //將傳進來的資料放到path柱列裡面，再做一些運算
        if (x == Max_x) x = 0;                                 //當Max_x等於X軸的最大距離 Max_x歸零
        Inva_handler.post(runnable);                         //呼叫重繪的執行緒
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Paint paint = new Paint();

        canvas.drawColor(Color.BLACK);


        try {
            //     draw_point.add(path.remove());               //將上一次存入的
            paint.setARGB(255, 0, 255, 0);


            Point pre_pt;
            Point this_pt = draw_point.get(0);

            for (int i = 0; i < draw_point.size(); i++) {
                pre_pt = this_pt;
                this_pt = draw_point.get(i);
                canvas.drawLine(pre_pt.x, pre_pt.y, this_pt.x, this_pt.y, paint);

                if (this_pt.x == 700) {
                    draw_point.clear();
                    invalidate();
                }
            }
        } catch (IndexOutOfBoundsException e) {
            Log.e("IndexOut", path.size() + "");
        } catch (Exception ex) {
            Log.e("onDraw Excetion", ex.toString() + "Line:" + ex.getStackTrace()[0].getLineNumber());
        }
        //Debug.stopMethodTracing();
    }
}

