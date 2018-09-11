# Importing a SQLite database into ObjectBox

This library can help importing data from a SQLite into an ObjectBox database.

To migrate your data you need to define a mapping of your SQLite database schema to ObjectBox entity 
classes.

## Automatic mapping detection
This works if
- table names match with entity class names 
  `CREATE TABLE "Customer" -> '@Entity Customer'`
- and column names match entity property names
  `name TEXT -> String name`

For example if your schema is:
```
CREATE TABLE "Order" (
    _id INTEGER PRIMARY KEY,
    text TEXT,
    customer INTEGER,
    FOREIGN KEY(customer) REFERENCES Customer(_id)
)
```

You need to define an entity class like:
```java
@Entity
class Order {
    @Id
    long id;
    String text;
    ToOne<Customer> customer; 
}
```

Then you can do:
```java
SqlMigration migration = new SqlMigration(database, boxStore);
migration.autoDetect();
migration.migrate();
```

The `_id INTEGER` column is automatically mapped to the `@Id long id` property, and the `FOREIGN KEY` 
column `customer INTEGER` is mapped to the `ToOne<Customer> customer` property.

## Manual mapping
If table and column names do or can not match entity and property names, or you do not want to
migrate all tables and/or columns, specify a manual mapping.

For example, if the table schema is:
```
CREATE TABLE orders (
    _id INTEGER PRIMARY KEY,
    order_text TEXT,
    order_customer INTEGER,
    FOREIGN KEY(order_customer) REFERENCES Customer(_id)
)
```

And the defined entity class is:
```java
@Entity
class Order {
    @Id
    long id;
    String text;
    ToOne<Customer> customer; 
}
```

Then your mapping code should be:
```java
SqlMigration migration = new SqlMigration(database, boxStore);
migration.mapTableToEntity("orders", Order.class)
    .mapColumnToProperty("_id", Order_.id)
    .mapColumnToProperty("order_text", Order_.text)
    .mapForeignKeyColumnToToOne("order_customer", "customer")
    .build();
migration.migrate();
```

## Foreign keys
When mapping foreign keys to to-one relationships the tool reads the foreign key column value as a
`long` and then calls `setTargetId(foreignKey)` on the `ToOne` property.

Multi-part foreign keys are not supported.

To-Many relationships are not supported.

## Post-migration
The `migrate()` step can accept a `PostMigrationStep` which is run in the same transaction, just
 after the migration code has finished. You can use this to tie up to-many relations or do any 
post-processing on the just inserted data.

```java
migration.migrate(new SqlMigration.PostMigrationStep() {
    @Override
    public void run(SQLiteDatabase database, BoxStore boxStore) {
        // runs in the same transaction as migration
    }
});
```

## Customization
If the default implementation does not do what you need it to, `SqlMigration`, `TableMapping` and 
`ColumnMapping` can be subclassed.

We also welcome change suggestions that might benefit others, feel free to create an issue!
