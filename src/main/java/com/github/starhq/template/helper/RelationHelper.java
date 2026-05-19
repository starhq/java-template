package com.github.starhq.template.helper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.github.starhq.template.common.constant.CacheConstant;
import com.github.starhq.template.common.enums.ErrorCode;
import com.github.starhq.template.common.exception.BusinessException;
import com.github.starhq.template.common.exception.NotFoundException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: many to many relations helper
 * @date 2026/4/14 23:36
 */
@Component
@RequiredArgsConstructor
public class RelationHelper {


    @Getter
    @AllArgsConstructor
    public enum AssociationType {
        RESOURCE(CacheConstant.RESOURCE, ErrorCode.RESOURCE_NOT_FOUND, ErrorCode.ROLE_ASSIGN_RESOURCES_FAILED),
        MENU(CacheConstant.MENU, ErrorCode.MENU_NOT_FOUND, ErrorCode.ROLE_ASSIGN_MENUS_FAILED),
        BUTTON(CacheConstant.BUTTON, ErrorCode.BUTTON_NOT_FOUND, ErrorCode.ROLE_ASSIGN_BUTTONS_FAILED),
        USER(CacheConstant.USER, ErrorCode.USER_NOT_FOUND, ErrorCode.USER_ASSIGN_FAILED);

        private final String name;
        private final ErrorCode notFoundError;
        private final ErrorCode assignError;
    }

    /**
     * 通用的多对多分配逻辑
     */
    public <E> void assignRelations(Long ownerId,
                                    Set<Long> targetIds,
                                    Consumer<Long> deleteLogic,
                                    BiFunction<Long, Long, E> entityFactory,
                                    Consumer<List<E>> upsertLogic,
                                    Consumer<Set<Long>> validator,
                                    AssociationType type) {

        if (CollectionUtils.isEmpty(targetIds)) {
            if (deleteLogic != null) {
                deleteLogic.accept(ownerId);
            }
            return;
        }

        if (validator != null) {
            validator.accept(targetIds);
        }

        List<E> entities = targetIds.stream().map(id -> entityFactory.apply(ownerId, id)).toList();

        try {
            upsertLogic.accept(entities);
        } catch (Exception e) {
            throw new BusinessException(type.getAssignError(), e);
        }
    }

    /**
     * 通用的实体存在性校验
     */
    public <X> void validateEntityExists(Set<Long> ids, BaseMapper<X> mapper, SFunction<X, Long> idGetter, ErrorCode notFound) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        long validCount = mapper.selectCount(new LambdaQueryWrapper<X>().in(idGetter, ids));
        if (validCount != ids.size()) {
            throw new NotFoundException(notFound);
        }
    }
}
