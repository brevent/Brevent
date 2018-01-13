/**
 * Copyright (c) 2017 Liu Dongmiao
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

import android.util.Base64;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Created by thom on 2017/7/20.
 * Refactored by thom on 2017/11/15.
 */
public class SimpleAdb {

    private static final String TAG = "SimpleAdb";

    private static final int A_SYNC = 0x434e5953;
    private static final int A_CNXN = 0x4e584e43;
    private static final int A_OPEN = 0x4e45504f;
    private static final int A_OKAY = 0x59414b4f;
    private static final int A_CLSE = 0x45534c43;
    private static final int A_WRTE = 0x45545257;
    private static final int A_AUTH = 0x48545541;

    private static final int A_VERSION = 0x01000000;
    private static final int MAX_PAYLOAD = 4096;

    private static final int TRIED_NONE = 0;
    private static final int TRIED_SIGNED = 1;
    private static final int TRIED_PUBKEY = 2;

    private static final int ADB_AUTH_SIGNATURE = 2;
    private static final int ADB_AUTH_PUBLICKEY = 3;

    private static final int HEAD_LENGTH = 24;

    private static final byte[] EMP = {0x00, 0x01,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            0x00,
            0x30, 0x21, 0x30, 0x09, 0x06, 0x05, 0x2b, 0x0e, 0x03, 0x02, 0x1a, 0x05, 0x00, 0x04, 0x14};

    private final BigInteger d;

    private final BigInteger m;

    private final byte[] k;

    private int auth = TRIED_NONE;

    /**
     * 1. generate adbkey
     * shell> adb keygen adbkey
     * 2. for pub (base64 + "\x00 " + host, base64 is enough)
     * shell> cat adbkey.pub
     * 3. for privateExponent and modulus
     * shell> openssl rsa -in adbkey -text -noout
     *
     * @param pub             pubkey, encode as base64
     * @param modulus         modulus, part of rsa key
     * @param privateExponent private exponent, rsa private key
     */
    public SimpleAdb(String pub, String modulus, String privateExponent) {
        this.k = formatBytes(pub);
        this.m = convert(modulus);
        this.d = convert(privateExponent);
    }

    /**
     * @param k pubkey
     * @param m modulus, part of rsa key
     * @param d private exponent, rsa private key
     */
    public SimpleAdb(byte[] k, byte[] m, byte[] d) {
        this.k = formatBytes(Base64.encode(k, Base64.NO_WRAP));
        this.m = new BigInteger(1, m);
        this.d = new BigInteger(1, d);
    }

    private static BigInteger convert(String s) {
        return new BigInteger(s.replaceAll("[ :\n]", ""), 0x10);
    }

    private static void send(OutputStream os, Message message) throws IOException {
        int sum = 0;
        if (message.data == null) {
            message.data = new byte[0];
        }
        for (byte b : message.data) {
            sum += (b & 0xff);
        }
        int length = message.data.length;
        ByteBuffer buffer = ByteBuffer.allocate(HEAD_LENGTH + length);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(message.command);
        buffer.putInt(message.arg0);
        buffer.putInt(message.arg1);
        buffer.putInt(length);
        buffer.putInt(sum);
        buffer.putInt(~message.command);
        i("send, command: " + command(message.command) + ", length: " + length);
        if (length > 0) {
            buffer.put(message.data);
        }
        os.write(buffer.array());
        os.flush();
    }

    private static Message recv(InputStream is) throws IOException {
        byte[] head = new byte[HEAD_LENGTH];
        if (is.read(head) != head.length) {
            e("recv head, length too short");
            throw new AdbException();
        }
        i("recv, head: " + Arrays.toString(head));
        ByteBuffer buffer = ByteBuffer.wrap(head);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        Message message = new Message();
        message.command = buffer.getInt();
        message.arg0 = buffer.getInt();
        message.arg1 = buffer.getInt();
        message.length = buffer.getInt();
        message.check = buffer.getInt();
        message.magic = buffer.getInt();
        i("recv, command: " + command(message.command)
                + ", arg0: " + message.arg0 + ", arg1: " + message.arg1);
        if (message.length > 0) {
            byte[] body = new byte[message.length];
            if (is.read(body) != body.length) {
                e("recv data, length too short");
                throw new AdbException();
            }
            message.data = body;
            if (message.command == A_AUTH) {
                i("recv, data: " + Arrays.toString(body));
            } else {
                i("recv: data: " + new String(body, "UTF-8"));
            }
        }
        return message;
    }

    private static byte[] formatBytes(byte[] bytes) {
        return Arrays.copyOf(bytes, bytes.length + 1);
    }

    private static byte[] formatBytes(String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        return Arrays.copyOf(bytes, bytes.length + 1);
    }

    public String exec(int port, String command) throws IOException {
        InetAddress localhost = InetAddress.getByAddress("localhost", new byte[] {127, 0, 0, 1});
        try (
                Socket socket = new Socket(localhost, port);
                OutputStream os = new BufferedOutputStream(socket.getOutputStream());
                InputStream is = new BufferedInputStream(socket.getInputStream())
        ) {
            return comm(os, is, command);
        }
    }

    private String comm(OutputStream os, InputStream is, String command) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Message message;
        send(os, new Message(A_CNXN, A_VERSION, MAX_PAYLOAD, formatBytes("host::brevent")));
        message = recv(is);
        while (message.command != A_CNXN) {
            if (message.command != A_AUTH) {
                return null;
            } else if (auth == TRIED_NONE) {
                send(os, new Message(A_AUTH, ADB_AUTH_SIGNATURE, 0, auth(message.data)));
                message = recv(is);
                this.auth = TRIED_SIGNED;
            } else if (auth == TRIED_SIGNED) {
                send(os, new Message(A_AUTH, ADB_AUTH_PUBLICKEY, 0, this.k));
                message = recv(is);
                auth = TRIED_PUBKEY;
            } else if (auth == TRIED_PUBKEY) {
                return null;
            }
        }
        send(os, new Message(A_OPEN, 1, 0, formatBytes("shell:" + command)));
        message = recv(is);
        if (message.command == A_OKAY) {
            try {
                readResult(os, is, baos);
            } catch (AdbException e) {
                // do nothing
            }
        } else if (message.command == A_CLSE) {
            send(os, new Message(A_CLSE, 1, message.arg0, new byte[0]));
        }
        return baos.toString("UTF-8");
    }

    private void readResult(OutputStream os, InputStream is, OutputStream bs) throws IOException {
        while (true) {
            Message message = recv(is);
            if (message.command == A_CLSE) {
                send(os, new Message(A_CLSE, 1, message.arg0, new byte[0]));
                break;
            } else if (message.command == A_WRTE) {
                if (message.length > 0) {
                    bs.write(message.data);
                }
                send(os, new Message(A_OKAY, 1, message.arg0, new byte[0]));
            }
        }
    }

    private byte[] auth(byte[] token) {
        byte[] bytes = Arrays.copyOf(EMP, 0x100);
        System.arraycopy(token, 0, bytes, 236, 20);
        BigInteger a = new BigInteger(bytes);
        BigInteger b = a.modPow(this.d, this.m);
        byte[] c = b.toByteArray();
        if (c.length > bytes.length) {
            int offset = c.length - bytes.length;
            System.arraycopy(c, offset, bytes, 0, bytes.length);
        } else {
            int offset = bytes.length - c.length;
            System.arraycopy(c, 0, bytes, offset, c.length);
        }
        return bytes;
    }

    private static String command(int command) {
        switch (command) {
            case A_SYNC:
                return "SYNC";
            case A_CNXN:
                return "CNXN";
            case A_OPEN:
                return "OPEN";
            case A_OKAY:
                return "OKAY";
            case A_CLSE:
                return "CLSE";
            case A_WRTE:
                return "WRTE";
            case A_AUTH:
                return "AUTH";
            default:
                return "XXXX";
        }
    }

    private static void i(String msg) {
        Log.i(TAG, msg);
    }

    private static void e(String msg) {
        Log.e(TAG, msg);
    }

    private static class Message {
        int command;
        int arg0;
        int arg1;
        int length;
        int check;
        int magic;
        byte[] data;

        Message() {

        }

        Message(int command, int arg0, int arg1, byte[] data) {
            this.command = command;
            this.arg0 = arg0;
            this.arg1 = arg1;
            this.data = data;
        }
    }

    static class AdbException extends IOException {
    }

}