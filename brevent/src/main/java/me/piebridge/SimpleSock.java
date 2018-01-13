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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.Random;

/**
 * Created by thom on 2018/1/11.
 */
public class SimpleSock {

    private static final String TAG = "SimpleSock";

    private static final int BACKLOG = 50;

    private static final int PORT_BREVENT = 59527;

    private final int port;

    private final Server server;

    private final Random random;

    public SimpleSock() throws IOException {
        this(PORT_BREVENT);
    }

    public SimpleSock(int port) throws IOException {
        this.port = port;
        this.server = new Server(port);
        this.random = new SecureRandom();
        new Thread(this.server).start();
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
            i("[c-check] io exception", e);
            return false;
        }
    }

    public void quit() {
        server.quit();
        check();
    }

    static void i(String msg, Throwable t) {
        Log.i(TAG, msg, t);
    }

    static class Server implements Runnable {

        private final ServerSocket server;

        private volatile boolean quit;

        Server(int port) throws IOException {
            InetSocketAddress address = new InetSocketAddress(InetAddress.getLoopbackAddress(), port);
            this.server = new ServerSocket();
            this.server.setReuseAddress(true);
            this.server.bind(address, BACKLOG);
        }

        void quit() {
            this.quit = true;
        }

        @Override
        public void run() {
            while (!quit) {
                try (
                        Socket socket = server.accept();
                        DataInputStream is = new DataInputStream(socket.getInputStream());
                        DataOutputStream os = new DataOutputStream(socket.getOutputStream());
                ) {
                    os.writeInt(is.readInt());
                    os.flush();
                } catch (IOException e) {
                    i("[s-accept] io exception", e);
                }
            }
            try {
                server.close();
            } catch (IOException e) {
                i("[s-close] io exception", e);
            }
        }
    }

}