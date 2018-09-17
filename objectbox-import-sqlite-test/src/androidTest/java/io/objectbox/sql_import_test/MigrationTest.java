package io.objectbox.sql_import_test;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.sql.ColumnMapping;
import io.objectbox.sql.SqlMigration;
import io.objectbox.sql.TableMapping;
import io.objectbox.sql_import_test.model.Customer;
import io.objectbox.sql_import_test.model.Customer_;
import io.objectbox.sql_import_test.model.Mode;
import io.objectbox.sql_import_test.model.MyObjectBox;
import io.objectbox.sql_import_test.model.Order;
import io.objectbox.sql_import_test.model.Order_;
import io.objectbox.sql_import_test.model.SimpleEntity;
import io.objectbox.sql_import_test.model.SimpleEntity_;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
                SqliteInsertHelper.insertSimpleEntity(database),
                SqliteInsertHelper.insertSimpleEntityAllNull(database)
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

        assertSimpleEntityMapping(map, 19);
        assertCustomerMapping(map);
        assertOrderMapping(map);

        // migrate
        migration.migrate(null);

        assertSimpleEntityBox(boxStore, simpleEntityIds, Mode.NULL);
        assertCustomerBox(boxStore, customerIds);
        assertOrderBox(boxStore, orderIds, customerIds);

        boxStore.close();
    }

    @Test
    public void migrateWithAutoDetect_customized() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();
        assertEquals("io.objectbox.sql_import_test", appContext.getPackageName());

        // database setup
        DatabaseHelper.delete(appContext);
        SQLiteDatabase database = new DatabaseHelper(appContext).getWritableDatabase();
        long[] simpleEntityIds = new long[]{
                SqliteInsertHelper.insertSimpleEntity(database),
                SqliteInsertHelper.insertSimpleEntityAllNull(database)
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
        // customize detected mapping
        migration.removeTableMapping(DatabaseContract.SimpleEntity.TABLE_NAME);
        migration.modifyTableMapping(DatabaseContract.Customer.TABLE_NAME)
                .mapColumnToProperty(DatabaseContract.Customer.COLUMN_NAME_NAME, Customer_.name,
                        new ColumnMapping.Mapper() {
                            @Override
                            public void mapValue(ColumnMapping mapping, Cursor row, Object entity) {
                                mapping.setValue(entity, "REDACTED");
                            }
                        })
                .build();
        migration.modifyTableMapping(DatabaseContract.Order.TABLE_NAME)
                .removeColumnMapping(DatabaseContract.Order.COLUMN_NAME_TEXT)
                .build();

        // assert mapping
        Map<String, TableMapping> map = migration.getTableMap();
        assertEquals(2, map.size());

        assertCustomerMapping(map);

        TableMapping orderMapping = map.get("Order");
        assertEquals("Order", orderMapping.getTableName());
        assertEquals(Order.class, orderMapping.getEntityClass());
        assertEquals(2, orderMapping.getColumnMap().size());

        // migrate
        migration.migrate(null);

        // assert box store
        Box<SimpleEntity> simpleEntityBox = boxStore.boxFor(SimpleEntity.class);
        assertEquals(0, simpleEntityBox.count());

        Box<Customer> customerBox = boxStore.boxFor(Customer.class);
        assertEquals(2, customerBox.count());
        for (Customer customer : customerBox.getAll()) {
            assertEquals("REDACTED", customer.name);
        }

        Box<Order> box = boxStore.boxFor(Order.class);
        assertEquals(3, box.count());
        assertOrder(box, orderIds[0], null, customerIds[0]);
        assertOrder(box, orderIds[1], null, customerIds[0]);
        assertOrder(box, orderIds[2], null, customerIds[1]);

        boxStore.close();
    }

    @Test
    public void migrateWithCustomMapping() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();
        assertEquals("io.objectbox.sql_import_test", appContext.getPackageName());

        // database setup
        DatabaseHelper.delete(appContext);
        SQLiteDatabase database = new DatabaseHelper(appContext).getWritableDatabase();
        long[] simpleEntityIds = new long[]{
                SqliteInsertHelper.insertSimpleEntity(database),
                SqliteInsertHelper.insertSimpleEntityAllNull(database)
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

        // manual mapping
        SqlMigration migration = new SqlMigration(database, boxStore);
        migration.mapTableToEntity(DatabaseContract.SimpleEntity.TABLE_NAME, SimpleEntity.class)
                .mapColumnToProperty(DatabaseContract.SimpleEntity._ID, SimpleEntity_.id)
                .mapColumnToProperty(DatabaseContract.SimpleEntity.COLUMN_NAME_BOOLEAN, SimpleEntity_.simpleBoolean)
                .mapColumnToProperty(DatabaseContract.SimpleEntity.COLUMN_NAME_BOOLEAN_NULL, SimpleEntity_.nullableBoolean)
                .mapColumnToProperty(DatabaseContract.SimpleEntity.COLUMN_NAME_INTEGER, SimpleEntity_.simpleInteger)
                .mapColumnToProperty(DatabaseContract.SimpleEntity.COLUMN_NAME_INTEGER_NULL, SimpleEntity_.nullableInteger)
                .mapColumnToProperty(DatabaseContract.SimpleEntity.COLUMN_NAME_SHORT, SimpleEntity_.simpleShort)
                .mapColumnToProperty(DatabaseContract.SimpleEntity.COLUMN_NAME_SHORT_NULL, SimpleEntity_.nullableShort)
                .mapColumnToProperty(DatabaseContract.SimpleEntity.COLUMN_NAME_LONG, SimpleEntity_.simpleLong)
                .mapColumnToProperty(DatabaseContract.SimpleEntity.COLUMN_NAME_LONG_NULL, SimpleEntity_.nullableLong)
                .mapColumnToProperty(DatabaseContract.SimpleEntity.COLUMN_NAME_FLOAT, SimpleEntity_.simpleFloat)
                .mapColumnToProperty(DatabaseContract.SimpleEntity.COLUMN_NAME_FLOAT_NULL, SimpleEntity_.nullableFloat)
                .mapColumnToProperty(DatabaseContract.SimpleEntity.COLUMN_NAME_DOUBLE, SimpleEntity_.simpleDouble)
                .mapColumnToProperty(DatabaseContract.SimpleEntity.COLUMN_NAME_DOUBLE_NULL, SimpleEntity_.nullableDouble)
                .mapColumnToProperty(DatabaseContract.SimpleEntity.COLUMN_NAME_BYTE, SimpleEntity_.simpleByte)
                .mapColumnToProperty(DatabaseContract.SimpleEntity.COLUMN_NAME_BYTE_NULL, SimpleEntity_.nullableByte)
                .mapColumnToProperty(DatabaseContract.SimpleEntity.COLUMN_NAME_BYTE_ARRAY, SimpleEntity_.byteArray)
                .mapColumnToProperty(DatabaseContract.SimpleEntity.COLUMN_NAME_STRING, SimpleEntity_.text)
                .mapColumnToProperty(DatabaseContract.SimpleEntity.COLUMN_NAME_DATE, SimpleEntity_.date)
                .mapColumnToProperty(DatabaseContract.SimpleEntity.COLUMN_NAME_MODE,
                        SimpleEntity_.mode, new ColumnMapping.Mapper() {
                            @Override
                            public void mapValue(ColumnMapping mapping, Cursor row, Object entity) {
                                int mode;
                                if (row.isNull(mapping.getColumnIndex())) {
                                    mode = Mode.DEFAULT.toId();
                                } else {
                                    mode = row.getInt(mapping.getColumnIndex());
                                }
                                mapping.setValue(entity, mode);
                            }
                        })
                .build();
        migration.mapTableToEntity(DatabaseContract.Customer.TABLE_NAME, Customer.class)
                .mapColumnToProperty(DatabaseContract.Customer._ID, Customer_.id)
                .mapColumnToProperty(DatabaseContract.Customer.COLUMN_NAME_NAME, Customer_.name)
                .build();
        migration.mapTableToEntity(DatabaseContract.Order.TABLE_NAME, Order.class)
                .mapColumnToProperty(DatabaseContract.Order._ID, Order_.id)
                .mapColumnToProperty(DatabaseContract.Order.COLUMN_NAME_TEXT, Order_.text)
                .mapForeignKeyColumnToToOne(DatabaseContract.Order.COLUMN_NAME_CUSTOMER, "customer")
                .build();

        Map<String, TableMapping> map = migration.getTableMap();
        assertEquals(3, map.size());

        assertSimpleEntityMapping(map, 19);
        assertCustomerMapping(map);
        assertOrderMapping(map);

        // migrate
        migration.migrate(null);

        assertSimpleEntityBox(boxStore, simpleEntityIds, Mode.DEFAULT);
        assertCustomerBox(boxStore, customerIds);
        assertOrderBox(boxStore, orderIds, customerIds);

        boxStore.close();
    }

    private void assertSimpleEntityMapping(Map<String, TableMapping> map, int expectedColumnCount) {
        TableMapping tableMapping = map.get("SimpleEntity");
        assertEquals("SimpleEntity", tableMapping.getTableName());
        assertEquals(SimpleEntity.class, tableMapping.getEntityClass());

        Map<String, ColumnMapping> columnMap = tableMapping.getColumnMap();
        assertEquals(expectedColumnCount, columnMap.size());
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

    private void assertSimpleEntityBox(BoxStore boxStore, long[] simpleEntityIds, Mode modeIfNull) {
        Box<SimpleEntity> box = boxStore.boxFor(SimpleEntity.class);
        assertEquals(2, box.count());

        assertSimpleEntity(box, simpleEntityIds[0]);
        assertSimpleEntityNullOrDefault(box, simpleEntityIds[1], modeIfNull);
    }

    private void assertSimpleEntity(Box<SimpleEntity> box, long simpleEntityId) {
        SimpleEntity notNullEntity = box.get(simpleEntityId);
        assertEquals(simpleEntityId, notNullEntity.getId());

        assertTrue(notNullEntity.isSimpleBoolean());
        assertEquals(true, notNullEntity.nullableBoolean);

        assertEquals(21, notNullEntity.simpleInteger);
        assertEquals(21, notNullEntity.getNullableInteger().intValue());
        assertEquals((short) 21, notNullEntity.getSimpleShort());
        assertEquals((short) 21, notNullEntity.nullableShort.shortValue());
        assertEquals(21L, notNullEntity.simpleLong);
        assertEquals(21L, notNullEntity.getNullableLong().longValue());

        assertEquals(21.0f, notNullEntity.getSimpleFloat(), 0);
        assertEquals(21.0f, notNullEntity.nullableFloat, 0);
        assertEquals(21.0, notNullEntity.simpleDouble, 0);
        assertEquals(21.0, notNullEntity.getNullableDouble(), 0);

        assertEquals((byte) 21, notNullEntity.getSimpleByte());
        assertEquals((byte) 21, notNullEntity.nullableByte.byteValue());

        assertTrue(Arrays.equals(new byte[]{1, 2, 3}, notNullEntity.getByteArray()));

        assertEquals("Farah", notNullEntity.text);

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.GERMANY);
        calendar.set(2018, 1, 2, 21, 42, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        assertEquals(calendar.getTimeInMillis(), notNullEntity.getDate().getTime());

        assertEquals(Mode.EXTRA, notNullEntity.mode);
    }

    private void assertSimpleEntityNullOrDefault(Box<SimpleEntity> box, long simpleEntityId,
                                                 Mode modeIfNull) {
        SimpleEntity nullEntity = box.get(simpleEntityId);
        assertEquals(simpleEntityId, nullEntity.getId());

        assertFalse(nullEntity.isSimpleBoolean());
        assertNull(nullEntity.nullableBoolean);

        assertEquals(0, nullEntity.simpleInteger);
        assertNull(nullEntity.getNullableInteger());
        assertEquals(0, nullEntity.getSimpleShort());
        assertNull(nullEntity.nullableShort);
        assertEquals(0, nullEntity.simpleLong);
        assertNull(nullEntity.getNullableLong());

        assertEquals(0, nullEntity.getSimpleFloat(), 0);
        assertNull(nullEntity.nullableFloat);
        assertEquals(0, nullEntity.simpleDouble, 0);
        assertNull(nullEntity.getNullableDouble());

        assertEquals((byte) 0, nullEntity.getSimpleByte());
        assertNull(nullEntity.nullableByte);

        assertNull(nullEntity.getByteArray());

        assertNull(nullEntity.text);

        assertNull(nullEntity.getDate());

        assertEquals(modeIfNull, nullEntity.mode);
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
        assertOrder(box, orderIds[1], "Droid", customerIds[0]);
        assertOrder(box, orderIds[2], "Speeder", customerIds[1]);
    }

    private void assertOrder(Box<Order> box, long orderId, String text, long customerId) {
        Order order = box.get(orderId);
        assertNotNull(order);
        assertEquals(text, order.text);
        assertEquals(customerId, order.customer.getTargetId());
        assertNotNull(order.customer.getTarget());
    }


}
