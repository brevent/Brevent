package me.piebridge.brevent.ui;

import android.util.Base64;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import me.piebridge.brevent.BuildConfig;

/**
 * Created by thom on 2017/7/20.
 */
public class SimpleAdb {

    private static final String TAG = "BreventADB";

    public static final int A_SYNC = 0x434e5953;
    public static final int A_CNXN = 0x4e584e43;
    public static final int A_OPEN = 0x4e45504f;
    public static final int A_OKAY = 0x59414b4f;
    public static final int A_CLSE = 0x45534c43;
    public static final int A_WRTE = 0x45545257;
    public static final int A_AUTH = 0x48545541;

    public static final int A_VERSION = 0x01000000;
    public static final int MAX_PAYLOAD = 4096;

    private static final int TRIED_NONE = 0;
    private static final int TRIED_SIGNED = 1;
    private static final int TRIED_PUBKEY = 2;

    private static final int ADB_AUTH_SIGNATURE = 2;
    private static final int ADB_AUTH_PUBLICKEY = 3;

    private static final int HEAD_LENGTH = 24;

    private final Socket socket;

    private final OutputStream os;

    private final InputStream is;

    private final String command;

    private int auth = TRIED_NONE;

    public SimpleAdb(int port, String path) throws IOException {
        socket = new Socket(InetAddress.getByAddress("localhost", new byte[] {127, 0, 0, 1}), port);
        os = new BufferedOutputStream(socket.getOutputStream());
        is = new BufferedInputStream(socket.getInputStream());
        command = "shell:sh " + path;
        d("command: " + path);
    }

    private int sendMessage(Message message) throws IOException {
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
        d("send, command: " + command(message.command) + ", length: " + length);
        if (length > 0) {
            buffer.put(message.data);
        }
        os.write(buffer.array());
        os.flush();
        return 0;
    }

    private Message readMessage() throws IOException {
        byte[] head = new byte[HEAD_LENGTH];
        if (is.read(head) != head.length) {
            e("recv head, length too short");
            throw new AdbException();
        }
        d("recv, head: " + Arrays.toString(head));
        ByteBuffer buffer = ByteBuffer.wrap(head);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        Message message = new Message();
        message.command = buffer.getInt();
        message.arg0 = buffer.getInt();
        message.arg1 = buffer.getInt();
        message.length = buffer.getInt();
        message.check = buffer.getInt();
        message.magic = buffer.getInt();
        d("recv, command: " + command(message.command)
                + ", arg0: " + message.arg0 + ", arg1: " + message.arg1);
        if (message.length > 0) {
            byte[] body = new byte[message.length];
            if (is.read(body) != body.length) {
                e("recv data, length too short");
                throw new AdbException();
            }
            message.data = body;
            if (message.command == A_AUTH) {
                d("recv, data: " + Arrays.toString(body));
            } else {
                d("recv: data: " + new String(body, "UTF-8"));
            }
        }
        return message;
    }

    private byte[] formatBytes(String s) throws UnsupportedEncodingException {
        byte[] bytes = s.getBytes("UTF-8");
        return Arrays.copyOf(bytes, bytes.length + 1);
    }

    public String run() throws IOException {
        try {
            return comm();
        } finally {
            os.close();
            is.close();
            socket.close();
        }
    }

    private String comm() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Message message;
        sendMessage(new Message(A_CNXN, A_VERSION, MAX_PAYLOAD, formatBytes("host::brevent")));
        message = readMessage();
        while (message.command != A_CNXN) {
            if (message.command != A_AUTH) {
                return null;
            } else if (auth == TRIED_NONE) {
                sendMessage(new Message(A_AUTH, ADB_AUTH_SIGNATURE, 0, auth(message.data)));
                message = readMessage();
                this.auth = TRIED_SIGNED;
            } else if (auth == TRIED_SIGNED) {
                sendMessage(new Message(A_AUTH, ADB_AUTH_PUBLICKEY, 0, generatePublicKey()));
                message = readMessage();
                auth = TRIED_PUBKEY;
            } else if (auth == TRIED_PUBKEY) {
                return null;
            }
        }
        sendMessage(new Message(A_OPEN, 1, 0, formatBytes(command)));
        message = readMessage();
        if (message.command == A_OKAY) {
            int remote = message.arg0;
            while (true) {
                try {
                    message = readMessage();
                } catch (AdbException e) {
                    break;
                }
                if (message.command == A_CLSE) {
                    sendMessage(new Message(A_CLSE, 1, remote, new byte[0]));
                    break;
                } else if (message.command == A_WRTE) {
                    if (message.length > 0) {
                        baos.write(message.data);
                    }
                    sendMessage(new Message(A_OKAY, 1, message.arg0, new byte[0]));
                }
            }
        } else if (message.command == A_CLSE) {
            sendMessage(new Message(A_CLSE, 1, message.arg0, new byte[0]));
        }
        return baos.toString("UTF-8");
    }

    private byte[] generatePublicKey() {
        byte[] encode = Base64.encode(BuildConfig.ADB_K, Base64.NO_WRAP);
        return Arrays.copyOf(encode, encode.length + 1);
    }

    private byte[] auth(byte[] token) {
        byte[] bytes = new byte[0x100];
        System.arraycopy(BuildConfig.ADB_P, 0, bytes, 0, BuildConfig.ADB_P.length);
        System.arraycopy(token, 0, bytes, BuildConfig.ADB_P.length, token.length);
        BigInteger a = new BigInteger(bytes);
        BigInteger b = a.modPow(new BigInteger(BuildConfig.ADB_D), new BigInteger(BuildConfig.ADB_M));
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

    private String command(int command) {
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

    static void d(String msg) {
        Log.d(TAG, msg);
    }

    static void e(String msg) {
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

    private static class AdbException extends IOException {
    }
}