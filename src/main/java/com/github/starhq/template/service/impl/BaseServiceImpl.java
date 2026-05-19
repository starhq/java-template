package com.github.starhq.template.service.impl;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.starhq.template.common.enums.ErrorCode;
import com.github.starhq.template.common.exception.BusinessException;
import com.github.starhq.template.common.exception.CustomException;
import com.github.starhq.template.common.exception.DuplicateException;
import com.github.starhq.template.common.exception.NotFoundException;
import org.springframework.dao.DuplicateKeyException;

import java.io.Serializable;
import java.util.Optional;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: enhance service impl
 * @date 2026/3/28 09:57
 */
public class BaseServiceImpl<M extends BaseMapper<T>, T>
        extends ServiceImpl<M, T> {

    protected T getAndCheckById(Serializable id, ErrorCode notFound) {
        return Optional.ofNullable(getBaseMapper().selectById(id))
                .orElseThrow(() -> new NotFoundException(notFound));
    }

    protected void insert(T data, ErrorCode duplicate, ErrorCode insert) {
        try {
            getBaseMapper().insert(data);
        } catch (DuplicateKeyException e) {
            throw new DuplicateException(duplicate, e);
        } catch (Exception e) {
            throw new BusinessException(insert, e);
        }
    }

    protected void update(T data, ErrorCode duplicate, ErrorCode update, ErrorCode notFound) {
        try {
            if (getBaseMapper().updateById(data) <= 0) {
                throw new NotFoundException(notFound);
            }
        } catch (DuplicateKeyException e) {
            throw new DuplicateException(duplicate, e);
        } catch (Exception e) {
            if (e instanceof CustomException ce) {
                throw ce;
            }
            throw new BusinessException(update, e);
        }
    }

    protected void delete(Serializable id, ErrorCode notFound, ErrorCode delete) {
        try {
            if (getBaseMapper().deleteById(id) <= 0) {
                throw new NotFoundException(notFound);
            }
        } catch (Exception e) {
            if (e instanceof CustomException ce) {
                throw ce;
            }
            throw new BusinessException(delete, e);
        }
    }
}
