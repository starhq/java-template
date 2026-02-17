package com.github.starhq.template.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.starhq.template.dto.LoginDTO;
import com.github.starhq.template.dto.PageRequest;
import com.github.starhq.template.dto.UserCreateDTO;
import com.github.starhq.template.dto.UserUpdateDTO;
import com.github.starhq.template.vo.LoginVO;
import com.github.starhq.template.vo.UserVO;

/**
 * 用户服务接口
 *
 * @author starhq
 */
public interface UserService {
    /**
     * 用户登录
     *
     * @param loginDTO 登录信息
     * @return 登录结果
     */
    LoginVO login(LoginDTO loginDTO);

    /**
     * 创建用户
     *
     * @param dto 用户创建DTO
     * @return 用户VO
     */
    UserVO createUser(UserCreateDTO dto);

    /**
     * 更新用户
     *
     * @param id  用户ID
     * @param dto 用户更新DTO
     * @return 用户VO
     */
    UserVO updateUser(Long id, UserUpdateDTO dto);

    /**
     * 根据ID查询用户
     *
     * @param id 用户ID
     * @return 用户VO
     */
    UserVO getUserById(Long id);

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户VO
     */
    UserVO getUserByUsername(String username);

    /**
     * 分页查询用户
     *
     * @param pageRequest 分页请求
     * @return 分页结果
     */
    Page<UserVO> listUsers(PageRequest pageRequest);

    /**
     * 删除用户
     *
     * @param id 用户ID
     */
    void deleteUser(Long id);
}
