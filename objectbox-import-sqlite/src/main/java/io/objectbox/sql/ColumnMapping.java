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
import android.support.annotation.Nullable;

import java.lang.reflect.Field;
import java.util.Date;

import io.objectbox.Property;
import io.objectbox.converter.PropertyConverter;
import io.objectbox.relation.ToOne;

/**
 * Maps a SQLite column to a property of an ObjectBox entity. The value of the column will be set as
 * the value of the property. There is a default mapper which uses the
 */
@SuppressWarnings("WeakerAccess")
public class ColumnMapping {

    public static final Mapper DEFAULT_MAPPER = new DefaultMapper();
    public static final Mapper FOREIGN_KEY_MAPPER = new ForeignKeyMapper();

    private final String columnName;
    private final int columnIndex;
    @Nullable
    private final Property property;
    private final Field field;
    private final Mapper mapper;

    ColumnMapping(String columnName, int columnIndex, @Nullable Property property, Field field,
                  Mapper mapper) {
        this.columnName = columnName;
        this.columnIndex = columnIndex;
        this.property = property;
        this.field = field;
        this.mapper = mapper;
    }

    public String getColumnName() {
        return columnName;
    }

    public int getColumnIndex() {
        return columnIndex;
    }

    @Nullable
    public Property getProperty() {
        return property;
    }

    public Field getField() {
        return field;
    }

    /**
     * Maps the value of this column to the assigned entity property.
     */
    void mapValue(Cursor row, Object entity) {
        mapper.mapValue(this, row, entity);
    }

    /**
     * Sets the value to the associated property field. If a converter is specified for the property,
     * converts the value first.
     */
    public void setValue(Object entity, Object value) {
        if (property != null && property.customType != null) {
            try {
                PropertyConverter converter = (PropertyConverter) property.converterClass.newInstance();
                //noinspection unchecked only know types at runtime
                value = converter.convertToEntityProperty(value);
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException("Failed to create converter for property " + property.name);
            } catch (InstantiationException e) {
                throw new IllegalArgumentException("Failed to create converter for property " + property.name);
            }
        }
        try {
            field.set(entity, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to set value for " + field.getName(), e);
        }
    }

    public interface Mapper {
        /**
         * Maps and sets the value at {@code mapping.getColumnIndex()} of the current {@code row}.
         * Call {@code mapping.setValue(entity, value)} with the mapped value to set it to the
         * property field.
         * <p/>
         * If you need something more elaborate, you can also set the value yourself. You can access
         * the field through {@code mapping.getField()}.
         */
        void mapValue(ColumnMapping mapping, Cursor row, Object entity);
    }

    public static class DefaultMapper implements Mapper {

        @Override
        public void mapValue(ColumnMapping mapping, Cursor row, Object entity) {
            int columnIndex = mapping.getColumnIndex();

            Property property = mapping.getProperty();
            Object value;
            if (property == null) {
                throw new IllegalArgumentException("Property required to map " + mapping.getColumnName());
            } else if (!property.type.isPrimitive() && row.isNull(columnIndex)) {
                value = null;
            } else if (property.type == boolean.class || property.type == Boolean.class) {
                value = row.getInt(columnIndex) == 1;
            } else if (property.type == int.class || property.type == Integer.class) {
                value = row.getInt(columnIndex);
            } else if (property.type == short.class || property.type == Short.class) {
                value = row.getShort(columnIndex);
            } else if (property.type == long.class || property.type == Long.class) {
                value = row.getLong(columnIndex);
            } else if (property.type == float.class || property.type == Float.class) {
                value = row.getFloat(columnIndex);
            } else if (property.type == double.class || property.type == Double.class) {
                value = row.getDouble(columnIndex);
            } else if (property.type == byte.class || property.type == Byte.class) {
                // Android stores Byte as INTEGER
                value = (byte) row.getInt(columnIndex);
            } else if (property.type == byte[].class) {
                value = row.getBlob(columnIndex);
            } else if (property.type == String.class) {
                value = row.getString(columnIndex);
            } else if (property.type == Date.class) {
                value = new Date(row.getLong(columnIndex));
            } else {
                throw new IllegalArgumentException("No mapping for property type " + property.type);
            }

            mapping.setValue(entity, value);
        }
    }

    public static class ForeignKeyMapper implements Mapper {

        @Override
        public void mapValue(ColumnMapping mapping, Cursor row, Object entity) {
            Field field = mapping.getField();
            if (!ToOne.class.isAssignableFrom(field.getType())) {
                throw new IllegalArgumentException("Field " + mapping.getColumnName()
                        + " is not a ToOne.");
            }
            // get value and assign it to the entity ToOne field
            long value = row.getLong(mapping.getColumnIndex());
            try {
                ToOne toOne = (ToOne) field.get(entity);
                toOne.setTargetId(value);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to access ToOne field " + field.getName());
            }
        }
    }

}
