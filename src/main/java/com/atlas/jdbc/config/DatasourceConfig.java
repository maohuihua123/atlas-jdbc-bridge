package com.atlas.jdbc.config;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatasourceConfig {
    private String cluster;
    private String driver;
    private String url;
    private String username;
    private String password;
}
