package io.objectbox.sql_import_test;

import android.provider.BaseColumns;

public final class DatabaseContract {

    private DatabaseContract() {
    }

    public static class SimpleEntity implements BaseColumns {
        // Looks like through Android API SQLite table and column names are case sensitive
        public static final String TABLE_NAME = "SimpleEntity";
        public static final String COLUMN_NAME_BOOLEAN = "simpleBoolean";
        public static final String COLUMN_NAME_BOOLEAN_NULL = "nullableBoolean";
        public static final String COLUMN_NAME_INTEGER = "simpleInteger";
        public static final String COLUMN_NAME_INTEGER_NULL = "nullableInteger";
        public static final String COLUMN_NAME_SHORT = "simpleShort";
        public static final String COLUMN_NAME_SHORT_NULL = "nullableShort";
        public static final String COLUMN_NAME_LONG = "simpleLong";
        public static final String COLUMN_NAME_LONG_NULL = "nullableLong";
        public static final String COLUMN_NAME_FLOAT = "simpleFloat";
        public static final String COLUMN_NAME_FLOAT_NULL = "nullableFloat";
        public static final String COLUMN_NAME_DOUBLE = "simpleDouble";
        public static final String COLUMN_NAME_DOUBLE_NULL = "nullableDouble";
        public static final String COLUMN_NAME_BYTE = "simpleByte";
        public static final String COLUMN_NAME_BYTE_NULL = "nullableByte";
        public static final String COLUMN_NAME_BYTE_ARRAY = "byteArray";
        public static final String COLUMN_NAME_STRING = "text";
        public static final String COLUMN_NAME_DATE = "date";
    }

    public static class Customer implements BaseColumns {
        public static final String TABLE_NAME = "Customer";
        public static final String COLUMN_NAME_NAME = "name";
    }

    public static class Order implements BaseColumns {
        public static final String TABLE_NAME = "Order";
        public static final String COLUMN_NAME_TEXT = "text";
        public static final String COLUMN_NAME_CUSTOMER = "customer";
    }

}
