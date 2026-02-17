package com.github.starhq.template.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.apache.ibatis.executor.BatchResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.starhq.template.BaseMapperTest;
import com.github.starhq.template.entity.SysRoleMenu;

class SysRoleMenuMapperTest extends BaseMapperTest {

    @Autowired
    private SysRoleMenuMapper roleMenuMapper;

    @Test
    void insertRoleMenu_shouldInsertSuccessfully() {
        List<SysRoleMenu> roleMenus = List.of(new SysRoleMenu(2L, 1L));

        List<BatchResult> inserts = roleMenuMapper.insert(roleMenus);

        assertThat(inserts.size()).isEqualTo(1);
    }

    @Test
    void deleteRoleMenu_By_Role_ID_shouldDeleteSuccessfully() {
        QueryWrapper<SysRoleMenu> query = new QueryWrapper<>();
        query.eq("role_id", 1L);

        int delete = roleMenuMapper.delete(query);
        assertThat(delete).isGreaterThanOrEqualTo(0);

        List<SysRoleMenu> selectList = roleMenuMapper.selectList(query);
        assertThat(selectList).isEmpty();
    }

    @Test
    void deleteRoleMenu_By_Menu_ID_shouldDeleteSuccessfully() {
        QueryWrapper<SysRoleMenu> query = new QueryWrapper<>();
        query.eq("menu_id", 1L);

        int delete = roleMenuMapper.delete(query);
        assertThat(delete).isGreaterThanOrEqualTo(0);

        List<SysRoleMenu> selectList = roleMenuMapper.selectList(query);
        assertThat(selectList).isEmpty();
    }
}