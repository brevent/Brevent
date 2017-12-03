package me.piebridge.brevent.protocol;

/**
 * Created by thom on 2017/12/3.
 */
public class BreventOpsOK extends BreventProtocol {

    public static final BreventOpsOK INSTANCE = new BreventOpsOK();

    private BreventOpsOK() {
        super(OPS_OK);
    }

}
