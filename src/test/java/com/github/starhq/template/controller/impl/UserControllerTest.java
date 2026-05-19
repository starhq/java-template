package com.github.starhq.template.controller.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.starhq.template.common.constant.TestConstant;
import com.github.starhq.template.common.enums.UserStatus;
import com.github.starhq.template.common.util.SecurityContextUtils;
import com.github.starhq.template.config.messages.MessageUtils;
import com.github.starhq.template.controller.UserController;
import com.github.starhq.template.model.dto.page.KeyWordPageRequest;
import com.github.starhq.template.model.dto.user.UserDTO;
import com.github.starhq.template.model.vo.menu.tree.LeftNavVO;
import com.github.starhq.template.model.vo.role.RoleCheckVO;
import com.github.starhq.template.model.vo.user.UserPageVO;
import com.github.starhq.template.model.vo.user.UserSimpleVO;
import com.github.starhq.template.service.ButtonService;
import com.github.starhq.template.service.MenuService;
import com.github.starhq.template.service.RoleService;
import com.github.starhq.template.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
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

import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description:
 * @date 2026/5/15 14:26
 */
@Import(TestConfig.class)
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private ButtonService buttonService;

    @MockitoBean
    private MenuService menuService;

    @MockitoBean
    private RoleService roleService;

    @MockitoBean
    private MessageUtils messageUtils;

    private static final Long TEST_USER_ID = 1L;
    private static final String TEST_USER_NAME = "test-user";

    private KeyWordPageRequest pageRequest;
    private IPage<UserPageVO> pageResult;
    private UserSimpleVO userResponse;
    private UserDTO request;
    private List<LeftNavVO> menuResponses;

    @BeforeEach
    void setUp() {
        // Initialize page request
        pageRequest = new KeyWordPageRequest();
        pageRequest.setPage(1L);
        pageRequest.setSize(10L);

        // Initialize user response
        UserPageVO response = new UserPageVO();
        response.setId(TEST_USER_ID);
        response.setUsername(TEST_USER_NAME);

        // Initialize page result
        pageResult = new Page<UserPageVO>(1, 10)
                .setRecords(Collections.singletonList(response))
                .setTotal(1);

        // Initialize simple user response
        userResponse = new UserSimpleVO();
        userResponse.setId(TEST_USER_ID);
        userResponse.setUsername(TEST_USER_NAME);

        // Initialize update request
        request = new UserDTO();
        request.setUsername(TEST_USER_NAME);
        request.setPassword("Wj@64066195");
        request.setStatus(UserStatus.ACTIVE);

        LeftNavVO menuResponse = new LeftNavVO();
        menuResponse.setId(1L);
        menuResponse.setName("Test Menu");
        menuResponses = Collections.singletonList(menuResponse);
    }

    @Test
    void create_Success() throws Exception {
        when(userService.createUser(request)).thenReturn(true);

        mockMvc.perform(post(TestConstant.VERSION + "/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void create_NoStatus() throws Exception {
        when(userService.createUser(request)).thenReturn(true);

        mockMvc.perform(post(TestConstant.VERSION + "/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"Wj@64066195\",\"roleIds\":null,\"status\":\"\",\"username\":\"test-user\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_InvalidStatus() throws Exception {
        when(userService.createUser(request)).thenReturn(true);

        mockMvc.perform(post(TestConstant.VERSION + "/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"Wj@64066195\",\"roleIds\":null,\"status\":\"invalid\",\"username\":\"test-user\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_withNoPassword_badRequest() throws Exception {
        when(userService.createUser(request)).thenReturn(true);

        request.setPassword("");
        mockMvc.perform(post(TestConstant.VERSION + "/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_weakPassword() throws Exception {
        when(userService.createUser(request)).thenReturn(true);

        request.setPassword("123456");
        mockMvc.perform(post(TestConstant.VERSION + "/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_Success() throws Exception {
        when(userService.updateUser(TEST_USER_ID, request)).thenReturn(true);

        mockMvc.perform(put(TestConstant.VERSION + "/users/" + TEST_USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void delete_Success() throws Exception {
        when(userService.removeById(TEST_USER_ID)).thenReturn(true);

        mockMvc.perform(delete(TestConstant.VERSION + "/users/" + TEST_USER_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void queryUsers_Success() throws Exception {
        when(userService.page(pageRequest)).thenReturn(pageResult);

        mockMvc.perform(get(TestConstant.VERSION + "/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("page", "1")
                        .param("size", "10")
                        .param("sort", "id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.data[0].id").value(TEST_USER_ID))
                .andExpect(jsonPath("$.data[0].username").value(TEST_USER_NAME));
    }

    @Test
    void queryUser_Success() throws Exception {
        when(userService.getUserById(TEST_USER_ID)).thenReturn(userResponse);

        mockMvc.perform(get(TestConstant.VERSION + "/users/" + TEST_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(TEST_USER_ID))
                .andExpect(jsonPath("$.data.username").value(TEST_USER_NAME));
    }

    @Test
    void queryUserRoles_Success() throws Exception {
        RoleCheckVO checkedRole = new RoleCheckVO();
        checkedRole.setId(1L);
        checkedRole.setChecked(true);

        when(roleService.selectCheckedRoles(TEST_USER_ID)).thenReturn(List.of(checkedRole));

        mockMvc.perform(get(TestConstant.VERSION + "/users/roles")
                        .param("userId", String.valueOf(TEST_USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(TEST_USER_ID))
                .andExpect(jsonPath("$.data[0].checked").value(true));
    }

    @Test
    void queryUserProfile_Success() throws Exception {
        try (MockedStatic<SecurityContextUtils> mockedStatic = mockStatic(SecurityContextUtils.class)) {
            mockedStatic.when(SecurityContextUtils::getRequiredUserId).thenReturn(TEST_USER_ID);
            when(userService.getUserById(TEST_USER_ID)).thenReturn(userResponse);
            mockMvc.perform(get(TestConstant.VERSION + "/users/profile"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(TEST_USER_ID))
                    .andExpect(jsonPath("$.data.username").value(TEST_USER_NAME));
        }
    }

    @Test
    void queryUserButton_Success() throws Exception {
        try (MockedStatic<SecurityContextUtils> mockedStatic = mockStatic(SecurityContextUtils.class)) {
            mockedStatic.when(SecurityContextUtils::getRequiredUserId).thenReturn(TEST_USER_ID);
            when(buttonService.select(TEST_USER_ID)).thenReturn(List.of("TEST_BUTTON"));

            mockMvc.perform(get(TestConstant.VERSION + "/users/buttons"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0]").value("TEST_BUTTON"));
        }
    }

    @Test
    void queryUserMenus_Success() throws Exception {
        try (MockedStatic<SecurityContextUtils> mockedStatic = mockStatic(SecurityContextUtils.class)) {
            mockedStatic.when(SecurityContextUtils::getRequiredUserId).thenReturn(TEST_USER_ID);

            when(menuService.selectSidebar(TEST_USER_ID)).thenReturn(menuResponses);

            mockMvc.perform(get(TestConstant.VERSION + "/users/menus"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].id").value(1))
                    .andExpect(jsonPath("$.data[0].name").value("Test Menu"));
        }
    }
}

