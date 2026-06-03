package com.github.starhq.template.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.starhq.template.BaseMapperTestConfiguration;
import com.github.starhq.template.entity.SysToken;
import com.github.starhq.template.model.vo.TokenPageVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TokenMapperTest extends BaseMapperTestConfiguration {

    @Autowired
    private SysTokenMapper tokenMapper;

    @Test
    void insertToken_shouldInsertSuccessfully() {
        SysToken token = prepare(1L);
        int result = tokenMapper.insert(token);

        assertThat(result).isEqualTo(1);
        assertThat(token.getId()).isNotNull().isGreaterThan(0L);

        SysToken dbToken = tokenMapper.selectById(token.getId());
        assertThat(dbToken).isNotNull();
        assertThat(dbToken.getUserId()).isEqualTo(1L);
    }

    @Test
    void selectTokenPage_shouldReturnPagedResult() {
        SysToken token = prepare(2L);
        tokenMapper.insert(token);

        Page<TokenPageVO> page = new Page<>(1, 10);
        QueryWrapper<TokenPageVO> wrapper = new QueryWrapper<>();
        wrapper.likeRight("u.username", "admin");
        wrapper.orderBy(true, false, "id");

        IPage<TokenPageVO> result = tokenMapper.selectTokenPage(page, wrapper);

        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isGreaterThan(0);
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().getFirst().getUsername()).isEqualTo("admin");
    }

    @Test
    void findByUserId_shouldReturnUser() {
        SysToken token = prepare(3L);
        tokenMapper.insert(token);

        QueryWrapper<SysToken> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", token.getUserId());

        SysToken result = tokenMapper.selectOne(wrapper);

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(token.getUserId());
    }

    @Test
    void deleteToken_shouldDeleteSuccessfully() {
        SysToken token = prepare(4L);
        tokenMapper.insert(token);

        int result = tokenMapper.deleteById(token.getId());

        assertThat(result).isEqualTo(1);

        SysToken dbToken = tokenMapper.selectById(token.getId());
        assertThat(dbToken).isNull();
    }

    @Test
    void upsertToken_shouldUpsertSuccessfully() {
        SysToken token = prepare(4L);
        tokenMapper.upsertToken(token);

        token.setAccessToken("upsert_token");
        tokenMapper.upsertToken(token);

        SysToken dbToken = tokenMapper.selectById(token.getId());
        assertThat(dbToken).isNotNull();
        assertThat(dbToken.getAccessToken()).isEqualTo("upsert_token");
    }

    private SysToken prepare(Long id) {
        SysToken token = new SysToken();
        token.setAccessToken("access_token");
        token.setRefreshToken("refresh_token");
        token.setCreatedAt(OffsetDateTime.now());
        token.setDeviceFingerprint("whatever");
        token.setExpiredAt(OffsetDateTime.now());
        token.setId(id);
        token.setUserId(1L);
        token.setRevoked(false);
        token.setLoginIp("127.0.0.1");
        return token;
    }

}
