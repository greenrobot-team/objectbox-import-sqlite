package io.objectbox.sql_import_test.model;

import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.relation.ToOne;

@Entity
public class Order {

    @Id(assignable = true)
    public long id;

    public String text;

    public long customerId; // TODO use ToOne.setTargetId() directly
    public ToOne<Customer> customer;

}
