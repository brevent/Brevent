package me.piebridge.brevent.protocol;

import android.os.Parcel;
import android.support.annotation.NonNull;

/**
 * Created by thom on 2018/01/24.
 */
public class BreventCmdResponse extends BreventProtocol {

    public final String request;

    public final int remain;

    public final String output;

    public final boolean done;

    public BreventCmdResponse(@NonNull String request, @NonNull String output,
                              boolean done, int remain) {
        super(CMD_RESPONSE);
        this.request = request;
        this.output = output;
        this.done = done;
        this.remain = remain;
    }

    BreventCmdResponse(Parcel in) {
        super(in);
        this.request = in.readString();
        this.output = in.readString();
        this.done = in.readInt() != 0;
        this.remain = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(request);
        dest.writeString(output);
        dest.writeInt(done ? 1 : 0);
        dest.writeInt(remain);
    }

    @Override
    public String toString() {
        return super.toString() + ", remain: " + remain + ", done: " + done;
    }

}
