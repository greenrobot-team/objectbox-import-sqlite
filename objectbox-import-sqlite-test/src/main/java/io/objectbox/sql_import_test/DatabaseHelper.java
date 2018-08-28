package io.objectbox.sql_import_test;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import io.objectbox.sql_import_test.DatabaseContract.Customer;
import io.objectbox.sql_import_test.DatabaseContract.Order;
import io.objectbox.sql_import_test.DatabaseContract.SimpleEntity;


public class DatabaseHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "SqlImport.db";

    public static void delete(Context context) {
        context.deleteDatabase(DATABASE_NAME);
    }

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    private static final String SQL_CREATE_SIMPLE_ENTITY =
            "CREATE TABLE " + SimpleEntity.TABLE_NAME + " (" +
                    SimpleEntity._ID + " INTEGER PRIMARY KEY," +
                    SimpleEntity.COLUMN_NAME_BOOLEAN + " INTEGER," +
                    SimpleEntity.COLUMN_NAME_BOOLEAN_NULL + " INTEGER," +
                    SimpleEntity.COLUMN_NAME_INTEGER + " INTEGER," +
                    SimpleEntity.COLUMN_NAME_INTEGER_NULL + " INTEGER," +
                    SimpleEntity.COLUMN_NAME_SHORT + " INTEGER," +
                    SimpleEntity.COLUMN_NAME_SHORT_NULL + " INTEGER," +
                    SimpleEntity.COLUMN_NAME_LONG + " INTEGER," +
                    SimpleEntity.COLUMN_NAME_LONG_NULL + " INTEGER," +
                    SimpleEntity.COLUMN_NAME_FLOAT + " REAL," +
                    SimpleEntity.COLUMN_NAME_FLOAT_NULL + " REAL," +
                    SimpleEntity.COLUMN_NAME_DOUBLE + " REAL," +
                    SimpleEntity.COLUMN_NAME_DOUBLE_NULL + " REAL," +
                    SimpleEntity.COLUMN_NAME_BYTE + " BLOB," +
                    SimpleEntity.COLUMN_NAME_BYTE_NULL + " BLOB," +
                    SimpleEntity.COLUMN_NAME_BYTE_ARRAY + " BLOB," +
                    SimpleEntity.COLUMN_NAME_STRING + " TEXT," +
                    SimpleEntity.COLUMN_NAME_DATE + " INTEGER)";

    private static final String SQL_CREATE_CUSTOMER =
            "CREATE TABLE " + Customer.TABLE_NAME + " (" +
                    Customer._ID + " INTEGER PRIMARY KEY," +
                    Customer.COLUMN_NAME_NAME + " TEXT)";

    private static final String SQL_CREATE_ORDER =
            "CREATE TABLE " + Order.TABLE_NAME + " (" +
                    Order._ID + " INTEGER PRIMARY KEY," +
                    Order.COLUMN_NAME_TEXT + " TEXT," +
                    Order.COLUMN_NAME_CUSTOMER + " INTEGER,"
                    + "FOREIGN KEY(" + Order.COLUMN_NAME_CUSTOMER + ") REFERENCES " + Customer.TABLE_NAME + "(_id))";

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_SIMPLE_ENTITY);
        db.execSQL(SQL_CREATE_CUSTOMER);
        db.execSQL(SQL_CREATE_ORDER);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + SimpleEntity.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + Customer.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + Order.TABLE_NAME);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}
