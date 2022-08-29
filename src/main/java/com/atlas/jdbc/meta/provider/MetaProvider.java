package com.atlas.jdbc.meta.provider;

import com.atlas.jdbc.meta.model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public abstract class MetaProvider implements AutoCloseable {

    protected final Connection connection;

    public MetaProvider(Connection connection) {
        this.connection = connection;
    }

    protected abstract String supplyDatabasesMetaSQL();

    protected abstract String supplyTablesMetaSQL();

    protected abstract String supplyColumnsMetaSQL();

    protected abstract String supplyForeignKeysMetaSQL();

   public InstanceMeta getInstanceMeta(){
       try {
           DatabaseMetaData metaData = connection.getMetaData();
           String url = metaData.getURL();
           String userName = metaData.getUserName();
           String productName = metaData.getDatabaseProductName();
           String productVersion = metaData.getDatabaseProductVersion();
           String driverName = metaData.getDriverName();
           String driverVersion = metaData.getDriverVersion();

           return InstanceMeta.builder()
                   .url(url).userName(userName)
                   .productName(productName).productVersion(productVersion)
                   .driverName(driverName).driverVersion(driverVersion)
                   .build();
       } catch (SQLException e) {
           throw new IllegalStateException(e);
       }
   }

    public List<DatabaseMeta> getDatabasesMeta() throws SQLException {
        List<DatabaseMeta> databaseMetas = new ArrayList<>();
        PreparedStatement ps = connection.prepareStatement(supplyDatabasesMetaSQL());
        ResultSet result = ps.executeQuery();
        while(result.next()){
            String catalogName = result.getString(1);
            String schemaName = result.getString(2);
            DatabaseMeta meta = DatabaseMeta.builder()
                    .catalogName(catalogName)
                    .schemaName(schemaName)
                    .build();
            databaseMetas.add(meta);
        }
        return databaseMetas;
    }

    public List<TableMeta> getTablesMeta() throws SQLException{
        List<TableMeta> tableMetas = new ArrayList<>();
        PreparedStatement ps = connection.prepareStatement(supplyTablesMetaSQL());
        ResultSet result = ps.executeQuery();
        while(result.next()){
            String tableSchema = result.getString(1);
            String tableName = result.getString(2);
            String tableType = result.getString(3);
            String tableComment = result.getString(4);
            String createTime = result.getString(5);
            String updateTime = result.getString(6);
            TableMeta tableMeta = TableMeta.builder()
                    .dbName(tableSchema).tableName(tableName)
                    .tableComment(tableComment).tableType(tableType)
                    .createTime(createTime).updateTime(updateTime)
                    .build();
            tableMetas.add(tableMeta);
        }
        return tableMetas;
    }

    public List<ColumnMeta> getColumnsMeta() throws SQLException{
        List<ColumnMeta> columnMetas = new ArrayList<>();
        PreparedStatement ps = connection.prepareStatement(supplyColumnsMetaSQL());
        ResultSet result = ps.executeQuery();
        while(result.next()){
            String tableSchema = result.getString(1);
            String tableName = result.getString(2);
            String columnName = result.getString(3);
            String columnComment = result.getString(4);
            String columnType = result.getString(5);
            String columnDefault = result.getString(6);
            String isNullable = result.getString(7);
            String autoIncrement = result.getString(8);
            ColumnMeta columnMeta = ColumnMeta.builder()
                    .dbName(tableSchema).tableName(tableName).columnName(columnName)
                    .columnComment(columnComment).columnType(columnType)
                    .defaultValue(columnDefault)
                    .isNullable(isNullable)
                    .autoIncrement(autoIncrement)
                    .build();
            columnMetas.add(columnMeta);
        }
        return columnMetas;
    }

    public List<ForeignKeyMeta> getForeignKeysMeta() throws SQLException{
        List<ForeignKeyMeta> foreignKeyMetas = new ArrayList<>();
        PreparedStatement ps = connection.prepareStatement(supplyForeignKeysMetaSQL());
        ResultSet result = ps.executeQuery();
        while(result.next()){
            String tableSchema = result.getString(1);
            String fkTableName = result.getString(2);
            String fkColumnName = result.getString(3);
            String pkTableName = result.getString(4);
            String pkColumnName = result.getString(5);
            String fkName = result.getString(6);
            String updateRule = result.getString(7);
            String deleteRule = result.getString(8);
            ForeignKeyMeta keyMeta = ForeignKeyMeta.builder()
                    .dbName(tableSchema).fkTableName(fkTableName).fkColumnName(fkColumnName)
                    .fkName(fkName).pkTableName(pkTableName).pkColumnName(pkColumnName)
                    .updateRule(updateRule).deleteRule(deleteRule)
                    .build();
            foreignKeyMetas.add(keyMeta);
        }
        return foreignKeyMetas;
    }

    @Override
    public void close() throws Exception {
        if (connection != null){
            connection.close();
        }
    }
}
