package com.atlas.jdbc.meta.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForeignKeyMeta {
    private String dbName;        // 数据库名称

    private String pkTableName;   // 外键所在的表名称

    private String pkColumnName;  // 外键所在表的列名

    private String fkName;        // 外键名称

    private String fkTableName;   // 外键所关联的表的名称(被引用的父表)

    private String fkColumnName;  // 外键所关联表的列名(被引用的字段)

    private String updateRule;    // 更新规则

    private String deleteRule;    // 删除规则
}
