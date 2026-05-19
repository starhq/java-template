package com.github.starhq.template.helper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.starhq.template.entity.SysUser;
import com.github.starhq.template.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: extract user's id and name as a map
 * @date 2026/4/14 14:22
 */
@Component
@RequiredArgsConstructor
public class SysUserMapperHelper implements Function<Set<Long>, Map<Long, String>> {

    private final SysUserMapper userMapper;

    @Override
    public Map<Long, String> apply(Set<Long> ids) {
        List<SysUser> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>().select(SysUser::getId, SysUser::getUsername).in(SysUser::getId, ids));
        if (CollectionUtils.isEmpty(users)) {
            return Collections.emptyMap();
        }
        return users.stream().collect(Collectors.toMap(SysUser::getId, SysUser::getUsername));
    }
}
