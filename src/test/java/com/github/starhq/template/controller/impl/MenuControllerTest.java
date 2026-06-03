package com.github.starhq.template.controller.impl;

import com.github.starhq.template.common.constant.TestConstant;
import com.github.starhq.template.common.enums.OpenStyle;
import com.github.starhq.template.config.i18n.MessageUtils;
import com.github.starhq.template.controller.MenuController;
import com.github.starhq.template.model.dto.MenuDTO;
import com.github.starhq.template.model.dto.PageRequest;
import com.github.starhq.template.model.vo.MenuSimpleVO;
import com.github.starhq.template.model.vo.MenuListVO;
import com.github.starhq.template.service.MenuService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description:
 * @date 2026/5/15 12:51
 */
@Import(TestConfig.class)
@WebMvcTest(MenuController.class)
@AutoConfigureMockMvc(addFilters = false)
class MenuControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private MenuService menuService;
    @MockitoBean
    private MessageUtils messageUtils;

    private static final Long TEST_MENU_ID = 1L;
    private static final String TEST_MENU_NAME = "test-menu";
    private static final String TEST_MENU_URL = "/test";

    private MenuDTO request;
    private MenuSimpleVO menuResponse;
    private List<MenuListVO> menuListResponses;

    @BeforeEach
    void setUp() {
        // Initialize create request
        request = new MenuDTO();
        request.setName(TEST_MENU_NAME);
        request.setUrl(TEST_MENU_URL);
        request.setIcon(TEST_MENU_NAME);
        request.setSortOrder(0);
        request.setOpenStyle(OpenStyle.INTERNAL);


        // Initialize menu response
        menuResponse = new MenuSimpleVO();
        menuResponse.setId(TEST_MENU_ID);
        menuResponse.setName(TEST_MENU_NAME);
        menuResponse.setUrl(TEST_MENU_URL);

        // Initialize menu list response
        MenuListVO menuListResponse = new MenuListVO();
        menuListResponse.setId(TEST_MENU_ID);
        menuListResponse.setName(TEST_MENU_NAME);
        menuListResponse.setUrl(TEST_MENU_URL);
        menuListResponses = Collections.singletonList(menuListResponse);

    }

    @Test
    void create_Success() throws Exception {
        when(menuService.createMenu(request)).thenReturn(true);

        mockMvc.perform(post(TestConstant.VERSION + "/menus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void create_OpenStyle_Fallback() throws Exception {
        when(menuService.createMenu(request)).thenReturn(true);

        mockMvc.perform(post(TestConstant.VERSION + "/menus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"icon\":\"test-menu\",\"name\":\"test-menu\",\"openStyle\":3,\"parentId\":null,\"sortOrder\":0,\"url\":\"/test\"}\n"))
                .andExpect(status().isCreated());
    }

    @Test
    void update_Success() throws Exception {
        when(menuService.updateMenu(TEST_MENU_ID, request)).thenReturn(true);

        mockMvc.perform(put(TestConstant.VERSION + "/menus/" + TEST_MENU_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void delete_Success() throws Exception {
        when(menuService.removeById(TEST_MENU_ID)).thenReturn(true);

        mockMvc.perform(delete(TestConstant.VERSION + "/menus/" + TEST_MENU_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void queryMenus_Success() throws Exception {
        when(menuService.selectList(any(PageRequest.class))).thenReturn(menuListResponses);

        mockMvc.perform(get(TestConstant.VERSION + "/menus"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(TEST_MENU_ID))
                .andExpect(jsonPath("$.data[0].name").value(TEST_MENU_NAME))
                .andExpect(jsonPath("$.data[0].url").value(TEST_MENU_URL));
    }

    @Test
    void queryMenuById_Success() throws Exception {
        when(menuService.getMenuById(TEST_MENU_ID)).thenReturn(menuResponse);

        mockMvc.perform(get(TestConstant.VERSION + "/menus/" + TEST_MENU_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(TEST_MENU_ID))
                .andExpect(jsonPath("$.data.name").value(TEST_MENU_NAME))
                .andExpect(jsonPath("$.data.url").value(TEST_MENU_URL));
    }

}
