package com.github.starhq.template.service;

import java.io.Serializable;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.github.starhq.template.entity.SysUser;
import com.github.starhq.template.model.dto.page.KeyWordPageRequest;
import com.github.starhq.template.model.dto.user.UserDTO;
import com.github.starhq.template.model.vo.user.UserPageVO;
import com.github.starhq.template.model.vo.user.UserSimpleVO;

/**
 * 用户服务接口
 *
 * @author starhq
 */
public interface UserService extends IService<SysUser> {

    /**
     * Retrieves a paginated list of users based on the provided user page DTO.
     *
     * @param pageRequest the DTO containing pagination and filtering information
     * @return a paginated response of users
     */
    IPage<UserPageVO> page(KeyWordPageRequest pageRequest);

    /**
     * 根据ID查询用户
     *
     * @param id 用户ID
     * @return 用户VO
     */
    UserSimpleVO getUserById(Serializable id);

    /**
     * 更新用户
     *
     * @param id  用户ID
     * @param dto 用户更新DTO
     * @return 是否成功
     */
    boolean updateUser(Serializable id, UserDTO dto);

    boolean createUser(UserDTO userDTO);
}
