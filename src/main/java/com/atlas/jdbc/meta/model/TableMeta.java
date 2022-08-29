package com.atlas.jdbc.meta.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TableMeta {
    private String dbName;       // 数据库名

    private String tableName;    // 表名

    private String tableType;    // 表类型 view or Table

    private String tableComment; // 注释

    private String createTime;   // 创建时间

    private String updateTime;   // 更新时间

}
