package com.phphy.control;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.phphy.widget.Rocker;
import com.phphy.widget.Video;

public class MainActivity extends Activity {
    ControlSocket controlSocket;
    Rocker rocker;
    Video video;
    ImageButton ring;
    ImageButton shutdown;

    private Toast mToast;
    private long exitTime;

    public void showToast(String text) {
        if (mToast == null) {
            mToast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        } else {
            mToast.setText(text);
            mToast.setDuration(Toast.LENGTH_SHORT);
        }
        mToast.show();
    }

    public void cancelToast() {
        if (mToast != null) {
            mToast.cancel();
        }
    }

    public void onBackPressed() {
        cancelToast();
        super.onBackPressed();
    }

    public Handler controlHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            showToast(msg.obj.toString());
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        controlSocket = ControlSocket.getInstance(controlHandler);

        super.onCreate(savedInstanceState);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_main);

        DisplayMetrics dm = getResources().getDisplayMetrics();
        controlSocket.screen_width = dm.widthPixels;
        controlSocket.screen_heigth = dm.heightPixels;

        rocker = (Rocker) findViewById(R.id.rocker);
        video = (Video) findViewById(R.id.video);
        ring = (ImageButton) findViewById(R.id.ring);
        shutdown = (ImageButton) findViewById(R.id.shutdown);

        rocker.setmRudderRadius(22);
        rocker.setmWheelRadius(59);
        Bitmap rocker_bg = BitmapFactory.decodeResource(getResources(), R.drawable.rocker_bg);
        Bitmap rocker_ctrl = BitmapFactory.decodeResource(getResources(), R.drawable.rocker_ctrl);
        rocker.setRockerBg(rocker_bg);
        rocker.setRockerCtrl(rocker_ctrl);
        rocker.setRudderListener(new Rocker.RudderListener() {

            @Override
            public void onSteeringWheelChanged(int action, int x, int y, int r) {
                controlSocket.x = x;
                controlSocket.y = y;
                controlSocket.r = r;
                if (action == Rocker.ACTION_START) {
                    controlSocket.isControl = true;
                } else if (action == Rocker.ACTION_STOP) {
                    controlSocket.stop();
                    controlSocket.isControl = false;
                }
            }
        });

        ring.setOnTouchListener(new Button.OnTouchListener() {
            public boolean onTouch(View view, MotionEvent event) {
                if (view.getId() == R.id.ring) {
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        controlSocket.ring(0);
                    }
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        controlSocket.ring(1);
                    }
                }
                return false;
            }
        });

        shutdown.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ((System.currentTimeMillis() - exitTime) > 2000) {
                    showToast("再按一次退出程序");
                    exitTime = System.currentTimeMillis();
                } else {
                    controlSocket.shutdown();
                    finish();
                    System.exit(0);
                }
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            if ((System.currentTimeMillis() - exitTime) > 2000) {
                showToast("再按一次退出程序");
                exitTime = System.currentTimeMillis();
            } else {
                finish();
                System.exit(0);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    protected void onStart() {
        super.onStart();
        controlSocket.isShowing = true;
    }

    protected void onStop() {
        super.onStop();
        controlSocket.isShowing = false;
    }

}