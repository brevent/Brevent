package me.piebridge.brevent.protocol;

import android.os.Parcel;

/**
 * Created by thom on 2018/01/24.
 */
public class BreventCmdRequest extends BreventProtocol {

    public final String command;

    public final boolean force;

    public BreventCmdRequest(String command, boolean force) {
        super(CMD_REQUEST);
        this.command = command;
        this.force = force;
    }

    BreventCmdRequest(Parcel in) {
        super(in);
        this.command = in.readString();
        this.force = in.readInt() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(command);
        dest.writeInt(force ? 1 : 0);
    }

    @Override
    public String toString() {
        return super.toString() + ", force: " + force;
    }

}
