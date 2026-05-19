package com.github.starhq.template.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class BaseAuditVO extends BaseIdVO {

    protected static final String UNKNOWN_USER = "unknown";

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private OffsetDateTime createdAt;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private OffsetDateTime updatedAt;

    /**
     * 创建者
     */
    private String creator;

    /**
     * 更新者
     */
    private String updater;

    /**
     * ✅ 提取出的动态填充逻辑
     */
    public void populateAuditFields(Long createdBy, Long updatedBy, Map<Long, String> nameMap) {
        this.setCreator(nameMap.getOrDefault(createdBy, UNKNOWN_USER));
        this.setUpdater(nameMap.getOrDefault(updatedBy, UNKNOWN_USER));
    }
}
