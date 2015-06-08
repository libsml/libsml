package com.github.libsml;

import com.google.common.base.Preconditions;

import java.util.HashMap;
import java.util.Map;


public enum Mode {


    LOCAL("local"),
    MR("mr"),
    SPARK("spark");

    private final String name;

    Mode(String name) {
        this.name = name;
    }

    private static Map<String, Mode> MODE_BY_NAME = new HashMap<String, Mode>();

    static {
        for (Mode mode : Mode.values()) {
            Mode old = MODE_BY_NAME.put(mode.getName(), mode);
            Preconditions.checkState(old == null, "Mode exception:duplicate model name=%s", mode.getName());
        }
    }

    public String getName() {
        return name;
    }

    public static Mode getByName(String name) {
        Mode mode = MODE_BY_NAME.get(name);
        Preconditions.checkState(mode != null, "Mode exception:found no model name=%s", mode.getName());
        return mode;
    }

}
