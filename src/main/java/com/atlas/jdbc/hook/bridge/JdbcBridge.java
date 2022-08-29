package com.atlas.jdbc.hook.bridge;

import com.atlas.jdbc.config.DatasourceConfig;
import com.atlas.jdbc.hook.utlis.JDBCUtils;
import com.atlas.jdbc.meta.MySqlMetaProvider;
import com.atlas.jdbc.meta.model.ColumnMeta;
import com.atlas.jdbc.meta.model.DatabaseMeta;
import com.atlas.jdbc.meta.model.InstanceMeta;
import com.atlas.jdbc.meta.model.TableMeta;
import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.AtlasClientV2;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntityHeader;
import org.apache.atlas.model.instance.EntityMutationResponse;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.apache.atlas.utils.AtlasJson;
import org.apache.atlas.utils.AuthenticationUtil;
import org.apache.commons.cli.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.configuration.Configuration;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.apache.atlas.type.AtlasTypeUtil.toAtlasRelatedObjectId;

public class JdbcBridge {

    private static final Logger LOG = LoggerFactory.getLogger(JdbcBridge.class);

    public static final String PREFIX = "jdbc";

    public static final String JDBC_TYPE_INSTANCE               = PREFIX + "_instance";
    public static final String JDBC_TYPE_DB                     = PREFIX + "_db";
    public static final String JDBC_TYPE_TABLE                  = PREFIX + "_table";
    public static final String JDBC_TYPE_COLUMN                 = PREFIX + "_column";
    public static final String JDBC_TYPE_INDEX                  = PREFIX + "_index";
    public static final String JDBC_TYPE_FOREIGN_KEY            = PREFIX + "_foreign_key";

    public static final String RELATIONSHIP_INSTANCE_DATABASES              = PREFIX + "_instance_databases";
    public static final String RELATIONSHIP_DB_TABLES                       = PREFIX + "_db_tables";
    public static final String RELATIONSHIP_TABLE_COLUMNS                   = PREFIX + "_table_columns";
    public static final String RELATIONSHIP_TABLE_INDEXES                   = PREFIX + "_table_indexes";
    public static final String RELATIONSHIP_INDEX_COLUMNS                   = PREFIX + "_index_columns";
    public static final String RELATIONSHIP_TABLE_FOREIGN_KEY               = PREFIX + "_table_foreign_key";
    public static final String RELATIONSHIP_FOREIGN_KEY_COLUMNS             = PREFIX + "_foreign_key_columns";
    public static final String RELATIONSHIP_FOREIGN_KEY_TABLE_REFERENCES    = PREFIX + "_foreign_key_table_references";
    public static final String RELATIONSHIP_FOREIGN_KEY_COLUMN_REFERENCES   = PREFIX + "_foreign_key_column_references";

    public static final String ATTRIBUTE_QUALIFIED_NAME            = "qualifiedName";
    public static final String ATTRIBUTE_NAME                      = "name";
    public static final String ATTRIBUTE_OWNER                     = "owner";
    public static final String ATTRIBUTE_COMMENT                   = "comment";
    public static final String ATTRIBUTE_CLUSTER_NAME              = "clusterName";
    public static final String ATTRIBUTE_DESCRIPTION               = "description";

    public static final String ATTRIBUTE_INSTANCE                   = "instance";
    public static final String ATTRIBUTE_DATABASE                   = "database";
    public static final String ATTRIBUTE_TABLE                      = "table";
    public static final String ATTRIBUTE_COLUMNS                    = "columns";
    public static final String ATTRIBUTE_INDEXES                    = "indexes";
    public static final String ATTRIBUTE_FOREIGN_KEYS               = "foreignKeys";

    public static final String ATTRIBUTE_INSTANCE_URL                   = "url";
    public static final String ATTRIBUTE_INSTANCE_PRODUCT_NAME          = "productName";
    public static final String ATTRIBUTE_INSTANCE_PRODUCT_VERSION       = "productVersion";
    public static final String ATTRIBUTE_INSTANCE_DRIVER_NAME           = "driverName";
    public static final String ATTRIBUTE_INSTANCE_DRIVER_VERSION        = "driverVersion";

    public static final String ATTRIBUTE_DB_CATALOG_NAME                = "catalogName";
    public static final String ATTRIBUTE_DB_SCHEMA_NAME                 = "schemaName";

    public static final String ATTRIBUTE_TABLE_NAME                     = "tableName";
    public static final String ATTRIBUTE_TABLE_TYPE                     = "tableType";
    public static final String ATTRIBUTE_TABLE_CREATE_TIME              = "createTime";
    public static final String ATTRIBUTE_TABLE_UPDATE_TIME              = "updateTime";

    public static final String ATTRIBUTE_COLUMN_TYPE                    = "columnType";
    public static final String ATTRIBUTE_COLUMN_IS_NULLABLE             = "isNullable";
    public static final String ATTRIBUTE_COLUMN_AUTO_INCREMENT          = "autoIncrement";
    public static final String ATTRIBUTE_COLUMN_DEFAULT_VALUE           = "defaultValue";

    public static final String ATTRIBUTE_FOREIGN_KEY_UPDATE_RULE        = "updateRule";
    public static final String ATTRIBUTE_FOREIGN_KEY_DELETE_RULE        = "deleteRule";

    public static final String JDBC_DATABASE_QUALIFIED_NAME_FORMAT      = "%s@%s";
    public static final String JDBC_TABLE_QUALIFIED_NAME_FORMAT         = "%s.%s@%s";
    public static final String JDBC_COLUMN_QUALIFIED_NAME_FORMAT         = "%s.%s.%s@%s";

    private final AtlasClientV2 atlasClientV2;

    private final String clusterName;

    private static final int    EXIT_CODE_SUCCESS                 = 0;
    private static final int    EXIT_CODE_FAILED                  = 1;
    private static final String ATLAS_ENDPOINT                    = "atlas.rest.address";
    private static final String DEFAULT_ATLAS_URL                 = "http://localhost:21000/";
    private static final String DATASOURCE_FILE_NAME              = "jdbc-bridge.properties";

    public JdbcBridge(AtlasClientV2 atlasClientV2, String metadataClusterName) {
        this.atlasClientV2 = atlasClientV2;
        this.clusterName = metadataClusterName;
    }

    public static void main(String[] args) {
        int           exitCode      = EXIT_CODE_FAILED;
        AtlasClientV2 atlasClientV2 = null;
        try {
            Options options = new Options();
            options.addOption("d","dir", true, "config directory");

            // 读取启动参数
            CommandLineParser parser    = new BasicParser();
            CommandLine cmd             = parser.parse(options, args);
            String   configDir = cmd.getOptionValue("d");

            // 设置Atlas的配置文件路径
            System.setProperty(ApplicationProperties.ATLAS_CONFIGURATION_DIRECTORY_PROPERTY, configDir);
            Configuration atlasConf     = ApplicationProperties.get();

            String[] urls               = atlasConf.getStringArray(ATLAS_ENDPOINT);
            if (urls == null || urls.length == 0) {
                urls = new String[] { DEFAULT_ATLAS_URL };
            }
            // 加载Atlas Client
            if (!AuthenticationUtil.isKerberosAuthenticationEnabled()) {
                String[] basicAuthUsernamePassword = AuthenticationUtil.getBasicAuthenticationInput();

                atlasClientV2 = new AtlasClientV2(urls, basicAuthUsernamePassword);
            } else {
                UserGroupInformation ugi = UserGroupInformation.getCurrentUser();

                atlasClientV2 = new AtlasClientV2(ugi, ugi.getShortUserName(), urls);
            }
            // 定义元数据模型
            if (!atlasClientV2.typeWithNameExists(JDBC_TYPE_INSTANCE)){
                String json = JDBCUtils.loadTypeDefsJsonFile();
                AtlasTypesDef atlasTypesDef = AtlasJson.fromJson(json, AtlasTypesDef.class);
                atlasClientV2.createAtlasTypeDefs(atlasTypesDef);
            }
            // 加载数据源配置
            DatasourceConfig config = JDBCUtils.lodaConfig(configDir, DATASOURCE_FILE_NAME);
            Connection connection = JDBCUtils.getConnection(config);

            // 读取元数据到Atlas
            MySqlMetaProvider mySqlMetaProvider = new MySqlMetaProvider(connection);
            InstanceMeta instanceMeta = mySqlMetaProvider.getInstanceMeta();
            JdbcBridge jdbcBridge = new JdbcBridge(atlasClientV2, config.getCluster());
            jdbcBridge.createOrUpdateInstance(instanceMeta);
            for (DatabaseMeta databaseMeta : mySqlMetaProvider.getDatabasesMeta()) {
                jdbcBridge.createOrUpdateDatabase(databaseMeta);
            }
            for (TableMeta tableMeta : mySqlMetaProvider.getTablesMeta()) {
                jdbcBridge.createOrUpdateTable(tableMeta);
            }
            for (ColumnMeta columnMeta : mySqlMetaProvider.getColumnsMeta()) {
                jdbcBridge.createOrUpdateColumn(columnMeta);
            }

            exitCode = EXIT_CODE_SUCCESS;

            LOG.info("Successfully imported Meta Data !!!");
        } catch(ParseException e) {
            LOG.error("Failed to parse arguments. Error: {} ", e.getMessage());

            printUsage();
        } catch(Exception e) {
            System.out.println("ImportJdbcEntities failed. Please check the log file for the detailed error message");

            e.printStackTrace();

            LOG.error("ImportJdbcEntities failed", e);
        } finally {
            if (atlasClientV2 != null) {
                atlasClientV2.close();
            }
        }
        System.exit(exitCode);
    }

    private static  void printUsage(){
        System.out.println("Usage: java - jar atlas-jdbc-bridge.jar -d <config directory>");
    }

    public void createOrUpdateInstance(InstanceMeta instanceMeta) throws Exception {
        String qualifiedName = clusterName;
        AtlasEntity.AtlasEntityWithExtInfo entityInAtlas  = findInstanceEntityInAtlas(qualifiedName);

        if (entityInAtlas == null) {
            LOG.info("Importing Jdbc Instance: " + qualifiedName);

            AtlasEntity entity = getInstanceMetaEntity(instanceMeta, null);

            entityInAtlas = createEntityInAtlas(new AtlasEntity.AtlasEntityWithExtInfo(entity));

        } else {
            LOG.info("Jdbc Instance already present in Atlas. Updating it..: " + qualifiedName);

            AtlasEntity entity = getInstanceMetaEntity(instanceMeta, entityInAtlas.getEntity());

            entityInAtlas.setEntity(entity);

            entityInAtlas = updateEntityInAtlas(entityInAtlas);
        }
    }

    private AtlasEntity getInstanceMetaEntity(InstanceMeta instanceMeta, AtlasEntity entity) {
        AtlasEntity ret  = (entity == null)  ?  new AtlasEntity(JDBC_TYPE_INSTANCE) : entity;

        ret.setAttribute(ATTRIBUTE_NAME, clusterName);
        ret.setAttribute(ATTRIBUTE_QUALIFIED_NAME, clusterName);
        ret.setAttribute(ATTRIBUTE_OWNER, instanceMeta.getUserName());
        ret.setAttribute(ATTRIBUTE_DESCRIPTION, instanceMeta.getProductName());

        ret.setAttribute(ATTRIBUTE_INSTANCE_URL, instanceMeta.getUrl());
        ret.setAttribute(ATTRIBUTE_INSTANCE_PRODUCT_NAME, instanceMeta.getProductName());
        ret.setAttribute(ATTRIBUTE_INSTANCE_PRODUCT_VERSION, instanceMeta.getProductVersion());
        ret.setAttribute(ATTRIBUTE_INSTANCE_DRIVER_NAME, instanceMeta.getDriverName());
        ret.setAttribute(ATTRIBUTE_INSTANCE_DRIVER_VERSION, instanceMeta.getDriverVersion());

        return ret;
    }

    public void createOrUpdateDatabase(DatabaseMeta databaseMeta) throws Exception {
        String qualifiedName = getDatabaseQualifiedName(clusterName, databaseMeta.getSchemaName());
        AtlasEntity.AtlasEntityWithExtInfo entityInAtlas = findDatabaseEntityInAtlas(qualifiedName);

        if (entityInAtlas == null) {
            LOG.info("Importing Database: " + qualifiedName);

            AtlasEntity entity = getDatabaseMetaEntity(databaseMeta, null);

            entityInAtlas = createEntityInAtlas(new AtlasEntity.AtlasEntityWithExtInfo(entity));
        } else {
            LOG.info("Database already present in Atlas. Updating it..: " + qualifiedName);

            AtlasEntity entity = getDatabaseMetaEntity(databaseMeta, entityInAtlas.getEntity());

            entityInAtlas.setEntity(entity);

            entityInAtlas = updateEntityInAtlas(entityInAtlas);
        }
    }

    protected AtlasEntity getDatabaseMetaEntity(DatabaseMeta databaseMeta, AtlasEntity entity) {

        AtlasEntity ret  = (entity == null)  ?  new AtlasEntity(JDBC_TYPE_DB) : entity;

        String qualifiedName = getDatabaseQualifiedName(clusterName, databaseMeta.getSchemaName());

        ret.setAttribute(ATTRIBUTE_NAME, databaseMeta.getSchemaName());
        ret.setAttribute(ATTRIBUTE_QUALIFIED_NAME, qualifiedName);
        ret.setAttribute(ATTRIBUTE_OWNER, clusterName);
        ret.setAttribute(ATTRIBUTE_DESCRIPTION, databaseMeta.getSchemaName());

        ret.setAttribute(ATTRIBUTE_DB_SCHEMA_NAME, databaseMeta.getSchemaName());
        ret.setAttribute(ATTRIBUTE_DB_CATALOG_NAME, databaseMeta.getCatalogName());

        AtlasEntity.AtlasEntityWithExtInfo instanceEntityInAtlas = findInstanceEntityInAtlas(clusterName);

        ret.setRelationshipAttribute(ATTRIBUTE_INSTANCE, toAtlasRelatedObjectId(instanceEntityInAtlas.getEntity(), RELATIONSHIP_INSTANCE_DATABASES));

        return ret;
    }

    public void createOrUpdateTable(TableMeta tableMeta) throws Exception {
        String qualifiedName = getTableQualifiedName(clusterName, tableMeta.getDbName(),tableMeta.getTableName());
        AtlasEntity.AtlasEntityWithExtInfo entityInAtlas = findTableEntityInAtlas(qualifiedName);

        if (entityInAtlas == null) {
            LOG.info("Importing Table: " + qualifiedName);

            AtlasEntity entity = getTableMetaEntity(tableMeta, null);

            entityInAtlas = createEntityInAtlas(new AtlasEntity.AtlasEntityWithExtInfo(entity));
        } else {
            LOG.info("Table already present in Atlas. Updating it..: " + qualifiedName);

            AtlasEntity entity = getTableMetaEntity(tableMeta, entityInAtlas.getEntity());

            entityInAtlas.setEntity(entity);

            entityInAtlas = updateEntityInAtlas(entityInAtlas);
        }
    }

    protected AtlasEntity getTableMetaEntity(TableMeta tableMeta, AtlasEntity entity) {
        AtlasEntity ret  = (entity == null)  ?  new AtlasEntity(JDBC_TYPE_TABLE) : entity;
        String qualifiedName = getTableQualifiedName(clusterName, tableMeta.getDbName(), tableMeta.getTableName());
        String dbQualifiedName = getDatabaseQualifiedName(clusterName, tableMeta.getDbName());
        String comment = tableMeta.getTableComment().isEmpty() ? null : tableMeta.getTableComment();

        ret.setAttribute(ATTRIBUTE_NAME, tableMeta.getTableName());
        ret.setAttribute(ATTRIBUTE_QUALIFIED_NAME, qualifiedName);
        ret.setAttribute(ATTRIBUTE_OWNER, dbQualifiedName);
        ret.setAttribute(ATTRIBUTE_COMMENT, comment);

        ret.setAttribute(ATTRIBUTE_TABLE_NAME, tableMeta.getTableName());
        ret.setAttribute(ATTRIBUTE_TABLE_TYPE, tableMeta.getTableType());
        ret.setAttribute(ATTRIBUTE_TABLE_CREATE_TIME, tableMeta.getCreateTime());
        ret.setAttribute(ATTRIBUTE_TABLE_UPDATE_TIME, tableMeta.getUpdateTime());

        AtlasEntity.AtlasEntityWithExtInfo databaseEntityInAtlas = findDatabaseEntityInAtlas(dbQualifiedName);

        ret.setRelationshipAttribute(ATTRIBUTE_DATABASE, toAtlasRelatedObjectId(databaseEntityInAtlas.getEntity(), RELATIONSHIP_DB_TABLES));

        return ret;
    }

    public void createOrUpdateColumn(ColumnMeta columnMeta) throws Exception {
        String qualifiedName = getColumnQualifiedName(clusterName, columnMeta.getDbName(),columnMeta.getTableName(),columnMeta.getColumnName());
        AtlasEntity.AtlasEntityWithExtInfo entityInAtlas = findColumnEntityInAtlas(qualifiedName);

        if (entityInAtlas == null) {
            LOG.info("Importing Column: " + qualifiedName);

            AtlasEntity entity = getColumnMetaEntity(columnMeta, null);

            entityInAtlas = createEntityInAtlas(new AtlasEntity.AtlasEntityWithExtInfo(entity));
        } else {
            LOG.info("Column already present in Atlas. Updating it..: " + qualifiedName);

            AtlasEntity entity = getColumnMetaEntity(columnMeta, entityInAtlas.getEntity());

            entityInAtlas.setEntity(entity);

            entityInAtlas = updateEntityInAtlas(entityInAtlas);
        }
    }

    protected AtlasEntity getColumnMetaEntity(ColumnMeta columnMeta, AtlasEntity entity) throws Exception {
        AtlasEntity ret  = (entity == null)  ?  new AtlasEntity(JDBC_TYPE_COLUMN) : entity;

        String dbName = columnMeta.getDbName();
        String tableName = columnMeta.getTableName();
        String columnName = columnMeta.getColumnName();
        String columnQualifiedName = getColumnQualifiedName(clusterName, dbName, tableName, columnName);
        String tableQualifiedName = getTableQualifiedName(clusterName, dbName, tableName);
        String columnComment = columnMeta.getColumnComment().isEmpty() ? null : columnMeta.getColumnComment();

        ret.setAttribute(ATTRIBUTE_NAME, columnMeta.getColumnName());
        ret.setAttribute(ATTRIBUTE_QUALIFIED_NAME, columnQualifiedName);
        ret.setAttribute(ATTRIBUTE_COMMENT, columnComment);

        ret.setAttribute(ATTRIBUTE_COLUMN_TYPE, columnMeta.getColumnType());
        ret.setAttribute(ATTRIBUTE_COLUMN_AUTO_INCREMENT, columnMeta.getAutoIncrement());
        ret.setAttribute(ATTRIBUTE_COLUMN_DEFAULT_VALUE, columnMeta.getDefaultValue());
        ret.setAttribute(ATTRIBUTE_COLUMN_IS_NULLABLE, columnMeta.getIsNullable());

        AtlasEntity.AtlasEntityWithExtInfo tableEntityInAtlas = findTableEntityInAtlas(tableQualifiedName);

        ret.setRelationshipAttribute(ATTRIBUTE_TABLE, toAtlasRelatedObjectId(tableEntityInAtlas.getEntity(), RELATIONSHIP_TABLE_COLUMNS));

        return ret;
    }

    private String getColumnQualifiedName(String clusterName, String dbName, String tableName, String columnName) {
        return String.format(JDBC_COLUMN_QUALIFIED_NAME_FORMAT, dbName, tableName, columnName, clusterName);
    }

    private String getTableQualifiedName(String clusterName, String dbName, String tableName) {
        return String.format(JDBC_TABLE_QUALIFIED_NAME_FORMAT, dbName, tableName, clusterName);
    }

    private String getDatabaseQualifiedName(String clusterName, String schemaName) {
        return String.format(JDBC_DATABASE_QUALIFIED_NAME_FORMAT, schemaName, clusterName);
    }

    private AtlasEntity.AtlasEntityWithExtInfo findInstanceEntityInAtlas(String qualifiedName) {
        AtlasEntity.AtlasEntityWithExtInfo ret;

        try {
            ret = findEntityInAtlas(JDBC_TYPE_INSTANCE, qualifiedName);
            clearRelationshipAttributes(ret);
        } catch (Exception e) {
            ret = null; // entity doesn't exist in Atlas
        }

        return ret;
    }

    private AtlasEntity.AtlasEntityWithExtInfo findDatabaseEntityInAtlas(String qualifiedName) {
        AtlasEntity.AtlasEntityWithExtInfo ret = null;

        try {
            ret = findEntityInAtlas(JDBC_TYPE_DB, qualifiedName);
            clearRelationshipAttributes(ret);
        } catch (Exception e) {
            ret = null; // entity doesn't exist in Atlas
        }

        return ret;
    }

    private AtlasEntity.AtlasEntityWithExtInfo findTableEntityInAtlas(String qualifiedName) {
        AtlasEntity.AtlasEntityWithExtInfo ret = null;

        try {
            ret = findEntityInAtlas(JDBC_TYPE_TABLE, qualifiedName);
            clearRelationshipAttributes(ret);
        } catch (Exception e) {
            ret = null; // entity doesn't exist in Atlas
        }

        return ret;
    }

    private AtlasEntity.AtlasEntityWithExtInfo findColumnEntityInAtlas(String qualifiedName) {
        AtlasEntity.AtlasEntityWithExtInfo ret = null;

        try {
            ret = findEntityInAtlas(JDBC_TYPE_COLUMN, qualifiedName);
            clearRelationshipAttributes(ret);
        } catch (Exception e) {
            ret = null; // entity doesn't exist in Atlas
        }

        return ret;
    }

    AtlasEntity.AtlasEntityWithExtInfo findEntityInAtlas(String typeName, String qualifiedName) throws Exception {
        Map<String, String> attributes = Collections.singletonMap(ATTRIBUTE_QUALIFIED_NAME, qualifiedName);

        return atlasClientV2.getEntityByAttribute(typeName, attributes);
    }

    AtlasEntity.AtlasEntityWithExtInfo createEntityInAtlas(AtlasEntity.AtlasEntityWithExtInfo entity) throws Exception {
        AtlasEntity.AtlasEntityWithExtInfo ret      = null;
        EntityMutationResponse response = atlasClientV2.createEntity(entity);
        List<AtlasEntityHeader> entities = response.getCreatedEntities();

        if (CollectionUtils.isNotEmpty(entities)) {
            ret = atlasClientV2.getEntityByGuid(entities.get(0).getGuid());

            LOG.info("Created {} entity: name={}, guid={}", ret.getEntity().getTypeName(), ret.getEntity().getAttribute(ATTRIBUTE_QUALIFIED_NAME), ret.getEntity().getGuid());
        }

        return ret;
    }

    AtlasEntity.AtlasEntityWithExtInfo updateEntityInAtlas(AtlasEntity.AtlasEntityWithExtInfo entity) throws Exception {
        AtlasEntity.AtlasEntityWithExtInfo ret;
        EntityMutationResponse response = atlasClientV2.updateEntity(entity);

        if (response != null) {
            List<AtlasEntityHeader> entities = response.getUpdatedEntities();

            if (CollectionUtils.isNotEmpty(entities)) {

                ret = atlasClientV2.getEntityByGuid(entities.get(0).getGuid());

                LOG.info("Updated {} entity: name={}, guid={} ", ret.getEntity().getTypeName(), ret.getEntity().getAttribute(ATTRIBUTE_QUALIFIED_NAME), ret.getEntity().getGuid());
            } else {
                LOG.info("Entity: name={} ", entity.toString() + " not updated as it is unchanged from what is in Atlas" );

                ret = entity;
            }
        } else {
            LOG.info("Entity: name={} ", entity.toString() + " not updated as it is unchanged from what is in Atlas" );

            ret = entity;
        }
        return ret;
    }

    private void clearRelationshipAttributes(AtlasEntity.AtlasEntityWithExtInfo entity) {
        if (entity != null) {
            clearRelationshipAttributes(entity.getEntity());

            if (entity.getReferredEntities() != null) {
                clearRelationshipAttributes(entity.getReferredEntities().values());
            }
        }
    }

    private void clearRelationshipAttributes(Collection<AtlasEntity> entities) {
        if (entities != null) {
            for (AtlasEntity entity : entities) {
                clearRelationshipAttributes(entity);
            }
        }
    }

    private void clearRelationshipAttributes(AtlasEntity entity) {
        if (entity != null && entity.getRelationshipAttributes() != null) {
            entity.getRelationshipAttributes().clear();
        }
    }
}
