package com.github.starhq.template.mapper;

import com.github.starhq.template.BaseMapperTest;
import com.github.starhq.template.entity.SysRole;
import com.github.starhq.template.model.vo.role.RoleCheckVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SysRoleMapperTest extends BaseMapperTest {

    @Autowired
    private SysRoleMapper roleMapper;

    @Test
    void insertRole_shouldInsertSuccessfully() {
        SysRole role = prepare(101L, "ADMIN", "Administrator");

        int result = roleMapper.insert(role);

        assertThat(result).isEqualTo(1);
        assertThat(role.getId()).isNotNull().isGreaterThan(0L);

        SysRole dbRole = roleMapper.selectById(role.getId());
        assertThat(dbRole).isNotNull();
        assertThat(dbRole.getCode()).isEqualTo("ADMIN");
        assertThat(dbRole.getName()).isEqualTo("Administrator");
    }

    @Test
    void updateRole_shouldUpdateSuccessfully() {
        SysRole role = prepare(102L, "USER", "User");
        roleMapper.insert(role);

        role.setName("Updated User");
        role.setDescription("Updated description");
        role.setUpdatedAt(OffsetDateTime.now());
        role.setUpdatedBy(1L);

        int result = roleMapper.updateById(role);

        assertThat(result).isEqualTo(1);

        SysRole dbRole = roleMapper.selectById(role.getId());
        assertThat(dbRole.getName()).isEqualTo("Updated User");
        assertThat(dbRole.getDescription()).isEqualTo("Updated description");
    }

    @Test
    void selectRolesByUserId_shouldReturnPagedResult() {
        List<RoleCheckVO> result = roleMapper.selectRolesByUserId(1L);

        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.getFirst().getName()).isEqualTo("Administrator");
    }

    @Test
    void findById_shouldReturnRole() {
        SysRole role = prepare(104L, "MODERATOR", "Moderator");
        roleMapper.insert(role);

        SysRole result = roleMapper.selectById(role.getId());

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(role.getId());
        assertThat(result.getCode()).isEqualTo("MODERATOR");
    }

    @Test
    void deleteRole_shouldDeleteSuccessfully() {
        SysRole role = prepare(105L, "TEST", "Test Role");
        roleMapper.insert(role);

        int result = roleMapper.deleteById(role.getId());

        assertThat(result).isEqualTo(1);

        SysRole dbRole = roleMapper.selectById(role.getId());
        assertThat(dbRole).isNull();
    }

    private SysRole prepare(Long id, String code, String name) {
        SysRole role = new SysRole();
        role.setId(id);
        role.setCode(code);
        role.setName(name);
        role.setDescription("Description for " + name);
        role.setIsDefault(false);
        role.setCreatedAt(OffsetDateTime.now());
        role.setCreatedBy(1L);
        return role;
    }
}