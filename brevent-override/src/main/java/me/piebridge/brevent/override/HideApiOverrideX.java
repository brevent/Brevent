package me.piebridge.brevent.override;

import java.lang.reflect.Field;

public class HideApiOverrideX {

    private HideApiOverrideX() {

    }

    public static int getOffset(Field field) {
        return field.getOffset();
    }

}
