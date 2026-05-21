package com.github.starhq.template.mapper;

import com.github.starhq.template.BaseMapperTest;
import com.github.starhq.template.entity.SysDictData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SysDictDataMapperTest extends BaseMapperTest {

    @Autowired
    private SysDictDataMapper dictDataMapper;

    @Test
    void insertDictData_shouldInsertSuccessfully() {
        SysDictData dictData = prepare(101L, "Active", "active_1");

        int result = dictDataMapper.insert(dictData);

        assertThat(result).isEqualTo(1);
        assertThat(dictData.getId()).isNotNull().isGreaterThan(0L);

        SysDictData dbDictData = dictDataMapper.selectById(dictData.getId());
        assertThat(dbDictData).isNotNull();
        assertThat(dbDictData.getLabel()).isEqualTo("Active");
        assertThat(dbDictData.getValue()).isEqualTo("active_1");
    }

    @Test
    void updateDictData_shouldUpdateSuccessfully() {
        SysDictData dictData = prepare(102L, "Inactive", "inactive_1");
        dictDataMapper.insert(dictData);

        dictData.setLabel("Updated Inactive");
        dictData.setDescription("Updated description");
        dictData.setUpdatedAt(OffsetDateTime.now());
        dictData.setUpdatedBy(1L);

        int result = dictDataMapper.updateById(dictData);

        assertThat(result).isEqualTo(1);

        SysDictData dbDictData = dictDataMapper.selectById(dictData.getId());
        assertThat(dbDictData.getLabel()).isEqualTo("Updated Inactive");
        assertThat(dbDictData.getDescription()).isEqualTo("Updated description");
    }

    @Test
    void findById_shouldReturnDictData() {
        SysDictData dictData = prepare(103L, "Male", "male");
        dictDataMapper.insert(dictData);

        SysDictData result = dictDataMapper.selectById(dictData.getId());

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(dictData.getId());
        assertThat(result.getLabel()).isEqualTo("Male");
    }

    @Test
    void deleteDictData_shouldDeleteSuccessfully() {
        SysDictData dictData = prepare(105L, "Temp", "temp");
        dictDataMapper.insert(dictData);

        int result = dictDataMapper.deleteById(dictData.getId());

        assertThat(result).isEqualTo(1);

        SysDictData dbDictData = dictDataMapper.selectById(dictData.getId());
        assertThat(dbDictData).isNull();
    }

    private SysDictData prepare(Long id, String label, String value) {
        SysDictData dictData = new SysDictData();
        dictData.setId(id);
        dictData.setTypeId(1L);
        dictData.setLabel(label);
        dictData.setValue(value);
        dictData.setDescription("Description for " + label);
        dictData.setCreatedAt(OffsetDateTime.now());
        dictData.setCreatedBy(1L);
        return dictData;
    }
}