package io.objectbox.sql_import_test;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import io.objectbox.sql_import_test.DatabaseContract.Customer;
import io.objectbox.sql_import_test.DatabaseContract.Order;
import io.objectbox.sql_import_test.DatabaseContract.SimpleEntity;
import io.objectbox.sql_import_test.model.Mode;

public class SqliteInsertHelper {

    public static long insertSimpleEntity(SQLiteDatabase database) {
        // TODO add null values
        // TODO add multiple rows with different values
        ContentValues values = new ContentValues();

        values.put(SimpleEntity.COLUMN_NAME_BOOLEAN, true);
        values.put(SimpleEntity.COLUMN_NAME_BOOLEAN_NULL, true);

        values.put(SimpleEntity.COLUMN_NAME_INTEGER, 21);
        values.put(SimpleEntity.COLUMN_NAME_INTEGER_NULL, 21);
        values.put(SimpleEntity.COLUMN_NAME_SHORT, (short) 21);
        values.put(SimpleEntity.COLUMN_NAME_SHORT_NULL, (short) 21);
        values.put(SimpleEntity.COLUMN_NAME_LONG, 21L);
        values.put(SimpleEntity.COLUMN_NAME_LONG_NULL, 21L);

        values.put(SimpleEntity.COLUMN_NAME_FLOAT, 21.0f);
        values.put(SimpleEntity.COLUMN_NAME_FLOAT_NULL, 21.0f);
        values.put(SimpleEntity.COLUMN_NAME_DOUBLE, 21.0);
        values.put(SimpleEntity.COLUMN_NAME_DOUBLE_NULL, 21.0);

        values.put(SimpleEntity.COLUMN_NAME_BYTE, (byte) 21);
        values.put(SimpleEntity.COLUMN_NAME_BYTE_NULL, (byte) 21);

        values.put(SimpleEntity.COLUMN_NAME_BYTE_ARRAY, new byte[]{1, 2, 3});

        values.put(SimpleEntity.COLUMN_NAME_STRING, "Farah");

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.GERMANY);
        calendar.set(2018, 1, 2, 21, 42, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        values.put(SimpleEntity.COLUMN_NAME_DATE, calendar.getTimeInMillis());

        values.put(SimpleEntity.COLUMN_NAME_MODE, Mode.EXTRA.toId());

        return database.insertOrThrow("\"" + SimpleEntity.TABLE_NAME + "\"", null, values);
    }

    public static long insertCustomer(SQLiteDatabase database, String name) {
        ContentValues values = new ContentValues();
        values.put(Customer.COLUMN_NAME_NAME, name);
        return database.insertOrThrow("\"" + Customer.TABLE_NAME + "\"", null, values);
    }

    public static long insertOrder(SQLiteDatabase database, String text, long customerId) {
        ContentValues values = new ContentValues();
        values.put(Order.COLUMN_NAME_TEXT, text);
        values.put(Order.COLUMN_NAME_CUSTOMER, customerId);
        return database.insertOrThrow("\"" + Order.TABLE_NAME + "\"", null, values);
    }

}
