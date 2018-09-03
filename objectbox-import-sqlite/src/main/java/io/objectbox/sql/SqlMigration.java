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
import java.util.List;
import java.util.Map;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.BoxStoreHelper;
import io.objectbox.EntityInfo;
import io.objectbox.Property;

/**
 * Migrates data from a SQLite database to ObjectBox by mapping tables and columns to entities
 * and properties.
 */
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
     * sensitive. Use {@link #setTableMap(HashMap)} instead to define a custom mapping.
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
            if (tableExistsWithName(tableName)) {
                TableMapping tableMapping = new TableMapping(tableName, entityClass);

                // add mapping for each property that a column can be found for
                EntityInfo entityInfo = boxStoreHelper.getEntityInfo(entityClass);
                Property[] properties = entityInfo.getAllProperties();
                for (Property property : properties) {
                    String columnName = property.name;
                    int indexOfColumn = indexOfColumnIn(columnName, tableName);
                    if (indexOfColumn == -1 && property.isId) {
                        // for @Id property try again with '_id'
                        columnName = "_id";
                        indexOfColumn = indexOfColumnIn(columnName, tableName);
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
                        tableMapping.putColumnMapping(
                                new ColumnMapping(columnName, indexOfColumn, property, field));
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

    private boolean tableExistsWithName(String tableName) {
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
    private int indexOfColumnIn(String columnName, String tableName) {
        Cursor cursor = database.query("\"" + tableName + "\"", null, null, null,
                null, null, null, "1");
        if (cursor == null) {
            return -1;
        }
        int columnIndex = cursor.getColumnIndex(columnName);
        cursor.close();
        return columnIndex;
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
