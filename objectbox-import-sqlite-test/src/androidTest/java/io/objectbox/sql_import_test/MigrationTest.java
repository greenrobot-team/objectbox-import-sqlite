package io.objectbox.sql_import_test;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.sql.ColumnMapping;
import io.objectbox.sql.SqlMigration;
import io.objectbox.sql.TableMapping;
import io.objectbox.sql_import_test.model.MyObjectBox;
import io.objectbox.sql_import_test.model.SimpleEntity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class MigrationTest {

    @Test
    public void migrateWithAutoDetect() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();
        assertEquals("io.objectbox.sql_import_test", appContext.getPackageName());

        // database setup
        DatabaseHelper.delete(appContext);
        SQLiteDatabase database = new DatabaseHelper(appContext).getWritableDatabase();
        long[] simpleEntityIds = new long[]{
                SqliteInsertHelper.insertSimpleEntity(database)
        };
        long[] customerIds = new long[] {
                SqliteInsertHelper.insertCustomer(database, "Leia"),
                SqliteInsertHelper.insertCustomer(database, "Luke")
        };
        long[] orderIds = new long[] {
                SqliteInsertHelper.insertOrder(database, "Lightsaber", customerIds[0]),
                SqliteInsertHelper.insertOrder(database, "Droid", customerIds[0]),
                SqliteInsertHelper.insertOrder(database, "Speeder", customerIds[1]),
        };

        BoxStore.deleteAllFiles(appContext, null);
        BoxStore boxStore = MyObjectBox.builder().androidContext(appContext).build();

        // detect mapping
        SqlMigration migration = new SqlMigration(database, boxStore);
        migration.autoDetect();

        Map<String, TableMapping> map = migration.getTableMap();
        assertEquals(1, map.size());
        TableMapping tableMapping = map.get("SimpleEntity");
        assertEquals("SimpleEntity", tableMapping.getTableName());
        assertEquals(SimpleEntity.class, tableMapping.getEntityClass());

        Map<String, ColumnMapping> columnMap = tableMapping.getColumnMap();
        assertEquals(18, columnMap.size());

        // migrate
        migration.migrate(null);

        Box<SimpleEntity> box = boxStore.boxFor(SimpleEntity.class);
        List<SimpleEntity> simpleEntities = box.query().build().find();
        assertEquals(1, simpleEntities.size());
        for (int i = 0; i < simpleEntities.size(); i++) {
            SimpleEntity e = simpleEntities.get(i);
            assertEquals(simpleEntityIds[i], e.getId());

            assertTrue(e.isSimpleBoolean());
            assertEquals(true, e.nullableBoolean);

            assertEquals(21, e.simpleInteger);
            assertEquals(21, e.getNullableInteger().intValue());
            assertEquals((short) 21, e.getSimpleShort());
            assertEquals((short) 21, e.nullableShort.shortValue());
            assertEquals(21L, e.simpleLong);
            assertEquals(21L, e.getNullableLong().longValue());

            assertEquals(21.0f, e.getSimpleFloat(), 0);
            assertEquals(21.0f, e.nullableFloat, 0);
            assertEquals(21.0, e.simpleDouble, 0);
            assertEquals(21.0, e.getNullableDouble(), 0);

            assertEquals((byte) 21, e.getSimpleByte());
            assertEquals((byte) 21, e.nullableByte.byteValue());

            assertTrue(Arrays.equals(new byte[]{1, 2, 3}, e.getByteArray()));

            assertEquals("Farah", e.text);

            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.GERMANY);
            calendar.set(2018, 1, 2, 21, 42, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            assertEquals(calendar.getTimeInMillis(), e.getDate().getTime());
        }
    }


}
