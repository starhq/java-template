package com.github.starhq.template.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.starhq.template.common.constant.CacheConstant;
import com.github.starhq.template.entity.BaseEntity;
import com.github.starhq.template.helper.CacheHelper;
import com.github.starhq.template.model.dto.page.PageRequest;
import com.github.starhq.template.model.vo.BaseAuditVO;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: auditable page service
 * @date 2026/4/14 12:40
 */
public class AuditBaseServiceImpl<M extends BaseMapper<T>, T extends BaseEntity>
        extends BaseServiceImpl<M, T> {

    @Autowired
    protected CacheHelper cacheHelper;

    protected <E extends BaseAuditVO, P extends PageRequest> IPage<E> pageVO(
            P request,
            Consumer<QueryWrapper<T>> queryConsumer,
            Function<Set<Long>, Map<Long, String>> dbLoader,
            Function<T, E> voMapper) {

        QueryWrapper<T> queryWrapper = request.toQueryWrapper();
        if (queryConsumer != null) {
            queryConsumer.accept(queryWrapper);
        }
        Page<T> page = request.toPage();

        IPage<T> pages = getBaseMapper().selectPage(page, queryWrapper);

        if (pages.getRecords().isEmpty()) {
            return new Page<>(pages.getCurrent(), pages.getSize(), pages.getTotal());
        }

        // Extract Audit IDs (CreatedBy/UpdatedBy)
        Set<Long> auditIds = pages.getRecords().stream()
                .flatMap(e -> Stream.of(e.getCreatedBy(), e.getUpdatedBy()))
                .filter(Objects::nonNull).collect(Collectors.toSet());

        // Use the new CacheHelper
        Map<Long, String> nameMap = cacheHelper.getBatchWithCache(auditIds, CacheConstant.ADMIN_NAME,
                dbLoader);

        return pages.convert(entity -> {
            E vo = voMapper.apply(entity);
            vo.populateAuditFields(entity.getCreatedBy(), entity.getUpdatedBy(), nameMap);
            return vo;
        });
    }
}
