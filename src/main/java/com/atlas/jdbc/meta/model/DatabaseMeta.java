package com.atlas.jdbc.meta.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseMeta {
    private String catalogName;

    private String schemaName;
}
