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

import java.util.HashMap;
import java.util.Map;

import io.objectbox.Box;
import io.objectbox.BoxStore;

public class TableMapping {

    private String tableName;
    private Class entityClass;

    private Map<String, ColumnMapping> columnMap = new HashMap<>();

    public TableMapping(String tableName, Class entityClass) {
        this.tableName = tableName;
        this.entityClass = entityClass;
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
    public void mapRow(Cursor row, Object entity, Box box) throws IllegalAccessException {
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
}
