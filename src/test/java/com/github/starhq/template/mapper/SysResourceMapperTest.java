package com.github.starhq.template.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.starhq.template.BaseMapperTest;
import com.github.starhq.template.entity.SysResource;
import com.github.starhq.template.enums.HttpMethod;
import com.github.starhq.template.vo.ResourceVO;

class SysResourceMapperTest extends BaseMapperTest {

    @Autowired
    private SysResourceMapper resourceMapper;

    @Test
    void insertResource_shouldInsertSuccessfully() {
        SysResource resource = prepare(100L, "Create Resource");

        int result = resourceMapper.insert(resource);

        assertThat(result).isEqualTo(1);
        assertThat(resource.getId()).isNotNull().isGreaterThan(0L);

        SysResource dbResource = resourceMapper.selectById(resource.getId());
        assertThat(dbResource).isNotNull();
        assertThat(dbResource.getName()).isEqualTo("Create Resource");
    }

    @Test
    void updateResource_shouldUpdateSuccessfully() {
        SysResource resource = prepare(101L, "Update Resource");
        resourceMapper.insert(resource);

        resource.setName("Updated Update Resource");
        resource.setDescription("Updated description");

        int result = resourceMapper.updateById(resource);

        assertThat(result).isEqualTo(1);

        SysResource dbResource = resourceMapper.selectById(resource.getId());
        assertThat(dbResource.getName()).isEqualTo("Updated Update Resource");
        assertThat(dbResource.getDescription()).isEqualTo("Updated description");
    }

    @Test
    void selectResourcePage_shouldReturnPagedResult() {
        SysResource resource = prepare(102L, "Page Resource");
        resourceMapper.insert(resource);

        Page<ResourceVO> page = new Page<>(1, 10);
        QueryWrapper<ResourceVO> wrapper = new QueryWrapper<>();
        wrapper.orderBy(true, false, "id");

        IPage<ResourceVO> result = resourceMapper.selectResourcePage(page, wrapper);

        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isGreaterThan(0);
        assertThat(result.getRecords()).hasSize(10);
        assertThat(result.getRecords().get(0).getName()).isEqualTo("Page Resource");
    }

    @Test
    void selectResourcesByRoleId_shouldReturnResource() {
        List<ResourceVO> result = resourceMapper.selectResourcesByRoleId(1L);

        assertThat(result).isNotNull();
        assertThat(result.size()).isGreaterThan(0);
    }

    @Test
    void selectResourcesByRoleIds_shouldReturnResource() {
        List<ResourceVO> result = resourceMapper.selectAssignedResourceByRoleIds(List.of(1L));

        assertThat(result).isNotNull();
        assertThat(result.size()).isGreaterThan(0);
    }

    @Test
    void deleteResource_shouldDeleteSuccessfully() {
        SysResource resource = prepare(103L, "Delete Resource");
        resourceMapper.insert(resource);

        int result = resourceMapper.deleteById(resource.getId());

        assertThat(result).isEqualTo(1);

        SysResource dbResource = resourceMapper.selectById(resource.getId());
        assertThat(dbResource).isNull();
    }

    private SysResource prepare(Long id, String name) {
        SysResource resource = new SysResource();
        resource.setId(id);
        resource.setUrl("/**");
        resource.setMethod(HttpMethod.GET);
        resource.setName(name);
        resource.setDescription("Description for " + name);
        resource.setCreatedBy(1L);
        resource.setCreatedAt(OffsetDateTime.now());
        return resource;
    }
}