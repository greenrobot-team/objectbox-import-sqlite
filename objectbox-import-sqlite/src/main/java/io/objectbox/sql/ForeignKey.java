package io.objectbox.sql;

public class ForeignKey {

    public final String columnFrom;
    public final String tableTo;
    public final String columnTo;

    public ForeignKey(String columnFrom, String tableTo, String columnTo) {
        this.columnFrom = columnFrom;
        this.tableTo = tableTo;
        this.columnTo = columnTo;
    }
}
