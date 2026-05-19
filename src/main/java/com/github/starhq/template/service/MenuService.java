package com.github.starhq.template.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.github.starhq.template.entity.SysMenu;
import com.github.starhq.template.model.dto.menu.MenuDTO;
import com.github.starhq.template.model.dto.page.PageRequest;
import com.github.starhq.template.model.vo.menu.MenuSimpleVO;
import com.github.starhq.template.model.vo.menu.tree.LeftNavVO;
import com.github.starhq.template.model.vo.menu.tree.MenuCheckVO;
import com.github.starhq.template.model.vo.menu.tree.MenuListVO;

import java.io.Serializable;
import java.util.List;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description:
 * @date 2026/4/4 22:10
 */
public interface MenuService extends IService<SysMenu> {

    /**
     * Retrieves a list of menus.
     *
     * @return menu response
     */
    List<MenuListVO> selectList(PageRequest pageRequest);

    /**
     * Retrieves a list of menus for navigation.
     *
     * @return list of navigation menu responses
     */
    List<LeftNavVO> selectSidebar(Serializable userId);


    /**
     * Retrieves a menu by its ID.
     *
     * @param id the menu ID
     * @return menu details
     */
    MenuSimpleVO getMenuById(Serializable id);

    /**
     * Retrieves a paginated list of parent's menus with checked status for a specific role.
     *
     * @param roleId the role ID
     * @return paginated response of checked menu
     */
    List<MenuCheckVO> selectCheckedMenus(Serializable roleId);
    /**
     * Updates an existing menu.
     *
     * @param menuDto menu update information
     * @return true if update successful
     */
    boolean updateMenu(Serializable id, MenuDTO menuDto);

    /**
     * Creates a new menu.
     *
     * @param menuDto menu creation information
     * @return true if creation successful
     */
    boolean createMenu(MenuDTO menuDto);
}
