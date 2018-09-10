package io.objectbox.sql_import_test.model;

import java.util.Date;

import io.objectbox.annotation.Convert;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.converter.PropertyConverter;

@Entity
public class SimpleEntity {

    // mix private and public to test if set is always possible

    @Id(assignable = true)
    private long id;

    // types with built-in support (http://objectbox.io/documentation/custom-types/)

    private boolean simpleBoolean;
    public Boolean nullableBoolean;

    public int simpleInteger;
    private Integer nullableInteger;

    private short simpleShort;
    public Short nullableShort;

    public long simpleLong;
    private Long nullableLong;

    private float simpleFloat;
    public Float nullableFloat;

    public double simpleDouble;
    private Double nullableDouble;

    private byte simpleByte;
    public Byte nullableByte;

    private byte[] byteArray;

    public String text;

    private Date date;

    @Convert(converter = ModeConverter.class, dbType = Integer.class)
    Mode mode;

    public SimpleEntity() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public boolean isSimpleBoolean() {
        return simpleBoolean;
    }

    public void setSimpleBoolean(boolean simpleBoolean) {
        this.simpleBoolean = simpleBoolean;
    }

    public Integer getNullableInteger() {
        return nullableInteger;
    }

    public void setNullableInteger(Integer nullableInteger) {
        this.nullableInteger = nullableInteger;
    }

    public short getSimpleShort() {
        return simpleShort;
    }

    public void setSimpleShort(short simpleShort) {
        this.simpleShort = simpleShort;
    }

    public Long getNullableLong() {
        return nullableLong;
    }

    public void setNullableLong(Long nullableLong) {
        this.nullableLong = nullableLong;
    }

    public float getSimpleFloat() {
        return simpleFloat;
    }

    public void setSimpleFloat(float simpleFloat) {
        this.simpleFloat = simpleFloat;
    }

    public Double getNullableDouble() {
        return nullableDouble;
    }

    public void setNullableDouble(Double nullableDouble) {
        this.nullableDouble = nullableDouble;
    }

    public byte getSimpleByte() {
        return simpleByte;
    }

    public void setSimpleByte(byte simpleByte) {
        this.simpleByte = simpleByte;
    }

    public byte[] getByteArray() {
        return byteArray;
    }

    public void setByteArray(byte[] byteArray) {
        this.byteArray = byteArray;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public static class ModeConverter implements PropertyConverter<Mode, Integer> {

        @Override
        public Mode convertToEntityProperty(Integer databaseValue) {
            return Mode.fromId(databaseValue);
        }

        @Override
        public Integer convertToDatabaseValue(Mode entityProperty) {
            return entityProperty.toId();
        }
    }
}
