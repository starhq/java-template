package com.github.starhq.template.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.github.starhq.template.BaseMapperTest;
import com.github.starhq.template.entity.SysMenu;
import com.github.starhq.template.enums.OpenStyle;
import com.github.starhq.template.vo.MenuVO;

class SysMenuMapperTest extends BaseMapperTest {

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
    void selectMenuList_shouldReturnResult() {
        List<MenuVO> result = menuMapper.selectMenus();

        assertThat(result).isNotNull();
        assertThat(result.size()).isGreaterThan(0);
    }

    @Test
    void selectMenusByUserId_shouldReturnResult() {
        List<MenuVO> result = menuMapper.selectMenusByRoleId(1L);

        assertThat(result).isNotNull();
        assertThat(result.size()).isGreaterThan(0);
    }

    @Test
    void selectAssignedRolesByUserIds_shouldReturnResult() {
        List<MenuVO> result = menuMapper.selectAssignedMenusByRoleIds(List.of(1L));

        assertThat(result).isNotNull();
        assertThat(result.size()).isGreaterThan(0);
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