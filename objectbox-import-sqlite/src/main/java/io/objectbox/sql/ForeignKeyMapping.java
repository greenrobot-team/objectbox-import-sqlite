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

import io.objectbox.relation.ToOne;

/**
 * Maps a SQLite foreign key column to a ToOne property of an ObjectBox entity. The value of the
 * column will be set as the target ID of the ToOne.
 */
@SuppressWarnings("WeakerAccess")
public class ForeignKeyMapping extends ColumnMapping {

    public ForeignKeyMapping(String columnName, int columnIndex, Field field) {
        super(columnName, columnIndex, null, field);
    }

    public void mapValue(Cursor row, Object entity) throws IllegalAccessException {
        if (!ToOne.class.isAssignableFrom(getField().getType())) {
            throw new IllegalArgumentException("Field " + getColumnName() + " is not a ToOne.");
        }
        // get value and assign it to the entity ToOne field
        long value = row.getLong(getColumnIndex());
        ToOne toOne = (ToOne) getField().get(entity);
        toOne.setTargetId(value);
    }
}
