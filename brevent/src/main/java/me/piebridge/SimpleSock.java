/**
 * Copyright (c) 2018 Liu Dongmiao
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package me.piebridge;

import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.Random;

/**
 * Created by thom on 2018/1/11.
 */
public class SimpleSock implements AutoCloseable {

    private static final String TAG = "SimpleSock";

    private static final int PORT_BREVENT = 59527;

    private final int port;

    private final Server server;

    private final Random random;

    public SimpleSock(int port) throws IOException {
        this.port = port;
        server = new Server(port);
        random = new SecureRandom();
        Thread thread = new Thread(server);
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
        while (!server.canAccept()) {
            Thread.yield();
        }
    }

    public static SimpleSock newInstance() throws IOException {
        int port = PORT_BREVENT;
        while (true) {
            try {
                return new SimpleSock(port);
            } catch (BindException e) {
                i("port " + port + " " + e.getMessage());
                port++;
            }
        }
    }

    public boolean check() {
        try (
                Socket socket = new Socket(InetAddress.getLoopbackAddress(), port);
                DataOutputStream os = new DataOutputStream(socket.getOutputStream());
                DataInputStream is = new DataInputStream(socket.getInputStream())
        ) {
            int number = random.nextInt();
            os.writeInt(number);
            os.flush();
            return is.readInt() == number;
        } catch (IOException e) {
            i("[client] " + e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void close() throws IOException {
        server.close();
    }

    static void i(String msg) {
        System.out.println(msg);
    }

    static void i(String msg, Throwable t) {
        Log.i(TAG, msg, t);
    }

    static class Server implements Runnable, AutoCloseable {

        private static final int BACKLOG = 50;

        private final ServerSocket serverSocket;

        private volatile boolean closed;

        private volatile boolean accept;

        private final Object lock = new Object();

        Server(int port) throws IOException {
            InetSocketAddress address = new InetSocketAddress(InetAddress.getLoopbackAddress(), port);
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(address, BACKLOG);
        }

        @Override
        public void close() throws IOException {
            synchronized (lock) {
                closed = true;
                serverSocket.close();
            }
        }

        boolean isClosed() {
            synchronized (lock) {
                return closed;
            }
        }

        boolean canAccept() {
            synchronized (lock) {
                return accept;
            }
        }

        @Override
        public void run() {
            synchronized (lock) {
                accept = true;
            }
            while (!isClosed()) {
                try (
                        Socket socket = serverSocket.accept();
                        DataInputStream is = new DataInputStream(socket.getInputStream());
                        DataOutputStream os = new DataOutputStream(socket.getOutputStream())
                ) {
                    os.writeInt(is.readInt());
                    os.flush();
                } catch (IOException e) {
                    if (isClosed()) {
                        break;
                    } else {
                        i("[server] " + e.getMessage(), e);
                    }
                }
            }
        }
    }

}