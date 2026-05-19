package com.github.starhq.template.mapper;

import java.io.Serializable;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.starhq.template.entity.SysButton;
import com.github.starhq.template.model.vo.button.ButtonCheckVO;

/**
 * 系统按钮 Mapper
 *
 * @author starhq
 */
@Mapper
public interface SysButtonMapper extends BaseMapper<SysButton> {

    List<ButtonCheckVO> selectButtonsByRoleId(@Param("roleId") Serializable roleId);

    List<SysButton> selectAssignedButtonsByUserId(@Param("userId") Serializable userId);

    List<Long> selectUserIdsByButtonId(@Param("buttonId") Serializable buttonId);
}
