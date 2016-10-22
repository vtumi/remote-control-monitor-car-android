package com.phphy.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.phphy.control.ControlSocket;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

public class Video extends SurfaceView implements Runnable, SurfaceHolder.Callback {
    private SurfaceHolder mHolder;
    private Canvas canvas;
    private Thread mThread;
    private ControlSocket controlSocket;

    private AtomicInteger temp_fps = new AtomicInteger(0);
    private AtomicInteger current_fps = new AtomicInteger(0);
    private AtomicInteger temp_speed = new AtomicInteger(0);

    private boolean isStop;

    public Video(Context context, AttributeSet as) {
        super(context, as);
        mHolder = getHolder();
        mHolder.addCallback(this);
        mThread = new Thread(this);

        controlSocket = ControlSocket.getInstance();

        // 创建定时器定时更新视频流帧速和下载速度
        new Timer().schedule(new TimerTask() {
            public void run() {
                temp_fps.set(current_fps.get());
                temp_speed.set(controlSocket.current_speed.get());

                current_fps.set(0);
                controlSocket.current_speed.set(0);
            }
        }, 1000, 1000);
    }

    @Override
    public void run() {
        Bitmap data;
        int width, height;
        String str_fps;
        final int TEXT_SEZE = 33;
        Paint pt = new Paint();
        pt.setAntiAlias(true);
        pt.setColor(Color.GREEN);
        pt.setTextSize(TEXT_SEZE);
        pt.setStrokeWidth(1);
        pt.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL));

        while (!isStop) {
            try {
                data = controlSocket.queue.take();

                if (data != null) {
                    width = data.getWidth();
                    height = data.getHeight();

                    canvas = mHolder.lockCanvas();
                    canvas.drawColor(Color.BLACK);
                    canvas.drawBitmap(data, (controlSocket.screen_width - width) / 2, (controlSocket.screen_heigth - height) / 2, null);

                    current_fps.addAndGet(1);
                    str_fps = String.format("FPS: [%2d] - SP: [%4d]KB/s", temp_fps.get(), temp_speed.get() / 1024);
                    canvas.drawText(str_fps, 2, TEXT_SEZE + 2, pt);

                    mHolder.unlockCanvasAndPost(canvas);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        isStop = false;
        new Thread(mThread).start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isStop = true;
    }

}

