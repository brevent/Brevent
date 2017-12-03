package me.piebridge.brevent.protocol;

/**
 * Created by thom on 2017/12/3.
 */
public class BreventOpsKO extends BreventProtocol {

    public static final BreventOpsKO INSTANCE = new BreventOpsKO();

    private BreventOpsKO() {
        super(OPS_KO);
    }

}
