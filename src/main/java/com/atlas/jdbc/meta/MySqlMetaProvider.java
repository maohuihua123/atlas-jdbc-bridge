package com.atlas.jdbc.meta;

import com.atlas.jdbc.meta.provider.MetaProvider;

import java.sql.Connection;

public class MySqlMetaProvider extends MetaProvider{

    private static final String DB_META_SQL = "select CATALOG_NAME, SCHEMA_NAME from INFORMATION_SCHEMA.SCHEMATA where SCHEMA_NAME not in ('sys','performance_schema','mysql','information_schema')";

    private static final String TB_META_SQL = "select TABLE_SCHEMA, TABLE_NAME, TABLE_TYPE, TABLE_COMMENT, CREATE_TIME, UPDATE_TIME from INFORMATION_SCHEMA.TABLES where TABLE_SCHEMA not in ('sys','performance_schema','mysql','information_schema')";

    private static final String COL_META_SQL = "select TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME, COLUMN_COMMENT, COLUMN_TYPE, COLUMN_DEFAULT, IS_NULLABLE, EXTRA from INFORMATION_SCHEMA.COLUMNS where TABLE_SCHEMA not in ('sys','performance_schema','mysql','information_schema')";

    private static final String FK_META_SQL = "select C.TABLE_SCHEMA, C.REFERENCED_TABLE_NAME, C.REFERENCED_COLUMN_NAME, C.TABLE_NAME, C.COLUMN_NAME, C.CONSTRAINT_NAME, R.UPDATE_RULE, R.DELETE_RULE from INFORMATION_SCHEMA.KEY_COLUMN_USAGE C JOIN INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS R ON R.TABLE_NAME = C.TABLE_NAME AND R.CONSTRAINT_NAME = C.CONSTRAINT_NAME AND R.REFERENCED_TABLE_NAME = C.REFERENCED_TABLE_NAME WHERE C.REFERENCED_TABLE_NAME IS NOT NULL;";

    public MySqlMetaProvider(Connection connection) {
        super(connection);
    }

    @Override
    protected String supplyDatabasesMetaSQL() {
        return DB_META_SQL;
    }

    @Override
    protected String supplyTablesMetaSQL() {
        return TB_META_SQL;
    }

    @Override
    protected String supplyColumnsMetaSQL() {
        return COL_META_SQL;
    }

    @Override
    protected String supplyForeignKeysMetaSQL() {
        return FK_META_SQL;
    }
}
