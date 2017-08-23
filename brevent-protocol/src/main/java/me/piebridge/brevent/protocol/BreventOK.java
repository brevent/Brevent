package me.piebridge.brevent.protocol;

/**
 * Created by thom on 2017/8/24.
 */
public class BreventOK extends BreventProtocol {

    public static final BreventOK INSTANCE = new BreventOK();

    private BreventOK() {
        super(STATUS_OK);
    }

}
