package com.github.starhq.template.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.starhq.template.BaseMapperTest;
import com.github.starhq.template.entity.SysUser;
import com.github.starhq.template.enums.UserStatus;
import com.github.starhq.template.vo.UserVO;

class UserMapperTest extends BaseMapperTest {

    @Autowired
    private SysUserMapper userMapper;

    @Test
    void insertUser_shouldInsertSuccessfully() {
        SysUser user = prepare(99L, "testuser");

        int result = userMapper.insert(user);

        assertThat(result).isEqualTo(1);
        assertThat(user.getId()).isNotNull().isGreaterThan(0L);

        SysUser dbUser = userMapper.selectById(user.getId());
        assertThat(dbUser).isNotNull();
        assertThat(dbUser.getUsername()).isEqualTo("testuser");
    }

    @Test
    void updateUser_shouldUpdateSuccessfully() {
        SysUser user = prepare(100L, "before");
        userMapper.insert(user);

        user.setUsername("after");
        user.setStatus(UserStatus.BANNED);

        int result = userMapper.updateById(user);

        assertThat(result).isEqualTo(1);

        SysUser dbUser = userMapper.selectById(user.getId());
        assertThat(dbUser.getUsername()).isEqualTo("after");
        assertThat(dbUser.getStatus()).isEqualTo(UserStatus.BANNED);
    }

    @Test
    void selectUserPage_shouldReturnPagedResult() {
        SysUser user = prepare(101L, "cloud");
        userMapper.insert(user);

        Page<UserVO> page = new Page<>(1, 10);
        QueryWrapper<UserVO> wrapper = new QueryWrapper<>();
        wrapper.orderBy(true, false, "id");

        IPage<UserVO> result = userMapper.selectUserPage("cloud", page, wrapper);

        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isGreaterThan(0);
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getUsername()).isEqualTo("cloud");
    }

    @Test
    void findById_shouldReturnUser() {
        SysUser user = prepare(102L, "lookup");
        userMapper.insert(user);

        SysUser result = userMapper.selectById(user.getId());

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(user.getId());
    }

    @Test
    void findByUsername_shouldReturnUser() {
        SysUser user = prepare(103L, "uniqueUser");
        userMapper.insert(user);

        QueryWrapper<SysUser> wrapper = new QueryWrapper<>();
        wrapper.eq("username", "uniqueUser");

        SysUser result = userMapper.selectOne(wrapper);

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("uniqueUser");
    }

    @Test
    void deleteUser_shouldDeleteSuccessfully() {
        SysUser user = prepare(104L, "todelete");
        userMapper.insert(user);

        int result = userMapper.deleteById(user.getId());

        assertThat(result).isEqualTo(1);

        SysUser dbUser = userMapper.selectById(user.getId());
        assertThat(dbUser).isNull();
    }

    private SysUser prepare(Long id, String usename) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername(usename);
        user.setPassword("123456");
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(OffsetDateTime.now());
        return user;
    }
}
