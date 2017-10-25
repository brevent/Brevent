package me.piebridge.brevent.protocol;

/**
 * Created by thom on 2017/8/24.
 */
public class BreventKO extends BreventProtocol {

    public static final BreventKO INSTANCE = new BreventKO();

    private BreventKO() {
        super(STATUS_KO);
    }

}
