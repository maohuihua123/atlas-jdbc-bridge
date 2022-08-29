package com.atlas.jdbc.hook.utlis;

import com.atlas.jdbc.config.DatasourceConfig;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Objects;
import java.util.Properties;

public class JDBCUtils {

    private JDBCUtils() {
    }

    public static String loadTypeDefsJsonFile() {
        String fileName = "jdbc-model.json";
        StringBuilder sb = new StringBuilder();
        try (
            InputStream in = JDBCUtils.class.getClassLoader().getResourceAsStream(fileName);
            InputStreamReader inputStreamReader = new InputStreamReader(Objects.requireNonNull(in));
            BufferedReader br = new BufferedReader(inputStreamReader)
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

    public static DatasourceConfig lodaConfig(String path, String fileName) throws IOException {

        File file = new File(path, fileName);
        InputStream inputStream = new FileInputStream(file);
        Properties properties = new Properties();
        properties.load(inputStream);

        String cluster = properties.getProperty("jdbc.cluster");
        String url = properties.getProperty("jdbc.url");
        String driver = properties.getProperty("jdbc.driver");
        String username = properties.getProperty("jdbc.username");
        String password = properties.getProperty("jdbc.password");

        return DatasourceConfig.builder()
                .cluster(cluster)
                .url(url).driver(driver)
                .username(username).password(password)
                .build();
    }

    public static Connection getConnection(DatasourceConfig config) {
        Connection conn = null;
        try {
            Class.forName(config.getDriver());
            conn = DriverManager.getConnection(config.getUrl(), config.getUsername(), config.getPassword());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return conn;
    }
}
