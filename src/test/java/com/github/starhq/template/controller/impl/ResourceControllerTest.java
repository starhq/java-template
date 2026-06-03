package com.github.starhq.template.controller.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.starhq.template.common.constant.TestConstant;
import com.github.starhq.template.common.enums.HttpMethod;
import com.github.starhq.template.config.i18n.MessageUtils;
import com.github.starhq.template.controller.ResourceController;
import com.github.starhq.template.model.dto.PageRequest;
import com.github.starhq.template.model.dto.ResourceDTO;
import com.github.starhq.template.model.vo.ResourcePageVO;
import com.github.starhq.template.model.vo.ResourceSimpleVO;
import com.github.starhq.template.service.ResourceService;
import com.jayway.jsonpath.JsonPath;
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

import static org.junit.jupiter.api.Assertions.*;
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
 * @date 2026/5/15 12:59
 */
@Import(TestConfig.class)
@WebMvcTest(ResourceController.class)
@AutoConfigureMockMvc(addFilters = false)
class ResourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;


    @MockitoBean
    private ResourceService resourceService;
    @MockitoBean
    private MessageUtils messageUtils;

    private static final Long TEST_RESOURCE_ID = 1L;
    private static final String TEST_RESOURCE_NAME = "test-resource";
    private static final String TEST_RESOURCE_URL = "/test";

    private ResourceDTO request;
    private ResourceSimpleVO resourceResponse;
    private IPage<ResourcePageVO> resourcePage;

    @BeforeEach
    void setUp() {
        // Initialize create request
        request = new ResourceDTO();
        request.setName(TEST_RESOURCE_NAME);
        request.setUrl(TEST_RESOURCE_URL);
        request.setMethods(List.of(HttpMethod.GET));


        // Initialize resource response
        resourceResponse = new ResourceSimpleVO();
        resourceResponse.setId(TEST_RESOURCE_ID);
        resourceResponse.setName(TEST_RESOURCE_NAME);
        resourceResponse.setUrl(TEST_RESOURCE_URL);
        resourceResponse.setMethods(List.of(HttpMethod.GET));

        // Initialize page response
        ResourcePageVO pageResponse = new ResourcePageVO();
        pageResponse.setId(TEST_RESOURCE_ID);
        pageResponse.setName(TEST_RESOURCE_NAME);
        pageResponse.setUrl(TEST_RESOURCE_URL);

        Page<ResourcePageVO> page = new Page<>(1, 10);
        page.setRecords(Collections.singletonList(pageResponse));
        page.setTotal(1);
        resourcePage = page;
    }

    @Test
    void create_Success() throws Exception {
        when(resourceService.createResource(request)).thenReturn(true);

        mockMvc.perform(post(TestConstant.VERSION + "/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void create_HttpMethod_BadRequest() throws Exception {
        when(resourceService.createResource(request)).thenReturn(true);

        mockMvc.perform(post(TestConstant.VERSION + "/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":null,\"methods\":[3],\"name\":\"test-resource\",\"url\":\"/test\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_Success() throws Exception {
        when(resourceService.updateResource(TEST_RESOURCE_ID, request)).thenReturn(true);

        mockMvc.perform(put(TestConstant.VERSION + "/resources/" + TEST_RESOURCE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void delete_Success() throws Exception {
        when(resourceService.removeById(TEST_RESOURCE_ID)).thenReturn(true);

        mockMvc.perform(delete(TestConstant.VERSION + "/resources/" + TEST_RESOURCE_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void queryResources_Success() throws Exception {
        when(resourceService.page(any(PageRequest.class))).thenReturn(resourcePage);

        mockMvc.perform(get(TestConstant.VERSION + "/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("page", "1")
                        .param("size", "10")
                        .param("sort", "id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(TEST_RESOURCE_ID))
                .andExpect(jsonPath("$.data[0].name").value(TEST_RESOURCE_NAME))
                .andExpect(jsonPath("$.data[0].url").value(TEST_RESOURCE_URL));
    }

    @Test
    void queryResourceById_Success() throws Exception {
        when(resourceService.getResourceById(TEST_RESOURCE_ID)).thenReturn(resourceResponse);

        mockMvc.perform(get(TestConstant.VERSION + "/resources/" + TEST_RESOURCE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(TEST_RESOURCE_ID))
                .andExpect(jsonPath("$.data.name").value(TEST_RESOURCE_NAME))
                .andExpect(jsonPath("$.data.url").value(TEST_RESOURCE_URL));
    }

    @Test
    void queryResourceById_VerifyHttpMethod() throws Exception {
        when(resourceService.getResourceById(TEST_RESOURCE_ID)).thenReturn(resourceResponse);

        mockMvc.perform(get(TestConstant.VERSION + "/resources/" + TEST_RESOURCE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.methods").isArray())
                .andExpect(result -> {
                    String json = result.getResponse().getContentAsString();
                    List<Integer> methods = JsonPath.read(json, "$.data.methods");

                    assertTrue(HttpMethod.contains(methods.getFirst(), HttpMethod.GET));
                    assertFalse(HttpMethod.contains(methods.getFirst(), HttpMethod.PATCH));
                    assertFalse(HttpMethod.contains(methods.getFirst(), null));

                    List<HttpMethod> httpMethods = methods.stream().map(HttpMethod::fromValue).toList();
                    assertEquals(1, HttpMethod.combine(httpMethods));
                    assertEquals(0, HttpMethod.combine(Collections.emptyList()));
                });
    }
}
