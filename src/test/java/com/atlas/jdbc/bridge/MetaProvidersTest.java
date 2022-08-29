package com.atlas.jdbc.bridge;

import com.atlas.jdbc.config.DatasourceConfig;
import com.atlas.jdbc.hook.bridge.JdbcBridge;
import com.atlas.jdbc.hook.utlis.JDBCUtils;
import com.atlas.jdbc.meta.MySqlMetaProvider;
import com.atlas.jdbc.meta.model.*;
import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.AtlasClientV2;
import org.apache.atlas.AtlasException;
import org.apache.atlas.AtlasServiceException;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.apache.atlas.utils.AtlasJson;
import org.apache.commons.configuration.Configuration;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class MetaProvidersTest {
    public Connection getConnection() {
        Connection conn = null;
        String driver = "com.mysql.cj.jdbc.Driver";
        String url = "jdbc:mysql://localhost:3306";
//        String url = "jdbc:mysql://1.117.222.245:3306";
        String user = "root";
//        String password = "Mhh1996!";
        String password = "root";
        try {
            Class.forName(driver);
            conn = DriverManager.getConnection(url, user, password);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return conn;
    }
    public AtlasClientV2 getAtlasClientV2() {
        String[] baseUrl = {"http://172.36.97.151:21000"};
//        String[] baseUrl = {"http://atlas.rainbou.top:8088"};
//        String[] baseUrl = {"http://120.48.156.61:21000"};

        String[] basicAuthUserNamePassword = {"admin", "admin"};

        return new AtlasClientV2(baseUrl, basicAuthUserNamePassword);
    }

    public static String loadTypeDefsFile(String fileName) {
        File file = new File(fileName);
        StringBuilder sb = new StringBuilder();
        try (
                FileReader fr = new FileReader(file);
                BufferedReader br = new BufferedReader(fr)
        ) {
            String str;
            while ((str = br.readLine()) != null) {
                sb.append(str);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }


    @Test
    void createTypeDefs() throws AtlasServiceException {
        AtlasClientV2 atlasClientV2 = getAtlasClientV2();
        String json = loadTypeDefsFile("src/main/resources/jdbc-model.json");
        AtlasTypesDef atlasTypesDef = AtlasJson.fromJson(json, AtlasTypesDef.class);
        atlasClientV2.createAtlasTypeDefs(atlasTypesDef);
    }

    @Test
    void metaProvider() throws Exception {
        MySqlMetaProvider mySqlMetaProvider = new MySqlMetaProvider(getConnection());
        InstanceMeta instanceMeta = mySqlMetaProvider.getInstanceMeta();
        System.out.println(instanceMeta);
        mySqlMetaProvider.getDatabasesMeta().forEach(System.out::println);
        mySqlMetaProvider.getTablesMeta().forEach(System.out::println);
        mySqlMetaProvider.getColumnsMeta().forEach(System.out::println);
        mySqlMetaProvider.getForeignKeysMeta().forEach(System.out::println);
    }

    @Test
    void metaImportToAtlas() throws Exception {
        MySqlMetaProvider mySqlMetaProvider = new MySqlMetaProvider(getConnection());
        InstanceMeta instanceMeta = mySqlMetaProvider.getInstanceMeta();
        JdbcBridge jdbcBridge = new JdbcBridge(getAtlasClientV2(), "mysql");
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
    }

    @Test
    void loadProperty() throws IOException, AtlasException {
        InputStream inputStream = new FileInputStream("src/main/resources/jdbc-bridge.properties");
        Properties properties = new Properties();
        properties.load(inputStream);
        properties.list(System.out);
        System.out.println("==============================================");
        String property = properties.getProperty("jdbc.url");
        System.out.println("property = " + property);

        DatasourceConfig dataSourceConfig = JDBCUtils.lodaConfig("src/main/resources","jdbc-bridge.properties");
        System.out.println(dataSourceConfig);

        String jsonFile = JDBCUtils.loadTypeDefsJsonFile();
        System.out.println(jsonFile);

        System.setProperty(ApplicationProperties.ATLAS_CONFIGURATION_DIRECTORY_PROPERTY, "C:\\Users\\mhh\\Desktop");

        Configuration atlasConf     = ApplicationProperties.get();
        System.out.println(Arrays.toString(atlasConf.getStringArray("atlas.rest.address")));
    }

    @Test
    void testOpenLooKeng() throws SQLException {
        String url = "jdbc:lk://172.36.97.151:8090";
        Connection connection = DriverManager.getConnection(url, "lk", null);
        DatabaseMetaData metaData = connection.getMetaData();
        String metaUrl = metaData.getURL();
        String userName = metaData.getUserName();
        String productName = metaData.getDatabaseProductName();
        String productVersion = metaData.getDatabaseProductVersion();
        String driverName = metaData.getDriverName();
        String driverVersion = metaData.getDriverVersion();
        InstanceMeta build = InstanceMeta.builder()
                .url(metaUrl).userName(userName)
                .productName(productName).productVersion(productVersion)
                .driverName(driverName).driverVersion(driverVersion)
                .build();
        System.out.println(build);

        List<DatabaseMeta> catalogMetas = new ArrayList<>();
        ResultSet catalogs = connection.getMetaData().getCatalogs();
        while (catalogs.next()) {
            String catalogName = catalogs.getString("TABLE_CAT");
            DatabaseMeta meta = DatabaseMeta.builder()
                    .catalogName(catalogName)
                    .build();
            catalogMetas.add(meta);
        }
        catalogMetas.forEach(System.out::println);



        String[] tableTypes = {"TABLE", "VIEW"};
        try (ResultSet tablesResult = connection.getMetaData()
                .getTables("hive", null, null, tableTypes)
        ) {
            while (tablesResult.next()) {
                String tableName = tablesResult.getString("TABLE_NAME");
                String tableType = tablesResult.getString("TABLE_TYPE");
                String tableComment = tablesResult.getString("REMARKS");
                System.out.println(tableName);
            }
        }
    }
}
