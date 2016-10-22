package com.phphy.control;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;

public class ControlSocket {
    private SendThread sendThread;
    private RecvThread recvThread;
    private Socket socket = null;
    private OutputStream outputStream;
    private InputStream inputStream;
    private Handler handler;
    private int block_size = 4096;
    private boolean isStopRecv = true;

    public int screen_width;
    public int screen_heigth;
    public BlockingQueue<Bitmap> queue;
    public AtomicInteger current_speed = new AtomicInteger(0);

    public boolean isShowing = false;
    public boolean isControl = false;
    public int x;
    public int y;
    public int r;

    private static ControlSocket controlSocket;

    private ControlSocket(Handler controlHandler) {
        handler = controlHandler;
        sendThread = new SendThread();
        recvThread = new RecvThread();
        new Thread(sendThread).start();
    }

    public static ControlSocket getInstance(Handler controlHandler) {
        if (controlSocket == null) {
            controlSocket = new ControlSocket(controlHandler);
        }
        return controlSocket;
    }

    public static ControlSocket getInstance() {
        return controlSocket;
    }

    private class SendThread implements Runnable {
        @Override
        public void run() {
            while (true) {
                if (isShowing) {
                    if (isControl) {
                        if (isClose()) {
                            close();
                            connect();
                        } else {
                            control(x, y, r);
                        }
                    }
                    startRecv();
                } else {
                    stopRecv();
                }

                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class RecvThread implements Runnable {
        @Override
        public void run() {
            connect();
            while (!isStopRecv) {
                download();
            }
        }
    }

    private static byte[] toHH(short n) {
        byte[] b = new byte[2];
        b[1] = (byte) (n & 0xff);
        b[0] = (byte) (n >> 8 & 0xff);
        return b;
    }

    /**
     * 连接socket服务器
     */
    private void connect() {
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(Constants.IP, Constants.PORT), Constants.TIMEOUT);
            outputStream = socket.getOutputStream();
            inputStream = socket.getInputStream();
        } catch (Exception e) {
            isControl = false;
            Message msg = new Message();
            msg.obj = "服务器连接失败";
            handler.sendMessage(msg);
            e.printStackTrace();
        }
    }

    /**
     * 判断是否断开连接
     */
    private Boolean isClose() {
        try {
            socket.sendUrgentData(0);
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 发送数据
     */
    private void send(byte[] data) {
        try {
            outputStream.write(data);
            outputStream.flush();
        } catch (Exception e) {
            Message msg = new Message();
            msg.obj = "数据发送超时";
            handler.sendMessage(msg);
            e.printStackTrace();
        }
    }

    /**
     * 关闭连接
     */
    private void close() {
        try {
            if (socket != null) {
                outputStream.close();
                socket.close();
                socket = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void download() {
        byte[] buffer = new byte[128 * 1024];
        int index = 0;
        try {
            for (; ; ) {
                index += inputStream.read(buffer, index, block_size);
                if (buffer[index - 2] == (byte) 0xFF && buffer[index - 1] == (byte) 0xD9 && buffer[0] == (byte) 0xFF && buffer[1] == (byte) 0xD8) {
                    Bitmap img = BitmapFactory.decodeStream(new ByteArrayInputStream(buffer, 0, index));
                    if (img != null) {
                        int width = screen_width;
                        int height = screen_heigth;

                        float rate_width = (float) width / (float) img.getWidth();
                        float rate_height = (float) height / (float) img.getHeight();

                        if (rate_width > rate_height)
                            width = (int) ((float) img.getWidth() * rate_height);
                        if (rate_width < rate_height)
                            height = (int) ((float) img.getHeight() * rate_width);

                        Bitmap bitmap = Bitmap.createScaledBitmap(img, width, height, false);
                        queue.add(bitmap);
                    }
                    break;
                }
            }
            current_speed.addAndGet(index);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void control(int x1, int y1, int r1) {
        //Log.d("xyr", "x:" + x1 + " y:" + y1 + " r:" + r1);
        byte type = 0x1;
        byte[] sendData = new byte[7];
        sendData[0] = type;
        System.arraycopy(toHH((short) x1), 0, sendData, 1, 2);
        System.arraycopy(toHH((short) y1), 0, sendData, 3, 2);
        System.arraycopy(toHH((short) r1), 0, sendData, 5, 2);

        send(sendData);
    }

    private void startRecv() {
        if (isStopRecv) {
            isStopRecv = false;
            queue = new LinkedBlockingQueue<>(32);
            new Thread(recvThread).start();
        }
    }

    private void stopRecv() {
        if (!isStopRecv) {
            isStopRecv = true;
            queue = null;
            close();
        }
    }

    public void ring(int shift) {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        byte type = 0x2;
        byte[] sendData = new byte[3];
        sendData[0] = type;
        System.arraycopy(toHH((short) shift), 0, sendData, 1, 2);

        send(sendData);
    }

    public void stop() {
        if (!isControl) return;
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        control(0, 0, 0);
    }

    public void shutdown() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        char type = 0xFF;
        byte[] sendData = new byte[1];
        sendData[0] = (byte) type;

        send(sendData);
        System.exit(0);
    }
}
