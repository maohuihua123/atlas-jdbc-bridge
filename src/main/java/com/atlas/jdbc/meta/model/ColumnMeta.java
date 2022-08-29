package com.atlas.jdbc.meta.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ColumnMeta {
    private String dbName;         // 数据库名称

    private String tableName;      // 数据表名

    private String columnName;     // 列名

    private String columnComment;  // 评论性的注释

    private String columnType;     // 数据类型(varchar)

    private String defaultValue;   // 默认值

    private String isNullable;     // 列的可空性(YES ; NO ; 空字符串(如果未知))

    private String autoIncrement;  // 是否自增(YES ; NO ; 空字符串)
}
