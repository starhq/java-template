package com.github.starhq.template.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.starhq.template.entity.SysAuditLog;
import com.github.starhq.template.model.vo.auditlog.AuditLogPageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 系统审计日志 Mapper
 *
 * @author starhq
 */
@Mapper
public interface SysAuditLogMapper extends BaseMapper<SysAuditLog> {

    IPage<AuditLogPageVO> selectAuditLogPage(@Param("page") Page<AuditLogPageVO> page, @Param(Constants.WRAPPER) Wrapper<AuditLogPageVO> wrapper);
}
