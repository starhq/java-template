package com.github.starhq.template.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;

import com.github.starhq.template.model.vo.resource.ResourceCheckVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.github.starhq.template.BaseMapperTest;
import com.github.starhq.template.common.enums.HttpMethod;
import com.github.starhq.template.entity.SysResource;

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
    void selectResourcesByRoleId_shouldReturnResource() {
        List<ResourceCheckVO> result = resourceMapper.selectResourcesByRoleId(1L);

        assertThat(result).isNotNull();
        assertThat(result.getFirst().getChecked()).isTrue();
        assertThat(result.size()).isGreaterThan(0);
    }

    @Test
    void selectAssignedResourceByUserId_shouldReturnResource() {
        List<SysResource> result = resourceMapper.selectAssignedResourceByUserId(1L);

        assertThat(result).isNotNull();
        assertThat(result.getFirst().getMethods()).isEqualTo(31);
        assertThat(result.size()).isGreaterThan(0);
    }

    @Test
    void selectUserIdsByResourceId_shouldReturnUserIds() {
        List<Long> result = resourceMapper.selectUserIdsByResourceId(1L);

        assertThat(result).isNotNull();
        assertThat(result.getFirst()).isEqualTo(1);
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
        resource.setMethods(HttpMethod.combine(List.of(HttpMethod.GET, HttpMethod.POST)));
        resource.setName(name);
        resource.setDescription("Description for " + name);
        resource.setCreatedBy(1L);
        resource.setCreatedAt(OffsetDateTime.now());
        return resource;
    }
}