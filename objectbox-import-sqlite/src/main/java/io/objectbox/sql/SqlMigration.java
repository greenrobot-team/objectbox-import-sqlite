/*
 * Copyright 2018 ObjectBox Ltd. All rights reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.objectbox.sql;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.BoxStoreHelper;
import io.objectbox.EntityInfo;
import io.objectbox.Property;
import io.objectbox.relation.ToOne;

/**
 * Migrates data from a SQLite database to ObjectBox by mapping tables and columns to entities
 * and properties.
 */
@SuppressWarnings("WeakerAccess")
public class SqlMigration {

    private final SQLiteDatabase database;
    private final BoxStore boxStore;
    private final BoxStoreHelper boxStoreHelper;

    private final Map<String, TableMapping> tableMap = new HashMap<>();

    public SqlMigration(SQLiteDatabase database, BoxStore boxStore) {
        this.database = database;
        this.boxStore = boxStore;
        this.boxStoreHelper = new BoxStoreHelper(boxStore);
    }

    /**
     * Maps a table to an entity. Use the returned builder to map columns of the table to properties
     * of the entity:
     *
     * <pre>migration.mapTableToEntity("customers", Customer.class)
     * .mapColumnToProperty("_id", Customer_.id)
     * .mapColumnToProperty("customer_name", Customer_.name)
     * .build();
     * </pre>
     *
     * @see #autoDetect(boolean, boolean)
     */
    public TableMapping.Builder mapTableToEntity(String tableName, Class entityClass) {
        return new TableMapping.Builder(database, boxStore, tableMap, tableName, entityClass);
    }

    /**
     * Gets a builder initialized with an existing table to entity mapping. The existing mapping
     * will be replaced when {@link TableMapping.Builder#build()} is called.
     */
    public TableMapping.Builder modifyTableMapping(String tableName) {
        TableMapping tableMapping = tableMap.get(tableName);
        if (tableMapping == null) {
            throw new IllegalStateException("No mapping for " + tableName);
        }
        return new TableMapping.Builder(database, boxStore, tableMap, tableName,
                tableMapping.getEntityClass(), tableMapping.getColumnMap());
    }

    /**
     * Removes a table to entity mapping.
     *
     * @return {@code null} if there was no mapping for this table, otherwise the removed mapping.
     */
    public TableMapping removeTableMapping(String tableName) {
        return tableMap.remove(tableName);
    }

    @VisibleForTesting
    public Map<String, TableMapping> getTableMap() {
        return tableMap;
    }

    /**
     * Set a custom entity to table and property to column mapping.
     */
    public void setTableMap(HashMap<String, TableMapping> customTableMap) {
        tableMap.clear();
        tableMap.putAll(customTableMap);
    }

    /**
     * Calls {@link #autoDetect(boolean, boolean)}, defaults to throwing if entity or property can
     * not be mapped.
     */
    public void autoDetect() {
        autoDetect(true, true);
    }

    /**
     * Builds table to entity and column to property mapping. Matches class names with table names
     * and property names with column names. Note that on Android table and column names are case
     * sensitive.
     * <p/>
     * A foreign key column is mapped to a ToOne, if one exists with the same name as the column.
     * <p/>
     * Use {@link #mapTableToEntity(String, Class)} instead to add custom mappings.
     *
     * @param throwIfEntityUnmapped   Throws if an entity could not be
     *                                automatically mapped to a table. Set to false if you have
     *                                entities that should not be mapped.
     * @param throwIfPropertyUnmapped Throws if an entity property could not be automatically mapped
     *                                to a column in the table for an entity. Set to false if your
     *                                entities have properties that should not be mapped.
     */
    public void autoDetect(boolean throwIfEntityUnmapped, boolean throwIfPropertyUnmapped) {
        Collection<Class> entityClasses = boxStore.getAllEntityClasses();
        List<String> unmappedEntities = new ArrayList<>();
        Map<Property, String> unmappedProperties = new HashMap<>();
        for (Class entityClass : entityClasses) {
            String tableName = entityClass.getSimpleName();
            if (tableExistsWithName(database, tableName)) {
                TableMapping tableMapping = new TableMapping(tableName, entityClass);

                Set<ForeignKey> foreignKeysOfTable = getForeignKeysOf(database, tableName);

                // add mapping for each property that a column can be found for
                EntityInfo entityInfo = boxStoreHelper.getEntityInfo(entityClass);
                Property[] properties = entityInfo.getAllProperties();
                for (Property property : properties) {
                    // look for direct mapping of property name -> column name
                    String columnName = property.name;
                    int indexOfColumn = indexOfColumnIn(database, columnName, tableName);
                    // fall back if no match found
                    if (indexOfColumn == -1) {
                        if (property.isId) {
                            // for @Id property try again with '_id'
                            columnName = "_id";
                            indexOfColumn = indexOfColumnIn(database, columnName, tableName);
                        } else if (property.name.endsWith("Id")) {
                            // for potential to-one target ID property, try again without 'Id' suffix
                            String columnFrom = property.name.substring(0, property.name.length() - 2);
                            // ensure that column stores a foreign key
                            if (isForeignKeyColumn(foreignKeysOfTable, columnFrom)) {
                                columnName = columnFrom;
                                indexOfColumn = indexOfColumnIn(database, columnName, tableName);
                                // check if there actually is a ToOne
                                try {
                                    Field field = entityClass.getDeclaredField(columnFrom);
                                    if (ToOne.class.isAssignableFrom(field.getType())) {
                                        field.setAccessible(true);
                                        tableMapping.putColumnMapping(new ColumnMapping(
                                                columnName, indexOfColumn, null, field,
                                                ColumnMapping.FOREIGN_KEY_MAPPER));
                                        continue;
                                    }
                                } catch (NoSuchFieldException ignored) {
                                }
                            }
                        }
                    }
                    if (indexOfColumn != -1) {
                        Field field;
                        try {
                            field = entityClass.getDeclaredField(property.name);
                        } catch (NoSuchFieldException e) {
                            throw new IllegalStateException("Failed to access field '"
                                    + property.name + "' of entity '" + tableName + "'.");
                        }
                        field.setAccessible(true); // to set private fields
                        tableMapping.putColumnMapping(new ColumnMapping(columnName, indexOfColumn,
                                property, field, ColumnMapping.DEFAULT_MAPPER));
                    } else {
                        unmappedProperties.put(property, tableName);
                    }
                }

                tableMap.put(tableName, tableMapping);
            } else {
                unmappedEntities.add(tableName);
            }
        }

        if (throwIfEntityUnmapped && !unmappedEntities.isEmpty()) {
            StringBuilder entities = new StringBuilder();
            for (String unmappedEntity : unmappedEntities) {
                if (entities.length() > 0) {
                    entities.append(",");
                }
                entities.append(unmappedEntity);
            }
            throw new IllegalStateException("Failed to map entities to a table of the same name: "
                    + entities);
        }
        if (throwIfPropertyUnmapped && !unmappedProperties.isEmpty()) {
            StringBuilder properties = new StringBuilder();
            for (Map.Entry<Property, String> entry : unmappedProperties.entrySet()) {
                if (properties.length() > 0) {
                    properties.append(",");
                }
                properties.append(entry.getValue()).append(".").append(entry.getKey().name);
            }
            throw new IllegalStateException("Failed to map properties to a column of the same name: "
                    + properties);
        }
    }

    static boolean isForeignKeyColumn(Set<ForeignKey> foreignKeysOfTable, String columnFrom) {
        for (ForeignKey foreignKey : foreignKeysOfTable) {
            if (foreignKey.columnFrom.equals(columnFrom)) {
                return true;
            }
        }
        return false;
    }

    static boolean tableExistsWithName(SQLiteDatabase database, String tableName) {
        Cursor cursor = database.query("sqlite_master", new String[]{"name"},
                "type='table' AND name=?", new String[]{tableName},
                null, null, null, "1");
        if (cursor == null) {
            return false;
        }
        boolean isTableExisting = cursor.getCount() > 0;
        cursor.close();
        return isTableExisting;
    }

    /**
     * Returns -1 if the column does not exist.
     */
    static int indexOfColumnIn(SQLiteDatabase database, String columnName, String tableName) {
        Cursor cursor = database.query("\"" + tableName + "\"", null, null, null,
                null, null, null, "1");
        if (cursor == null) {
            return -1;
        }
        int columnIndex = cursor.getColumnIndex(columnName);
        cursor.close();
        return columnIndex;
    }

    static Set<ForeignKey> getForeignKeysOf(SQLiteDatabase database, String tableName) {
        Set<ForeignKey> foreignKeys = new HashSet<>();

        Cursor cursor = database.rawQuery("PRAGMA foreign_key_list(\"" + tableName + "\")", null);
        //noinspection TryFinallyCanBeTryWithResources
        try {
            final int idColumnIndex = cursor.getColumnIndex("id");
            final int seqColumnIndex = cursor.getColumnIndex("seq");
            final int tableColumnIndex = cursor.getColumnIndex("table");
            final int fromColumnIndex = cursor.getColumnIndex("from");
            final int toColumnIndex = cursor.getColumnIndex("to");

            // get IDs of multi-column foreign keys
            Set<Integer> multiColumnForeignKeys = new HashSet<>();
            final int count = cursor.getCount();
            for (int position = 0; position < count; position++) {
                cursor.moveToPosition(position);
                final int seq = cursor.getInt(seqColumnIndex);
                if (seq != 0) {
                    multiColumnForeignKeys.add(cursor.getInt(idColumnIndex));
                }
            }

            for (int position = 0; position < count; position++) {
                cursor.moveToPosition(position);
                final int id = cursor.getInt(idColumnIndex);
                if (multiColumnForeignKeys.contains(id)) {
                    continue; // skip, multi-column foreign keys are not supported
                }
                foreignKeys.add(new ForeignKey(
                        cursor.getString(fromColumnIndex),
                        cursor.getString(tableColumnIndex),
                        cursor.getString(toColumnIndex)
                ));
            }
        } finally {
            cursor.close();
        }

        return foreignKeys;
    }

    /**
     * Runs migration using current mapping. A given {@link PostMigrationStep} will be executed
     * after all migration work is done, but still within the migration transaction.
     */
    public void migrate(@Nullable final PostMigrationStep postMigrationStep) {
        boxStore.runInTx(new Runnable() {
            @Override
            public void run() {
                try {
                    migrateImpl();
                } catch (InstantiationException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
                if (postMigrationStep != null) {
                    postMigrationStep.run(database, boxStore);
                }
            }
        });
    }

    private void migrateImpl() throws InstantiationException, IllegalAccessException {
        for (TableMapping tableMapping : tableMap.values()) {
            Box box = tableMapping.box(boxStore);

            Cursor query = tableMapping.query(database);
            //noinspection TryFinallyCanBeTryWithResources unsure if desugar runs on Java level 1.7
            try {
                while (query.moveToNext()) {
                    Object entity = tableMapping.newEntity();
                    tableMapping.mapRow(query, entity, box);
                }
            } finally {
                query.close();
            }
        }
    }

    public interface PostMigrationStep {
        void run(SQLiteDatabase database, BoxStore boxStore);
    }
}
