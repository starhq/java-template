package com.github.starhq.template.config.cache;

import java.lang.reflect.Method;
import java.util.StringJoiner;

import org.springframework.cache.interceptor.KeyGenerator;

public class CacheKeyGenerator implements KeyGenerator {

    @Override
    public Object generate(Object target, Method method, Object... params) {
        StringJoiner joiner = new StringJoiner(":");

        joiner.add(target.getClass().getSimpleName());
        joiner.add(method.getName());

        for (Object param : params) {
            joiner.add(String.valueOf(param));
        }

        return joiner.toString();
    }

}
