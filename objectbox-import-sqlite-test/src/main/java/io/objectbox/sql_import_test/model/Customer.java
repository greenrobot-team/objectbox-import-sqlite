package io.objectbox.sql_import_test.model;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

@Entity
public class Customer {

    @Id(assignable = true)
    public long id;

    public String name;

}
