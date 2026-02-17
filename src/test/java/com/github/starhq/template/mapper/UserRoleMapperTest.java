package com.github.starhq.template.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.apache.ibatis.executor.BatchResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.starhq.template.BaseMapperTest;
import com.github.starhq.template.entity.SysUserRole;

class UserRoleMapperTest extends BaseMapperTest {

    @Autowired
    private SysUserRoleMapper userRoleMapper;

    @Test
    void insertUserRole_shouldInsertSuccessfully() {
        List<SysUserRole> userRoles = List.of(new SysUserRole(2L, 1L));

        List<BatchResult> inserts = userRoleMapper.insert(userRoles);

        assertThat(inserts.size()).isEqualTo(1);
    }

    @Test
    void deleteUserRole_By_User_ID_shouldDeleteSuccessfully() {
        QueryWrapper<SysUserRole> query = new QueryWrapper<>();
        query.eq("user_id", 2L);

        int delete = userRoleMapper.delete(query);
        assertThat(delete).isEqualTo(1);

        List<SysUserRole> selectList = userRoleMapper.selectList(query);
        assertThat(selectList).isEmpty();
    }

    @Test
    void deleteUserRole_By_Role_ID_shouldDeleteSuccessfully() {
        QueryWrapper<SysUserRole> query = new QueryWrapper<>();
        query.eq("role_id", 2L);

        int delete = userRoleMapper.delete(query);
        assertThat(delete).isEqualTo(1);

        List<SysUserRole> selectList = userRoleMapper.selectList(query);
        assertThat(selectList).isEmpty();
    }

}
