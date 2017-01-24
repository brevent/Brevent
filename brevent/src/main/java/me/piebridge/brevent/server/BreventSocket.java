package me.piebridge.brevent.server;

import android.os.Handler;
import android.os.Process;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.CountDownLatch;

import me.piebridge.brevent.BuildConfig;
import me.piebridge.brevent.protocol.BreventProtocol;

/**
 * Socket Handler
 * <p>
 * Created by thom on 2017/2/13.
 */
class BreventSocket implements Runnable {

    private final Handler mHandler;

    private final ServerSocket mServerSocket;

    private final CountDownLatch mCountDownLatch;

    BreventSocket(Handler handler, ServerSocket serverSocket, CountDownLatch countDownLatch) {
        mHandler = handler;
        mServerSocket = serverSocket;
        mCountDownLatch = countDownLatch;
    }

    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        for (; ; ) {
            try {
                Socket socket = mServerSocket.accept();
                handle(socket);
                socket.close();
            } catch (IOException e) {
                if (SocketException.class.equals(e.getClass()) && "Socket closed".equals(e.getMessage())) {
                    ServerLog.i("server socket is closed");
                    break;
                }
                ServerLog.w("cannot accept", e);
            }
        }
        ServerLog.d("Brevent Socket countDown");
        mCountDownLatch.countDown();
    }

    private void handle(Socket socket) throws IOException {
        DataInputStream is = new DataInputStream(socket.getInputStream());
        short size = is.readShort();
        byte[] bytes = new byte[size];
        int length;
        int offset = 0;
        int remain = bytes.length;
        while (remain > 0 && (length = is.read(bytes, offset, remain)) != -1) {
            if (length > 0) {
                offset += length;
                remain -= length;
            }
        }
        BreventProtocol request = BreventProtocol.unwrap(bytes);
        if (request != null) {
            if (!BuildConfig.RELEASE) {
                ServerLog.d("request: " + request.getClass().getSimpleName() + ", " +
                        request.toString());
            }
            int message = request.getAction() == BreventProtocol.STATUS_REQUEST ? BreventServer.MESSAGE_REQUEST_STATUS : BreventServer.MESSAGE_REQUEST_MANAGE;
            mHandler.obtainMessage(message, request).sendToTarget();
        }
        is.close();
    }

}
