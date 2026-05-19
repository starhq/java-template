package com.github.starhq.template.mapper;

import com.github.starhq.template.BaseMapperTest;
import com.github.starhq.template.entity.SysDictType;
import com.github.starhq.template.model.vo.dictType.DictTypeWithDataVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SysDictTypeMapperTest extends BaseMapperTest {

    @Autowired
    private SysDictTypeMapper dictTypeMapper;

    @Test
    void insertDictType_shouldInsertSuccessfully() {
        SysDictType dictType = prepare(101L, "user_status_1", "User Status");

        int result = dictTypeMapper.insert(dictType);

        assertThat(result).isEqualTo(1);
        assertThat(dictType.getId()).isNotNull().isGreaterThan(0L);

        SysDictType dbDictType = dictTypeMapper.selectById(dictType.getId());
        assertThat(dbDictType).isNotNull();
        assertThat(dbDictType.getType()).isEqualTo("user_status_1");
        assertThat(dbDictType.getName()).isEqualTo("User Status");
    }

    @Test
    void updateDictType_shouldUpdateSuccessfully() {
        SysDictType dictType = prepare(102L, "gender", "Gender");
        dictTypeMapper.insert(dictType);

        dictType.setName("Updated Gender");
        dictType.setDescription("Updated gender description");
        dictType.setCreatedAt(OffsetDateTime.now());
        dictType.setCreatedBy(1L);

        int result = dictTypeMapper.updateById(dictType);

        assertThat(result).isEqualTo(1);

        SysDictType dbDictType = dictTypeMapper.selectById(dictType.getId());
        assertThat(dbDictType.getName()).isEqualTo("Updated Gender");
        assertThat(dbDictType.getDescription()).isEqualTo("Updated gender description");
    }

    @Test
    void findById_shouldReturnDictType() {
        SysDictType dictType = prepare(103L, "role_type", "Role Type");
        dictTypeMapper.insert(dictType);

        SysDictType result = dictTypeMapper.selectById(dictType.getId());

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(dictType.getId());
        assertThat(result.getType()).isEqualTo("role_type");
    }

    @Test
    void deleteDictType_shouldDeleteSuccessfully() {
        SysDictType dictType = prepare(104L, "test_type", "Test Type");
        dictTypeMapper.insert(dictType);

        int result = dictTypeMapper.deleteById(dictType.getId());

        assertThat(result).isEqualTo(1);

        SysDictType dbDictType = dictTypeMapper.selectById(dictType.getId());
        assertThat(dbDictType).isNull();
    }

    @Test
    void selectDictTypes_shouldReturnResult() {
        List<DictTypeWithDataVO> result = dictTypeMapper.selectDictTypesWithData();

        assertThat(result).isNotNull();
        assertThat(result.size()).isGreaterThan(0);
    }

    private SysDictType prepare(Long id, String type, String name) {
        SysDictType dictType = new SysDictType();
        dictType.setId(id);
        dictType.setType(type);
        dictType.setName(name);
        dictType.setDescription("Description for " + name);
        dictType.setCreatedAt(OffsetDateTime.now());
        dictType.setCreatedBy(1L);
        return dictType;
    }
}