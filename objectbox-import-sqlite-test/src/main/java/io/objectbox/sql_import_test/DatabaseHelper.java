package io.objectbox.sql_import_test;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

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

    private static final String SQL_CREATE_ENTRIES =
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

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + SimpleEntity.TABLE_NAME;

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}
