package me.piebridge.brevent.ui;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.Window;

import me.piebridge.brevent.R;

/**
 * Created by thom on 2017/3/2.
 */
public class BreventLicenses extends PreferenceActivity {

    @Override
    @SuppressWarnings("deprecation")
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.licenses);
    }

}
