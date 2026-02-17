package com.github.starhq.template.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.starhq.template.dto.PageRequest;
import com.github.starhq.template.dto.UserCreateDTO;
import com.github.starhq.template.dto.UserUpdateDTO;
import com.github.starhq.template.service.UserService;
import com.github.starhq.template.vo.Result;
import com.github.starhq.template.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户控制器
 *
 * @author starhq
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    /**
     * 创建用户
     *
     * @param dto 用户创建DTO
     * @return 用户VO
     */
    @PostMapping
    public Result<UserVO> createUser(@Valid @RequestBody UserCreateDTO dto) {
        UserVO userVO = userService.createUser(dto);
        return Result.success(userVO);
    }

    /**
     * 更新用户
     *
     * @param id  用户ID
     * @param dto 用户更新DTO
     * @return 用户VO
     */
    @PutMapping("/{id}")
    public Result<UserVO> updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateDTO dto) {
        UserVO userVO = userService.updateUser(id, dto);
        return Result.success(userVO);
    }

    /**
     * 根据ID查询用户
     *
     * @param id 用户ID
     * @return 用户VO
     */
    @GetMapping("/{id}")
    public Result<UserVO> getUserById(@PathVariable Long id) {
        UserVO userVO = userService.getUserById(id);
        return Result.success(userVO);
    }

    /**
     * 分页查询用户
     *
     * @param pageRequest 分页请求
     * @return 分页结果
     */
    @GetMapping
    public Result<Page<UserVO>> listUsers(@Valid PageRequest pageRequest) {
        Page<UserVO> page = userService.listUsers(pageRequest);
        return Result.success(page);
    }

    /**
     * 删除用户
     *
     * @param id 用户ID
     * @return 结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success();
    }
}
