package com.github.starhq.template.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.starhq.template.entity.SysAuditLog;
import com.github.starhq.template.enums.TargetType;
import com.github.starhq.template.vo.AuditLogVO;

/**
 * 系统审计日志Mapper
 *
 * @author starhq
 */
@Mapper
public interface SysAuditLogMapper extends BaseMapper<SysAuditLog> {

    IPage<AuditLogVO> selectAuditLogPage(@Param("targetType") TargetType targetType, @Param("username") String username,
            @Param("page") Page<AuditLogVO> page,
            @Param("ew") Wrapper<AuditLogVO> wrapper);
}
