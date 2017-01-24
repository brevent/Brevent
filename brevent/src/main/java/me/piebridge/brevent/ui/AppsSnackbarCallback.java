package me.piebridge.brevent.ui;

import android.os.Handler;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.Snackbar;
import android.view.View;

import me.piebridge.brevent.protocol.BreventPackages;

/**
 * Created by thom on 2017/2/7.
 */
public class AppsSnackbarCallback extends BaseTransientBottomBar.BaseCallback<Snackbar> implements View.OnClickListener  {

    private final Handler handler;

    private final Handler uiHandler;

    private final BreventPackages response;

    public AppsSnackbarCallback(Handler handler, Handler uiHandler, BreventPackages response) {
        this.handler = handler;
        this.uiHandler = uiHandler;
        this.response = response;
    }

    @Override
    public void onShown(Snackbar snackbar) {
        uiHandler.obtainMessage(BreventActivity.UI_MESSAGE_UPDATE_BREVENT, response).sendToTarget();
    }

    @Override
    public void onClick(View v) {
        response.undo();
        handler.obtainMessage(BreventActivity.MESSAGE_BREVENT_REQUEST, response).sendToTarget();
    }

}
