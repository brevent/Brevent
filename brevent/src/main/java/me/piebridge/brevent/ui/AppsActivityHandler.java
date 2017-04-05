package me.piebridge.brevent.ui;

import android.app.NotificationManager;
import android.content.Context;
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

    public static final int LATER = 3000;

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
                send(new BreventProtocol(BreventProtocol.STATUS_REQUEST));
                break;
            case BreventActivity.MESSAGE_RETRIEVE3:
                UILog.d("retry request status");
                BreventProtocol breventProtocol = new BreventProtocol(BreventProtocol.STATUS_REQUEST);
                breventProtocol.retry = true;
                send(breventProtocol);
                break;
            case BreventActivity.MESSAGE_BREVENT_RESPONSE:
                if (!hasResponse) {
                    UILog.d("received response");
                    hasResponse = true;
                    hideNotification();
                }
                removeMessages(BreventActivity.MESSAGE_RETRIEVE3);
                removeMessages(BreventActivity.MESSAGE_BREVENT_NO_RESPONSE);
                BreventProtocol breventResponse = (BreventProtocol) message.obj;
                if (breventResponse.versionMismatched()) {
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
            Socket socket = new Socket(BreventProtocol.HOST, BreventProtocol.PORT);
            if (action == BreventProtocol.STATUS_REQUEST) {
                socket.setSoTimeout(LATER);
            }
            DataOutputStream os = new DataOutputStream(socket.getOutputStream());
            message.writeTo(os);
            os.flush();
            socket.close();
            if (action != BreventProtocol.STATUS_REQUEST) {
                sendEmptyMessageDelayed(BreventActivity.MESSAGE_BREVENT_NO_RESPONSE, DELAY);
            } else {
                sendEmptyMessageDelayed(BreventActivity.MESSAGE_BREVENT_NO_RESPONSE, DELAY * 2);
            }
        } catch (ConnectException e) {
            UILog.v("cannot connect to " + BreventProtocol.HOST + ":" + BreventProtocol.PORT, e);
            if (!message.retry) {
                uiHandler.sendEmptyMessage(BreventActivity.UI_MESSAGE_NO_BREVENT);
            }
        } catch (IOException | RemoteException e) {
            UILog.v("cannot connect to " + BreventProtocol.HOST + ":" + BreventProtocol.PORT, e);
            uiHandler.obtainMessage(BreventActivity.UI_MESSAGE_IO_BREVENT, e).sendToTarget();
        }
        if (!hasResponse && action == BreventProtocol.STATUS_REQUEST) {
            sendEmptyMessageDelayed(BreventActivity.MESSAGE_RETRIEVE3, LATER);
        }
    }

}
