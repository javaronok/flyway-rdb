package org.flywaydb.database.rdb;

import org.flywaydb.core.internal.database.base.Table;
import org.flywaydb.core.internal.jdbc.JdbcTemplate;

import java.sql.SQLException;

public class RdbTable extends Table<RedDatabase, RdbSchema> {

    public RdbTable(JdbcTemplate jdbcTemplate, RedDatabase database, RdbSchema schema, String name) {
        super(jdbcTemplate, database, schema, name);
    }

    @Override
    protected void doDrop() throws SQLException {
        jdbcTemplate.execute("DROP TABLE " + this);
    }

    @Override
    protected boolean doExists() throws SQLException {
        return jdbcTemplate.queryForInt("select count(*) from RDB$RELATIONS\n" +
                                                "where RDB$RELATION_NAME = ?\n" +
                                                "and RDB$VIEW_BLR is null", name) > 0;
    }

    @Override
    protected void doLock() throws SQLException {
        /*
         RDB has row-level locking on all transaction isolation levels (this requires fetching the row to lock).
         Table-level locks can only be reserved in SERIALIZABLE (isc_tpb_consistency) with caveats.
         This approach will read all records from table (without roundtrips to the server) to locking all records; it
         will not claim a table-level lock unless the isolation level is SERIALIZABLE. This means that inserts are
         still possible as are selects that don't use 'with lock'.
        */
        jdbcTemplate.execute("execute block as\n"
                                     + "declare tempvar integer;\n"
                                     + "begin\n"
                                     + "  for select 1 from " + this + " with lock into :tempvar do\n"
                                     + "  begin\n"
                                     + "  end\n"
                                     + "end");
    }

    @Override
    public String toString() {
        // No schema, only plain table name
        return database.doQuote(name);
    }
}