package me.piebridge.brevent.protocol;

/**
 * Created by thom on 2018/2/25.
 */
public class BaseBreventOK extends BaseBreventProtocol {

    public static final BaseBreventOK INSTANCE = new BaseBreventOK();

    private BaseBreventOK() {
        super(BASE_STATUS_OK);
    }

}
