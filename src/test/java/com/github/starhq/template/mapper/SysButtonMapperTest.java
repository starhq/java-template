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
import com.github.starhq.template.entity.SysButton;
import com.github.starhq.template.vo.ButtonVO;

class SysButtonMapperTest extends BaseMapperTest {

    @Autowired
    private SysButtonMapper buttonMapper;

    @Test
    void insertButton_shouldInsertSuccessfully() {
        SysButton button = prepare(101L, "btn_save", "Save Button");

        int result = buttonMapper.insert(button);

        assertThat(result).isEqualTo(1);
        assertThat(button.getId()).isNotNull().isGreaterThan(0L);

        SysButton dbButton = buttonMapper.selectById(button.getId());
        assertThat(dbButton).isNotNull();
        assertThat(dbButton.getCode()).isEqualTo("btn_save");
        assertThat(dbButton.getName()).isEqualTo("Save Button");
    }

    @Test
    void updateButton_shouldUpdateSuccessfully() {
        SysButton button = prepare(102L, "btn_cancel", "Cancel Button");
        buttonMapper.insert(button);

        button.setName("Updated Cancel Button");
        button.setDescription("Updated description");

        int result = buttonMapper.updateById(button);

        assertThat(result).isEqualTo(1);

        SysButton dbButton = buttonMapper.selectById(button.getId());
        assertThat(dbButton.getName()).isEqualTo("Updated Cancel Button");
        assertThat(dbButton.getDescription()).isEqualTo("Updated description");
    }

    @Test
    void selectButtonPage_shouldReturnPagedResult() {
        SysButton button = prepare(103L, "btn_cancel", "Cancel Button");
        buttonMapper.insert(button);

        Page<ButtonVO> page = new Page<>(1, 10);
        QueryWrapper<ButtonVO> wrapper = new QueryWrapper<>();
        wrapper.orderBy(true, false, "id");

        IPage<ButtonVO> result = buttonMapper.selectButtonPage(1L, page, wrapper);

        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isGreaterThan(0);
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getName()).isEqualTo("Cancel Button");
    }

    @Test
    void selectButtonsByRoleId_shouldReturnResource() {
        List<ButtonVO> result = buttonMapper.selectButtonsByRoleId(1L);

        assertThat(result).isNotNull();
        assertThat(result.size()).isGreaterThan(0);
    }

    @Test
    void selectAssignedResourcesByRoleIds_shouldReturnResource() {
        List<SysButton> result = buttonMapper.selectAssignedButtonsByRoleIds(List.of(1L));

        assertThat(result).isNotNull();
        assertThat(result.size()).isGreaterThan(0);
    }

    @Test
    void findById_shouldReturnButton() {
        SysButton button = prepare(104L, "btn_delete", "Delete Button");
        buttonMapper.insert(button);

        SysButton result = buttonMapper.selectById(button.getId());

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(button.getId());
        assertThat(result.getCode()).isEqualTo("btn_delete");
    }

    @Test
    void deleteButton_shouldDeleteSuccessfully() {
        SysButton button = prepare(105L, "btn_test", "Test Button");
        buttonMapper.insert(button);

        int result = buttonMapper.deleteById(button.getId());

        assertThat(result).isEqualTo(1);

        SysButton dbButton = buttonMapper.selectById(button.getId());
        assertThat(dbButton).isNull();
    }

    private SysButton prepare(Long id, String code, String name) {
        SysButton button = new SysButton();
        button.setId(id);
        button.setMenuId(1L);
        button.setCode(code);
        button.setName(name);
        button.setDescription("Description for " + name);
        button.setCreatedAt(OffsetDateTime.now());
        button.setCreatedBy(1L);
        return button;
    }
}