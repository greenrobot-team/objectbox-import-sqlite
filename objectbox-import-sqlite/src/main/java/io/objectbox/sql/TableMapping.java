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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.BoxStoreHelper;
import io.objectbox.EntityInfo;
import io.objectbox.Property;
import io.objectbox.relation.ToOne;

public class TableMapping {

    private final String tableName;
    private final Class entityClass;

    private final Map<String, ColumnMapping> columnMap;

    private TableMapping(Builder builder) {
        this.tableName = builder.tableName;
        this.entityClass = builder.entityClass;
        this.columnMap = builder.columnMap;
    }

    public TableMapping(String tableName, Class entityClass) {
        this.tableName = tableName;
        this.entityClass = entityClass;
        this.columnMap = new HashMap<>();
    }

    /**
     * Adds the given mapping to the column map using the column name as key. Returns the value of
     * any previous mapping with the specified key or null if there was no mapping.
     */
    @Nullable
    public ColumnMapping putColumnMapping(ColumnMapping columnMapping) {
        return columnMap.put(columnMapping.getColumnName(), columnMapping);
    }

    public Box box(BoxStore boxStore) {
        return boxStore.boxFor(entityClass);
    }

    public Cursor query(SQLiteDatabase db) {
        return db.query("\"" + tableName + "\"", null, null, null, null, null, null);
    }

    public Object newEntity() throws IllegalAccessException, InstantiationException {
        return entityClass.newInstance();
    }

    /**
     * Maps a row based on the current column map to an entity and then puts it into the box.
     * You might want to override this method to add custom behavior for mapping a row to an entity.
     */
    public void mapRow(Cursor row, Object entity, Box box) {
        for (ColumnMapping columnMapping : columnMap.values()) {
            columnMapping.mapValue(row, entity);
        }
        //noinspection unchecked Type is not known
        box.put(entity);
    }

    @VisibleForTesting
    public String getTableName() {
        return tableName;
    }

    @VisibleForTesting
    public Class getEntityClass() {
        return entityClass;
    }

    @VisibleForTesting
    public Map<String, ColumnMapping> getColumnMap() {
        return columnMap;
    }

    public static class Builder {
        private final SQLiteDatabase database;
        private final Map<String, TableMapping> tableMap;

        private final String tableName;
        private final Set<ForeignKey> foreignKeys;

        private final Class entityClass;
        private final EntityInfo entityInfo;

        private Map<String, ColumnMapping> columnMap = new HashMap<>();

        public Builder(SQLiteDatabase database, BoxStore boxStore, Map<String,
                TableMapping> tableMap, String tableName, Class entityClass) {
            if (!SqlMigration.tableExistsWithName(database, tableName)) {
                throw new IllegalArgumentException("There is no table called '" + tableName + "'");
            }
            if (!boxStore.getAllEntityClasses().contains(entityClass)) {
                throw new IllegalArgumentException("There is no entity '" + entityClass + "'");
            }
            this.database = database;
            this.tableMap = tableMap;
            this.tableName = tableName;
            this.entityClass = entityClass;

            foreignKeys = SqlMigration.getForeignKeysOf(database, tableName);
            entityInfo = new BoxStoreHelper(boxStore).getEntityInfo(entityClass);
        }

        /**
         * Maps a column to a property. This works for supported and custom property types.
         * <p/>
         * A simple example that maps the '_id' column to the 'id' property:
         * <pre>
         * migration.mapTableToEntity("customers", Customer.class)
         *     .mapColumnToProperty("_id", Customer_.id)
         *     .build();
         * </pre>
         *
         * @see #mapForeignKeyColumnToToOne(String, String)
         */
        public Builder mapColumnToProperty(String columnName, Property property) {
            return mapColumnToProperty(columnName, property, ColumnMapping.DEFAULT_MAPPER);
        }

        /**
         * Maps a column to a property. This works for supported and custom property types. Pass
         * a mapper to customize how a row value is mapped to a property value.
         * <p/>
         * A simple example that maps the '_id' column to the 'id' property:
         * <pre>
         * migration.mapTableToEntity("customers", Customer.class)
         *     .mapColumnToProperty("_id", Customer_.id)
         *     .build();
         * </pre>
         *
         * @see #mapColumnToProperty(String, Property)
         * @see #mapForeignKeyColumnToToOne(String, String)
         */
        public Builder mapColumnToProperty(String columnName, Property property,
                                           @Nullable ColumnMapping.Mapper mapper) {
            int indexOfColumn = SqlMigration.indexOfColumnIn(database, columnName, tableName);
            if (indexOfColumn == -1) {
                throw new IllegalArgumentException("There is no column '" + columnName
                        + "' in table '" + tableName + "'");
            }
            boolean entityHasProperty = false;
            for (Property entityProperty : entityInfo.getAllProperties()) {
                if (entityProperty.equals(property)) {
                    entityHasProperty = true;
                    break;
                }
            }
            if (!entityHasProperty) {
                throw new IllegalArgumentException("There is no property '" + property.name
                        + "' in entity '" + entityInfo.getEntityName() + "'");
            }
            Field field;
            try {
                field = entityClass.getDeclaredField(property.name);
            } catch (NoSuchFieldException e) {
                throw new IllegalStateException("Failed to access field '" + property.name
                        + "' of entity '" + entityInfo.getEntityName() + "'");
            }
            field.setAccessible(true); // to set private fields

            ColumnMapping columnMapping = new ColumnMapping(columnName, indexOfColumn, property,
                    field, mapper);
            columnMap.put(columnMapping.getColumnName(), columnMapping);
            return this;
        }

        /**
         * Maps a foreign key column to a ToOne property.
         *
         * <pre>
         * // To map column 'order_customer' to the Order.customer ToOne property
         * migration.mapTableToEntity("orders", Order.class)
         *         .mapForeignKeyColumnToToOne("order_customer", "customer")
         *         .build();
         * </pre>
         */
        public Builder mapForeignKeyColumnToToOne(String columnName, String toOneName) {
            int indexOfColumn = SqlMigration.indexOfColumnIn(database, columnName, tableName);
            if (indexOfColumn == -1) {
                throw new IllegalArgumentException("There is no column '" + columnName
                        + "' in table '" + tableName + "'");
            }
            if (!SqlMigration.isForeignKeyColumn(foreignKeys, columnName)) {
                throw new IllegalArgumentException("'" + columnName + "' is not a FOREIGN KEY column");
            }
            Field field;
            try {
                field = entityClass.getDeclaredField(toOneName);
                if (!ToOne.class.isAssignableFrom(field.getType())) {
                    throw new IllegalArgumentException("Field '" + toOneName + "' is not a ToOne");
                }
            } catch (NoSuchFieldException e) {
                throw new IllegalStateException("Failed to access field '" + toOneName
                        + "' of entity '" + entityInfo.getEntityName() + "'");
            }
            field.setAccessible(true); // to set private fields

            ColumnMapping foreignKeyMapping = new ColumnMapping(columnName, indexOfColumn,
                    null, field, ColumnMapping.FOREIGN_KEY_MAPPER);
            columnMap.put(foreignKeyMapping.getColumnName(), foreignKeyMapping);
            return this;
        }

        /**
         * Constructs a new {@link TableMapping} and adds it to the table map.
         */
        public void build() {
            if (columnMap.isEmpty()) {
                throw new IllegalStateException("Table mapping has no mapping for columns");
            }
            tableMap.put(tableName, new TableMapping(this));
        }
    }
}
