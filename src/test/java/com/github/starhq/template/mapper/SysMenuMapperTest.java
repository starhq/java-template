package com.github.starhq.template.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.starhq.template.BaseMapperTestConfiguration;
import com.github.starhq.template.common.enums.OpenStyle;
import com.github.starhq.template.entity.SysMenu;
import com.github.starhq.template.model.vo.menu.tree.MenuCheckVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SysMenuMapperTest extends BaseMapperTestConfiguration {

    @Autowired
    private SysMenuMapper menuMapper;

    @Test
    void insertMenu_shouldInsertSuccessfully() {
        SysMenu menu = prepare(101L, "Home Menu", "/home");

        int result = menuMapper.insert(menu);

        assertThat(result).isEqualTo(1);
        assertThat(menu.getId()).isNotNull().isGreaterThan(0L);

        SysMenu dbMenu = menuMapper.selectById(menu.getId());
        assertThat(dbMenu).isNotNull();
        assertThat(dbMenu.getName()).isEqualTo("Home Menu");
        assertThat(dbMenu.getUrl()).isEqualTo("/home");
    }

    @Test
    void updateMenu_shouldUpdateSuccessfully() {
        SysMenu menu = prepare(102L, "Profile Menu", "/profile");
        menuMapper.insert(menu);

        menu.setName("Updated Profile");
        menu.setUrl("/updated-profile");
        menu.setOpenStyle(OpenStyle.EXTERNAL);
        menu.setCreatedAt(OffsetDateTime.now());
        menu.setCreatedBy(1L);

        int result = menuMapper.updateById(menu);

        assertThat(result).isEqualTo(1);

        SysMenu dbMenu = menuMapper.selectById(menu.getId());
        assertThat(dbMenu.getName()).isEqualTo("Updated Profile");
        assertThat(dbMenu.getUrl()).isEqualTo("/updated-profile");
        assertThat(dbMenu.getOpenStyle()).isEqualTo(OpenStyle.EXTERNAL);
    }

    @Test
    void selectAssignedParentMenus_shouldReturnResult() {
        QueryWrapper<SysMenu> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", 1L);

        var menus = menuMapper.selectAssignedMenus(wrapper);

        assertThat(menus).isNotNull()
                .hasSizeGreaterThan(0);
    }

    @Test
    void selectMenuPageByRoleId_shouldReturnCheckedResult() {
        // Given
        Page<MenuCheckVO> page = new Page<>();
        page.setSearchCount(false);


        // When
        QueryWrapper<MenuCheckVO> wrapper = new QueryWrapper<>();
        wrapper.isNull("parent_id");
        wrapper.orderBy(true, false, "sort_order");

        List<MenuCheckVO> result = menuMapper.selectMenusByRoleId(1L);

        assertThat(result).isNotEmpty();
    }

    @Test
    void findById_shouldReturnMenu() {
        SysMenu menu = prepare(103L, "Settings Menu", "/settings");
        menuMapper.insert(menu);

        SysMenu result = menuMapper.selectById(menu.getId());

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(menu.getId());
        assertThat(result.getName()).isEqualTo("Settings Menu");
    }

    @Test
    void selectUserIdsByMenuId_shouldReturnUserIds() {
        Long menuId = 1L;

        List<Long> userIds = menuMapper.selectUserIdsByMenuId(menuId);
        assertThat(userIds).isNotNull().isNotEmpty();
    }

    @Test
    void deleteMenu_shouldDeleteSuccessfully() {
        SysMenu menu = prepare(105L, "Temp Menu", "/temp");
        menuMapper.insert(menu);

        int result = menuMapper.deleteById(menu.getId());

        assertThat(result).isEqualTo(1);

        SysMenu dbMenu = menuMapper.selectById(menu.getId());
        assertThat(dbMenu).isNull();
    }

    private SysMenu prepare(Long id, String name, String url) {
        SysMenu menu = new SysMenu();
        menu.setId(id);
        // menu.setParentId(null); // Root menu
        menu.setName(name);
        menu.setUrl(url);
        menu.setIcon("icon-home");
        menu.setSortOrder(1);
        menu.setOpenStyle(OpenStyle.INTERNAL);
        menu.setCreatedAt(OffsetDateTime.now());
        menu.setCreatedBy(1L);
        return menu;
    }
}