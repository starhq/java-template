package com.github.starhq.template.config.mybatis;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.github.starhq.template.config.mybatis.handler.BaseEntityHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * MyBatis-Plus配置类
 *
 * @author starhq
 */
@Configuration
public class MyBatisPlusConfiguration {

    /**
     * 分页插件
     *
     * @return MybatisPlusInterceptor
     */
    @Bean
    MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));
        return interceptor;
    }

    @Bean
    BaseEntityHandler baseEntityHandler(Environment environment) {
        return new BaseEntityHandler(environment);
    }

}
