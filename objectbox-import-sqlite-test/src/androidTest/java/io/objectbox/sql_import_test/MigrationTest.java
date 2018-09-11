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
import io.objectbox.sql_import_test.model.Customer;
import io.objectbox.sql_import_test.model.Mode;
import io.objectbox.sql_import_test.model.MyObjectBox;
import io.objectbox.sql_import_test.model.Order;
import io.objectbox.sql_import_test.model.SimpleEntity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
        long[] customerIds = new long[]{
                SqliteInsertHelper.insertCustomer(database, "Leia"),
                SqliteInsertHelper.insertCustomer(database, "Luke")
        };
        long[] orderIds = new long[]{
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
        assertEquals(3, map.size());

        assertSimpleEntityMapping(map);
        assertCustomerMapping(map);
        assertOrderMapping(map);

        // migrate
        migration.migrate(null);

        assertSimpleEntityBox(boxStore, simpleEntityIds);
        assertCustomerBox(boxStore, customerIds);
        assertOrderBox(boxStore, orderIds, customerIds);
    }

    private void assertSimpleEntityMapping(Map<String, TableMapping> map) {
        TableMapping tableMapping = map.get("SimpleEntity");
        assertEquals("SimpleEntity", tableMapping.getTableName());
        assertEquals(SimpleEntity.class, tableMapping.getEntityClass());

        Map<String, ColumnMapping> columnMap = tableMapping.getColumnMap();
        assertEquals(19, columnMap.size());
    }

    private void assertCustomerMapping(Map<String, TableMapping> map) {
        TableMapping tableMapping = map.get("Customer");
        assertEquals("Customer", tableMapping.getTableName());
        assertEquals(Customer.class, tableMapping.getEntityClass());

        Map<String, ColumnMapping> columnMap = tableMapping.getColumnMap();
        assertEquals(2, columnMap.size());
    }

    private void assertOrderMapping(Map<String, TableMapping> map) {
        TableMapping tableMapping = map.get("Order");
        assertEquals("Order", tableMapping.getTableName());
        assertEquals(Order.class, tableMapping.getEntityClass());

        Map<String, ColumnMapping> columnMap = tableMapping.getColumnMap();
        assertEquals(3, columnMap.size());
    }

    private void assertSimpleEntityBox(BoxStore boxStore, long[] simpleEntityIds) {
        Box<SimpleEntity> box = boxStore.boxFor(SimpleEntity.class);
        List<SimpleEntity> simpleEntities = box.getAll();
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

            assertEquals(Mode.EXTRA, e.mode);
        }
    }

    private void assertCustomerBox(BoxStore boxStore, long[] customerIds) {
        Box<Customer> box = boxStore.boxFor(Customer.class);
        assertEquals(2, box.count());

        Customer leia = box.get(customerIds[0]);
        assertNotNull(leia);
        assertEquals("Leia", leia.name);

        Customer luke = box.get(customerIds[1]);
        assertNotNull(luke);
        assertEquals("Luke", luke.name);
    }

    private void assertOrderBox(BoxStore boxStore, long[] orderIds, long[] customerIds) {
        Box<Order> box = boxStore.boxFor(Order.class);
        assertEquals(3, box.count());

        assertOrder(box, orderIds[0], "Lightsaber", customerIds[0]);
        assertOrder(box,orderIds[1], "Droid", customerIds[0]);
        assertOrder(box, orderIds[2],"Speeder", customerIds[1]);
    }

    private void assertOrder(Box<Order> box, long orderId, String text, long customerId) {
        Order order = box.get(orderId);
        assertNotNull(order);
        assertEquals(text, order.text);
        assertEquals(customerId, order.customer.getTargetId());
        assertNotNull(order.customer.getTarget());
    }


}
