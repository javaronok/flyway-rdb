package org.flywaydb.database.rdb;

import org.flywaydb.core.internal.database.base.Connection;
import org.flywaydb.core.internal.database.base.Schema;

public class RdbConnection extends Connection<RedDatabase> {

    private static final String DUMMY_SCHEMA_NAME = "default";

    RdbConnection(RedDatabase database, java.sql.Connection connection) {
        super(database, connection);
    }

    @Override
    protected String getCurrentSchemaNameOrSearchPath() {
        return DUMMY_SCHEMA_NAME;
    }

    @Override
    public Schema getSchema(String name) {
        // database == schema, always return the same dummy schema
        return new RdbSchema(jdbcTemplate, database, DUMMY_SCHEMA_NAME);
    }
}