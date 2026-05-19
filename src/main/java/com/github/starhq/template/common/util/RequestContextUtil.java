package com.github.starhq.template.common.util;

import com.github.starhq.template.model.dto.RequestContext;
import lombok.experimental.UtilityClass;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description:
 * @date 2026/5/12 13:12
 */
@UtilityClass
public class RequestContextUtil {

    private static final ThreadLocal<RequestContext> CONTEXT = new ThreadLocal<>();


    public static void setContext(RequestContext context) {
        CONTEXT.set(context);
    }

    public static RequestContext getContext() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
