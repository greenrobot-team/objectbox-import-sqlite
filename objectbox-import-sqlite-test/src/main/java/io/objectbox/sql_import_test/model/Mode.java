package io.objectbox.sql_import_test.model;

public enum Mode {
    DEFAULT(0),
    EXTRA(1);

    private final int id;

    Mode(int id) {
        this.id = id;
    }

    public static Mode fromId(int id) {
        for (Mode mode : Mode.values()) {
            if (mode.id == id) {
                return mode;
            }
        }
        return Mode.DEFAULT;
    }

    public int toId() {
        return id;
    }
}
