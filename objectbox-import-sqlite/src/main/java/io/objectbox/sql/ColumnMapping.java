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

import java.lang.reflect.Field;
import java.util.Date;

import io.objectbox.Property;

/**
 * Maps a SQLite column to a property of an ObjectBox entity. The value of the column will be set as
 * the value of the property.
 */
@SuppressWarnings("WeakerAccess")
public class ColumnMapping {

    private final String columnName;
    private final int columnIndex;
    private final Property property;
    private final Field field;

    public ColumnMapping(String columnName, int columnIndex, Property property, Field field) {
        this.columnName = columnName;
        this.columnIndex = columnIndex;
        this.property = property;
        this.field = field;
    }

    public String getColumnName() {
        return columnName;
    }

    public int getColumnIndex() {
        return columnIndex;
    }

    public Field getField() {
        return field;
    }

    /**
     * Maps the value of this column to the assigned entity property. You might want to override
     * this method to add custom behavior for mapping a column to an entity property.
     */
    public void mapValue(Cursor row, Object entity) throws IllegalAccessException {
        // get value and assign it to the entity field
        int columnIndex = getColumnIndex();
        if (property.type == boolean.class || property.type == Boolean.class) {
            boolean value = row.getInt(columnIndex) == 1;
            field.set(entity, value);
        } else if (property.type == int.class || property.type == Integer.class) {
            int value = row.getInt(columnIndex);
            field.set(entity, value);
        } else if (property.type == short.class || property.type == Short.class) {
            short value = row.getShort(columnIndex);
            field.set(entity, value);
        } else if (property.type == long.class || property.type == Long.class) {
            long value = row.getLong(columnIndex);
            field.set(entity, value);
        } else if (property.type == float.class || property.type == Float.class) {
            float value = row.getFloat(columnIndex);
            field.set(entity, value);
        } else if (property.type == double.class || property.type == Double.class) {
            double value = row.getDouble(columnIndex);
            field.set(entity, value);
        } else if (property.type == byte.class || property.type == Byte.class) {
            // Android stores Byte as INTEGER
            int value = row.getInt(columnIndex);
            field.set(entity, (byte) value);
        } else if (property.type == byte[].class) {
            byte[] value = row.getBlob(columnIndex);
            field.set(entity, value);
        } else if (property.type == String.class) {
            String value = row.getString(columnIndex);
            field.set(entity, value);
        } else if (property.type == Date.class) {
            long value = row.getLong(columnIndex);
            field.set(entity, new Date(value));
        } else {
            throw new IllegalArgumentException("No mapping for property type " + property.type);
        }
    }
}
