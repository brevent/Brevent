package me.piebridge.brevent.ui;

import android.app.Application;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import me.piebridge.brevent.R;

/**
 * Created by thom on 2017/2/7.
 */
public class BreventApplication extends Application {

    private UUID mToken;

    private boolean allowRoot;

    private boolean mSupportStopped = true;

    private boolean copied;

    @Override
    public void onCreate() {
        super.onCreate();
        mToken = UUID.randomUUID();
    }

    public UUID getToken() {
        return mToken;
    }

    public void setAllowRoot() {
        allowRoot = true;
    }

    public boolean allowRoot() {
        return allowRoot;
    }

    public void setSupportStopped(boolean supportStopped) {
        if (mSupportStopped != supportStopped) {
            mSupportStopped = supportStopped;
        }
    }

    public boolean supportStopped() {
        return mSupportStopped;
    }

    public File copyBrevent() {
        File file = getApplicationContext().getExternalFilesDir(null);
        if (file == null) {
            return null;
        }
        File output = new File(file, "brevent.sh");
        if (!copied) {
            try {
                try (
                        InputStream is = getResources().openRawResource(R.raw.brevent);
                        OutputStream os = new FileOutputStream(output)
                ) {
                    byte[] bytes = new byte[0x400];
                    int length;
                    while ((length = is.read(bytes)) != -1) {
                        os.write(bytes, 0, length);
                        if (length < 0x400) {
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                UILog.d("Can't copy brevent", e);
                return null;
            }
        }
        return output;
    }

}
