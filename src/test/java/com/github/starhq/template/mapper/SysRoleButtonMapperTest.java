package com.github.starhq.template.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.apache.ibatis.executor.BatchResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.starhq.template.BaseMapperTest;
import com.github.starhq.template.entity.SysRoleButton;

class SysRoleButtonMapperTest extends BaseMapperTest {

    @Autowired
    private SysRoleButtonMapper roleButtonMapper;

    @Test
    void insertRoleButton_shouldInsertSuccessfully() {
        List<SysRoleButton> roleButtons = List.of(new SysRoleButton(2L, 1L));

        List<BatchResult> inserts = roleButtonMapper.insert(roleButtons);

        assertThat(inserts.size()).isEqualTo(1);
    }

    @Test
    void deleteRoleButton_By_Role_ID_shouldDeleteSuccessfully() {
        QueryWrapper<SysRoleButton> query = new QueryWrapper<>();
        query.eq("role_id", 1L);

        int delete = roleButtonMapper.delete(query);
        assertThat(delete).isGreaterThanOrEqualTo(0);

        List<SysRoleButton> selectList = roleButtonMapper.selectList(query);
        assertThat(selectList).isEmpty();
    }

    @Test
    void deleteRoleButton_By_Button_ID_shouldDeleteSuccessfully() {
        QueryWrapper<SysRoleButton> query = new QueryWrapper<>();
        query.eq("button_id", 1L);

        int delete = roleButtonMapper.delete(query);
        assertThat(delete).isGreaterThanOrEqualTo(0);

        List<SysRoleButton> selectList = roleButtonMapper.selectList(query);
        assertThat(selectList).isEmpty();
    }
}