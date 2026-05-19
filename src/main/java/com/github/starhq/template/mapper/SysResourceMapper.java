package com.github.starhq.template.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.starhq.template.entity.SysResource;
import com.github.starhq.template.model.vo.resource.ResourceCheckVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.io.Serializable;
import java.util.List;

/**
 * 系统资源 Mapper
 *
 * @author starhq
 */
@Mapper
public interface SysResourceMapper extends BaseMapper<SysResource> {

    List<ResourceCheckVO> selectResourcesByRoleId(@Param("roleId") Serializable roleId);

    List<SysResource> selectAssignedResourceByUserId(@Param("userId") Serializable userId);

    List<Long> selectUserIdsByResourceId(@Param("resourceId") Serializable resourceId);
}
