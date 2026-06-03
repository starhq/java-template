package com.github.starhq.template.controller.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.starhq.template.common.constant.TestConstant;
import com.github.starhq.template.config.i18n.MessageUtils;
import com.github.starhq.template.controller.RoleController;
import com.github.starhq.template.model.dto.PageRequest;
import com.github.starhq.template.model.dto.RoleDTO;
import com.github.starhq.template.model.vo.ButtonCheckVO;
import com.github.starhq.template.model.vo.MenuCheckVO;
import com.github.starhq.template.model.vo.ResourceCheckVO;
import com.github.starhq.template.model.vo.RolePageVO;
import com.github.starhq.template.model.vo.RoleSimpleVO;
import com.github.starhq.template.service.ButtonService;
import com.github.starhq.template.service.MenuService;
import com.github.starhq.template.service.ResourceService;
import com.github.starhq.template.service.RoleService;
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
 * @date 2026/5/15 13:48
 */
@Import(TestConfig.class)
@WebMvcTest(RoleController.class)
@AutoConfigureMockMvc(addFilters = false)
class RoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private RoleService roleService;

    @MockitoBean
    private ButtonService buttonService;

    @MockitoBean
    private ResourceService resourceService;

    @MockitoBean
    private MenuService menuService;

    @MockitoBean
    private MessageUtils messageUtils;

    private static final Long TEST_ROLE_ID = 1L;
    private static final String TEST_ROLE_NAME = "test-role";
    private static final String TEST_ROLE_CODE = "TEST_ROLE";

    private IPage<RolePageVO> pageResult;
    private RoleSimpleVO roleResponse;
    private RoleDTO request;
    private List<ButtonCheckVO> checkedButtons;
    private List<ResourceCheckVO> checkedResources;
    private List<MenuCheckVO> checkedMenus;

    @BeforeEach
    void setUp() {
        // Initialize role response
        RolePageVO response = new RolePageVO();
        response.setId(TEST_ROLE_ID);
        response.setName(TEST_ROLE_NAME);
        response.setCode(TEST_ROLE_CODE);

        // Initialize page result
        pageResult = new Page<RolePageVO>(1, 10)
                .setRecords(Collections.singletonList(response))
                .setTotal(1);

        // Initialize simple role response
        roleResponse = new RoleSimpleVO();
        roleResponse.setId(TEST_ROLE_ID);
        roleResponse.setName(TEST_ROLE_NAME);
        roleResponse.setCode(TEST_ROLE_CODE);

        // Initialize create request
        request = new RoleDTO();
        request.setName(TEST_ROLE_NAME);
        request.setCode(TEST_ROLE_CODE);


        // Initialize checked responses
        checkedButtons = Collections.singletonList(new ButtonCheckVO());
        checkedResources = Collections.singletonList(new ResourceCheckVO());
        checkedMenus = Collections.singletonList(new MenuCheckVO());
    }

    @Test
    void create_Success() throws Exception {
        when(roleService.createRole(request)).thenReturn(true);

        mockMvc.perform(post(TestConstant.VERSION + "/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void update_Success() throws Exception {
        when(roleService.updateRole(TEST_ROLE_ID, request)).thenReturn(true);

        mockMvc.perform(put(TestConstant.VERSION + "/roles/" + TEST_ROLE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void delete_Success() throws Exception {
        when(roleService.removeById(TEST_ROLE_ID)).thenReturn(true);

        mockMvc.perform(delete(TestConstant.VERSION + "/roles/" + TEST_ROLE_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void queryRoles_Success() throws Exception {
        when(roleService.page(any(PageRequest.class))).thenReturn(pageResult);

        mockMvc.perform(get(TestConstant.VERSION + "/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("page", "1")
                        .param("size", "10")
                        .param("sort", "id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.data[0].id").value(TEST_ROLE_ID))
                .andExpect(jsonPath("$.data[0].name").value(TEST_ROLE_NAME))
                .andExpect(jsonPath("$.data[0].code").value(TEST_ROLE_CODE));
    }

    @Test
    void queryRole_Success() throws Exception {
        when(roleService.getRoleById(TEST_ROLE_ID)).thenReturn(roleResponse);

        mockMvc.perform(get(TestConstant.VERSION + "/roles/" + TEST_ROLE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(TEST_ROLE_ID))
                .andExpect(jsonPath("$.data.name").value(TEST_ROLE_NAME))
                .andExpect(jsonPath("$.data.code").value(TEST_ROLE_CODE));
    }

    @Test
    void queryRoleButtons_Success() throws Exception {
        when(buttonService.selectCheckedButtons(TEST_ROLE_ID)).thenReturn(checkedButtons);

        mockMvc.perform(get(TestConstant.VERSION + "/roles/buttons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("roleId", String.valueOf(TEST_ROLE_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void queryRoleResources_Success() throws Exception {
        when(resourceService.selectCheckedResources(TEST_ROLE_ID)).thenReturn(checkedResources);

        mockMvc.perform(get(TestConstant.VERSION + "/roles/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("roleId", String.valueOf(TEST_ROLE_ID))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void queryRoleMenus_Success() throws Exception {
        when(menuService.selectCheckedMenus(TEST_ROLE_ID)).thenReturn(checkedMenus);

        mockMvc.perform(get(TestConstant.VERSION + "/roles/menus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("roleId", String.valueOf(TEST_ROLE_ID))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }
}
