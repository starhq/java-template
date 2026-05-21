package com.github.starhq.template.config.mybatis;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.github.starhq.template.config.mybatis.handler.BaseEntityHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * MyBatis-Plus core configuration class.
 *
 * <p>Registers essential MyBatis-Plus extensions such as the pagination interceptor
 * and custom automatic field population handlers via the Spring IoC container.
 *
 * @author starhq
 */
@Configuration
public class MyBatisPlusConfiguration {

    /**
     * Configures the MyBatis-Plus interceptor chain.
     *
     * <p>MyBatis-Plus uses an interceptor chain pattern to extend functionalities. This configuration
     * specifically adds the {@link PaginationInnerInterceptor}, which automatically translates
     * MyBatis-Plus's {@link com.baomidou.mybatisplus.extension.plugins.pagination.Page} object
     * into standard SQL pagination syntax (e.g., {@code LIMIT ... OFFSET ...} for PostgreSQL)
     * at runtime.
     *
     * <p><b>Note:</b> It is crucial to specify the correct {@link DbType} (PostgreSQL in this case).
     * Different databases have slightly different pagination syntax, and using the wrong dialect
     * will result in syntax errors during paginated queries.
     *
     * @return a configured {@link MybatisPlusInterceptor} instance
     */
    @Bean
    MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // Add pagination support and explicitly set the dialect to PostgreSQL
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));
        return interceptor;
    }

    /**
     * Registers the custom automatic field population handler.
     *
     * <p>This handler intercepts MyBatis insert/update operations to automatically fill
     * audit fields (like {@code createdAt}, {@code updatedBy}) defined in the base entity,
     * avoiding repetitive boilerplate code in the service layer.
     *
     * @param environment the Spring Environment, injected to determine the active profile
     *                    for conditional user ID resolution (see {@link BaseEntityHandler})
     * @return a configured {@link BaseEntityHandler} instance
     */
    @Bean
    BaseEntityHandler baseEntityHandler(Environment environment) {
        return new BaseEntityHandler(environment);
    }
}
