# Importing a SQLite database into ObjectBox

This Android library helps with importing data from a SQLite database into ObjectBox.

To migrate your data you need to define a mapping of your SQLite database schema to ObjectBox entity 
classes.

## Automatic mapping detection
This works if
- entity class names match with table names 
  `'@Entity Customer' -> CREATE TABLE "Customer"`
- and entity property names match with column names
  `String name -> name TEXT`

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

By default `autoDetect()` will throw if it could not find a table for an entity, or a column for a
property. You can turn this off using `autoDetect(false /* ignore missing tables */, false /* ignore missing columns */)`.

## Customization
You can remove or modify mappings:
```java
// remove mapping for "Order" table
migration.removeTableMapping("Order");

// remove mapping of "text" column
migration.modifyTableMapping("Order")
        .removeColumnMapping("text")
        .build();
```

You can provide a custom mapper to read and set values:
```java
migration.modifyTableMapping("Order")
        .mapColumnToProperty("text", Order_.text,
                new ColumnMapping.Mapper() {
                    @Override
                    public void mapValue(ColumnMapping mapping, Cursor row, Object entity) {
                        // prefix "text" column values
                        String text = row.getString(mapping.getColumnIndex());
                        mapping.setValue(entity, "ARCHIVED - " + text);
                    }
                })
        .build();
```

## Manual mapping
You can also build a mapping completely by yourself.

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

Then your mapping code could be:
```java
SqlMigration migration = new SqlMigration(database, boxStore);
migration.mapTableToEntity("orders", Order.class)
    .mapColumnToProperty("_id", Order_.id)
    .mapColumnToProperty("order_text", Order_.text,
            new ColumnMapping.Mapper() {
                @Override
                public void mapValue(ColumnMapping mapping, Cursor row, Object entity) {
                    // prefix "text" column values
                    String text = row.getString(mapping.getColumnIndex());
                    mapping.setValue(entity, "ARCHIVED - " + text);
                }
            })
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

## Something else
If you are missing something, we welcome change suggestions that might benefit others, feel free to 
create an issue!
