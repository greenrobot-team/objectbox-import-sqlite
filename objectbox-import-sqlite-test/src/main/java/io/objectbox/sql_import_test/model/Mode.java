package io.objectbox.sql_import_test.model;

public enum Mode {
    NULL(0),
    DEFAULT(1),
    EXTRA(2);

    private final int id;

    Mode(int id) {
        this.id = id;
    }

    public static Mode fromId(Integer id) {
        if (id == null) {
            return Mode.NULL;
        }
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
