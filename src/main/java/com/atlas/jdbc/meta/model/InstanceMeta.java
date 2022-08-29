package com.atlas.jdbc.meta.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstanceMeta {
    private String url;                     // 数据库连接

    private String userName;                // 用户名称

    private String productName;             // 产品名称

    private String productVersion;          // 产品版本

    private String driverName;              // 驱动名称

    private String driverVersion;           // 驱动版本
}
