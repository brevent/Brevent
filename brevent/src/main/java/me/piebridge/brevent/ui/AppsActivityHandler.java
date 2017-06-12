package me.piebridge.brevent.ui;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.WorkerThread;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;

import me.piebridge.brevent.protocol.BreventProtocol;
import me.piebridge.brevent.protocol.BreventRequest;

/**
 * Created by thom on 2017/2/3.
 */
public class AppsActivityHandler extends Handler {

    private static final long DELAY = 15000;

    private static final int TIMEOUT = 5000;

    private static final int RETRY = 1000;

    private final Handler uiHandler;

    private final WeakReference<BreventActivity> mReference;

    private boolean hasResponse;

    public AppsActivityHandler(BreventActivity activity, Handler handler) {
        super(newLooper());
        mReference = new WeakReference<>(activity);
        uiHandler = handler;
    }

    private static Looper newLooper() {
        HandlerThread thread = new HandlerThread("Brevent");
        thread.start();
        return thread.getLooper();
    }

    @Override
    public void handleMessage(Message message) {
        switch (message.what) {
            case BreventActivity.MESSAGE_RETRIEVE:
                UILog.d("request status");
                uiHandler.sendEmptyMessage(BreventActivity.UI_MESSAGE_SHOW_PROGRESS);
            case BreventActivity.MESSAGE_RETRIEVE2:
                removeMessages(BreventActivity.MESSAGE_BREVENT_NO_RESPONSE);
                requestStatus(false);
                break;
            case BreventActivity.MESSAGE_RETRIEVE3:
                UILog.d("retry request status");
                requestStatus(true);
                break;
            case BreventActivity.MESSAGE_BREVENT_RESPONSE:
                if (!hasResponse) {
                    UILog.d("received response");
                    hasResponse = true;
                    hideNotification();
                }
                removeMessages(BreventActivity.MESSAGE_RETRIEVE3);
                removeMessages(BreventActivity.MESSAGE_BREVENT_NO_RESPONSE);
                BreventProtocol response = (BreventProtocol) message.obj;
                BreventActivity activity = mReference.get();
                if (activity != null && !activity.isStopped()) {
                    activity.onBreventResponse(response);
                }
                uiHandler.sendEmptyMessage(BreventActivity.UI_MESSAGE_HIDE_DISABLED);
                break;
            case BreventActivity.MESSAGE_BREVENT_NO_RESPONSE:
                uiHandler.sendEmptyMessage(BreventActivity.UI_MESSAGE_NO_BREVENT_DATA);
                break;
            case BreventActivity.MESSAGE_BREVENT_REQUEST:
                send((BreventProtocol) message.obj);
                break;
        }
    }

    private void requestStatus(boolean retry) {
        BreventRequest request = new BreventRequest();
        request.retry = retry;
        send(request);
    }

    private void hideNotification() {
        BreventActivity breventActivity = mReference.get();
        if (breventActivity != null) {
            Context context = breventActivity.getApplicationContext();
            ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                    .cancel(BreventIntentService.ID);
        }
    }

    @WorkerThread
    private void send(BreventProtocol message) {
        int action = message.getAction();
        try {
            Socket socket = new Socket(InetAddress.getLoopbackAddress(), BreventProtocol.PORT);
            if (action == BreventProtocol.STATUS_REQUEST) {
                socket.setSoTimeout(TIMEOUT);
            }
            DataOutputStream os = new DataOutputStream(socket.getOutputStream());
            BreventProtocol.writeTo(message, os);
            os.flush();
            if (action != BreventProtocol.STATUS_REQUEST) {
                sendEmptyMessageDelayed(BreventActivity.MESSAGE_BREVENT_NO_RESPONSE, DELAY);
            } else {
                sendEmptyMessageDelayed(BreventActivity.MESSAGE_BREVENT_NO_RESPONSE, DELAY * 2);
            }

            DataInputStream is = new DataInputStream(socket.getInputStream());
            BreventProtocol response = BreventProtocol.readFrom(is);
            obtainMessage(BreventActivity.MESSAGE_BREVENT_RESPONSE, response).sendToTarget();

            os.close();
            is.close();
            socket.close();
        } catch (ConnectException e) {
            UILog.v("cannot connect to localhost:" + BreventProtocol.PORT, e);
            if (!message.retry) {
                uiHandler.sendEmptyMessage(BreventActivity.UI_MESSAGE_NO_BREVENT);
            }
        } catch (IOException e) {
            UILog.v("cannot connect to localhost:" + BreventProtocol.PORT, e);
            uiHandler.obtainMessage(BreventActivity.UI_MESSAGE_IO_BREVENT, e).sendToTarget();
        }
        if (action == BreventProtocol.STATUS_REQUEST) {
            sendEmptyMessageDelayed(BreventActivity.MESSAGE_RETRIEVE3,
                    AppsDisabledFragment.isEmulator() ? TIMEOUT : RETRY);
        }
    }

}
