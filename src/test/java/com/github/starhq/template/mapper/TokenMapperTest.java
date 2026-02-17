package com.github.starhq.template.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.starhq.template.BaseMapperTest;
import com.github.starhq.template.entity.SysToken;
import com.github.starhq.template.vo.TokenVO;

class TokenMapperTest extends BaseMapperTest {

    @Autowired
    private SysTokenMapper tokenMapper;

    @Test
    void insertToken_shouldInsertSuccessfully() {
        SysToken token = prepare(1L, 1L);
        int result = tokenMapper.insert(token);

        assertThat(result).isEqualTo(1);
        assertThat(token.getId()).isNotNull().isGreaterThan(0L);

        SysToken dbToken = tokenMapper.selectById(token.getId());
        assertThat(dbToken).isNotNull();
        assertThat(dbToken.getUserId()).isEqualTo(1L);
    }

    @Test
    void selectTokenPage_shouldReturnPagedResult() {
        SysToken token = prepare(2L, 1L);
        tokenMapper.insert(token);

        Page<TokenVO> page = new Page<>(1, 10);
        QueryWrapper<TokenVO> wrapper = new QueryWrapper<>();
        wrapper.orderBy(true, false, "id");

        IPage<TokenVO> result = tokenMapper.selectTokenPage("admin", page, wrapper);

        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isGreaterThan(0);
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getUsername()).isEqualTo("admin");
    }

    @Test
    void findByUserId_shouldReturnUser() {
        SysToken token = prepare(3L, 1L);
        tokenMapper.insert(token);

        QueryWrapper<SysToken> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", token.getUserId());

        SysToken result = tokenMapper.selectOne(wrapper);

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(token.getUserId());
    }

    @Test
    void deleteToken_shouldDeleteSuccessfully() {
        SysToken token = prepare(4L, 1L);
        tokenMapper.insert(token);

        int result = tokenMapper.deleteById(token.getId());

        assertThat(result).isEqualTo(1);

        SysToken dbToken = tokenMapper.selectById(token.getId());
        assertThat(dbToken).isNull();
    }

    private SysToken prepare(Long id, Long userID) {
        SysToken token = new SysToken();
        token.setAccessToken("access_token");
        token.setRefreshToken("refresh_token");
        token.setCreatedAt(OffsetDateTime.now());
        token.setDeviceFingerprint("whatever");
        token.setExpiredAt(OffsetDateTime.now());
        token.setId(id);
        token.setUserId(userID);
        token.setRevoked(false);
        token.setLoginIp("127.0.0.1");
        return token;
    }

}
