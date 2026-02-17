package com.github.starhq.template.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.apache.ibatis.executor.BatchResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.starhq.template.BaseMapperTest;
import com.github.starhq.template.entity.SysRoleResource;

class SysRoleResourceMapperTest extends BaseMapperTest {

    @Autowired
    private SysRoleResourceMapper roleResourceMapper;

    @Test
    void insertRoleResource_shouldInsertSuccessfully() {
        List<SysRoleResource> roleResources = List.of(new SysRoleResource(2L, 1L));

        List<BatchResult> inserts = roleResourceMapper.insert(roleResources);

        assertThat(inserts.size()).isEqualTo(1);
    }

    @Test
    void deleteRoleResource_By_Role_ID_shouldDeleteSuccessfully() {
        QueryWrapper<SysRoleResource> query = new QueryWrapper<>();
        query.eq("role_id", 1L);

        int delete = roleResourceMapper.delete(query);
        assertThat(delete).isGreaterThanOrEqualTo(0);

        List<SysRoleResource> selectList = roleResourceMapper.selectList(query);
        assertThat(selectList).isEmpty();
    }

    @Test
    void deleteRoleResource_By_Resource_ID_shouldDeleteSuccessfully() {
        QueryWrapper<SysRoleResource> query = new QueryWrapper<>();
        query.eq("resource_id", 1L);

        int delete = roleResourceMapper.delete(query);
        assertThat(delete).isGreaterThanOrEqualTo(0);

        List<SysRoleResource> selectList = roleResourceMapper.selectList(query);
        assertThat(selectList).isEmpty();
    }
}