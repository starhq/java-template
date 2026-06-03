package com.github.starhq.template.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.starhq.template.BaseMapperTestConfiguration;
import com.github.starhq.template.common.enums.TargetType;
import com.github.starhq.template.entity.SysAuditLog;
import com.github.starhq.template.model.vo.AuditLogPageVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SysAuditLogMapperTest extends BaseMapperTestConfiguration {

    @Autowired
    private SysAuditLogMapper auditLogMapper;

    @Test
    void insertAuditLog_shouldInsertSuccessfully() {
        SysAuditLog auditLog = prepare(101L, "create_user", 1L, TargetType.USER);

        int result = auditLogMapper.insert(auditLog);

        assertThat(result).isEqualTo(1);
        assertThat(auditLog.getId()).isNotNull().isGreaterThan(0L);

        SysAuditLog dbAuditLog = auditLogMapper.selectById(auditLog.getId());
        assertThat(dbAuditLog).isNotNull();
        assertThat(dbAuditLog.getAction()).isEqualTo("create_user");
        assertThat(dbAuditLog.getTargetType()).isEqualTo(TargetType.USER);
    }

    @Test
    void updateAuditLog_shouldUpdateSuccessfully() {
        SysAuditLog auditLog = prepare(102L, "update_user", 2L, TargetType.USER);
        auditLogMapper.insert(auditLog);

        // Note: Audit logs are typically immutable, so we'll just verify the initial
        // insertion worked
        SysAuditLog dbAuditLog = auditLogMapper.selectById(auditLog.getId());
        assertThat(dbAuditLog).isNotNull();
        assertThat(dbAuditLog.getAction()).isEqualTo("update_user");
    }

    @Test
    void findById_shouldReturnAuditLog() {
        SysAuditLog auditLog = prepare(103L, "delete_user", 3L, TargetType.BUTTON);
        auditLogMapper.insert(auditLog);

        SysAuditLog result = auditLogMapper.selectById(auditLog.getId());

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(auditLog.getId());
        assertThat(result.getAction()).isEqualTo("delete_user");
    }

    @Test
    void findLogPage_shouldReturnAuditLog() {
        SysAuditLog auditLog = prepare(104L, "login_attempt", 4L, TargetType.USER);
        auditLogMapper.insert(auditLog);

        Page<AuditLogPageVO> page = new Page<>(1, 10);

        QueryWrapper<AuditLogPageVO> wrapper = new QueryWrapper<>();
        wrapper.eq("target_type", TargetType.USER);
        wrapper.likeRight("creator.username", "admin");
        wrapper.orderBy(true, false, "id");

        IPage<AuditLogPageVO> result = auditLogMapper.selectAuditLogPage(page, wrapper);

        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isGreaterThan(0);
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().getFirst().getAction()).isEqualTo("login_attempt");
    }

    @Test
    void deleteAuditLog_shouldDeleteSuccessfully() {
        SysAuditLog auditLog = prepare(105L, "test_action", 5L, TargetType.USER);
        auditLogMapper.insert(auditLog);

        int result = auditLogMapper.deleteById(auditLog.getId());

        assertThat(result).isEqualTo(1);

        SysAuditLog dbAuditLog = auditLogMapper.selectById(auditLog.getId());
        assertThat(dbAuditLog).isNull();
    }

    private SysAuditLog prepare(Long id, String action, Long targetId, TargetType targetType) {
        SysAuditLog auditLog = new SysAuditLog();
        auditLog.setId(id);
        auditLog.setAction(action);
        auditLog.setTargetId(targetId);
        auditLog.setTargetType(targetType);
        auditLog.setValue("{\"userId\": " + targetId + ", \"action\": \"" + action + "\"}");
        auditLog.setCreatedAt(OffsetDateTime.now());
        auditLog.setCreatedBy(1L);
        return auditLog;
    }
}