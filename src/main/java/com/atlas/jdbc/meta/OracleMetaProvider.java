package com.atlas.jdbc.meta;

import com.atlas.jdbc.meta.provider.MetaProvider;

import java.sql.Connection;

public class OracleMetaProvider extends MetaProvider {

    public OracleMetaProvider(Connection connection) {
        super(connection);
    }

    @Override
    protected String supplyDatabasesMetaSQL() {
        return null;
    }

    @Override
    protected String supplyTablesMetaSQL() {
        return null;
    }

    @Override
    protected String supplyColumnsMetaSQL() {
        return null;
    }

    @Override
    protected String supplyForeignKeysMetaSQL() {
        return null;
    }
}
