package me.piebridge.brevent.ui;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.support.annotation.WorkerThread;

import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.ConnectException;
import java.net.Socket;

import me.piebridge.brevent.protocol.BreventProtocol;

/**
 * Created by thom on 2017/2/3.
 */
public class AppsActivityHandler extends Handler {

    public static final long DELAY = 15000;

    private final Handler uiHandler;

    private final WeakReference<BreventActivity> mReference;

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
                uiHandler.sendEmptyMessage(BreventActivity.UI_MESSAGE_SHOW_PROGRESS);
            case BreventActivity.MESSAGE_RETRIEVE2:
                removeMessages(BreventActivity.MESSAGE_BREVENT_NO_RESPONSE);
                send(new BreventProtocol(BreventProtocol.STATUS_REQUEST));
                break;
            case BreventActivity.MESSAGE_BREVENT_RESPONSE:
                removeMessages(BreventActivity.MESSAGE_BREVENT_NO_RESPONSE);
                BreventProtocol breventResponse = (BreventProtocol) message.obj;
                if (breventResponse.versionUnmatched()) {
                    uiHandler.sendEmptyMessage(BreventActivity.UI_MESSAGE_VERSION_UNMATCHED);
                } else {
                    BreventActivity activity = mReference.get();
                    if (activity != null) {
                        activity.onBreventResponse(breventResponse);
                    }
                    uiHandler.sendEmptyMessage(BreventActivity.UI_MESSAGE_HIDE_DISABLED);
                }
                break;
            case BreventActivity.MESSAGE_BREVENT_NO_RESPONSE:
                uiHandler.sendEmptyMessage(BreventActivity.UI_MESSAGE_NO_BREVENT_DATA);
                break;
            case BreventActivity.MESSAGE_BREVENT_REQUEST:
                send((BreventProtocol) message.obj);
                break;
        }
    }

    @WorkerThread
    private void send(BreventProtocol message) {
        try {
            Socket socket = new Socket(BreventProtocol.HOST, BreventProtocol.PORT);
            DataOutputStream os = new DataOutputStream(socket.getOutputStream());
            message.writeTo(os);
            os.flush();
            socket.close();
            if (message.getAction() != BreventProtocol.STATUS_REQUEST) {
                sendEmptyMessageDelayed(BreventActivity.MESSAGE_BREVENT_NO_RESPONSE, DELAY);
            } else {
                sendEmptyMessageDelayed(BreventActivity.MESSAGE_BREVENT_NO_RESPONSE, DELAY * 2);
            }
        } catch (ConnectException e) {
            UILog.v("cannot connect to " + BreventProtocol.HOST + ":" + BreventProtocol.PORT, e);
            uiHandler.sendEmptyMessage(BreventActivity.UI_MESSAGE_NO_BREVENT);
        } catch (IOException | RemoteException e) {
            uiHandler.obtainMessage(BreventActivity.UI_MESSAGE_IO_BREVENT, e).sendToTarget();
        }
    }

}
