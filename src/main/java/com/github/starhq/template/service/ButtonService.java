package com.github.starhq.template.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.github.starhq.template.entity.SysButton;
import com.github.starhq.template.model.dto.button.ButtonDTO;
import com.github.starhq.template.model.dto.button.ButtonPageRequest;
import com.github.starhq.template.model.vo.button.ButtonCheckVO;
import com.github.starhq.template.model.vo.button.ButtonPageVO;
import com.github.starhq.template.model.vo.button.ButtonSimpleVO;

import java.io.Serializable;
import java.util.List;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: button service
 * @date 2026/4/3 12:41
 */
public interface ButtonService extends IService<SysButton> {

    /**
     * Retrieves a paginated list of buttons.
     *
     * @param pageInfo pagination and sorting information
     * @return paginated list of buttons
     */
    IPage<ButtonPageVO> page(ButtonPageRequest pageInfo);

    /**
     * Get all buttons assigned to current user's roles.
     *
     * @return list of buttons
     */
    List<String> select(Serializable userId);

    /**
     * Get button by ID.
     *
     * @param id button ID
     * @return button details
     */
    ButtonSimpleVO getButtonById(Serializable id);

    /**
     * Get checked buttons for a role.
     *
     * @param roleId role ID
     * @return list of checked buttons
     */
    List<ButtonCheckVO> selectCheckedButtons(Serializable roleId);

    /**
     * Create a new button.
     *
     * @param buttonDto button creation data
     * @return true if successful
     */
    boolean createButton(ButtonDTO buttonDto);

    /**
     * Update an existing button.
     *
     * @param buttonDto button update data
     * @return true if successful
     */
    boolean updateButton(Serializable id, ButtonDTO buttonDto);
}
