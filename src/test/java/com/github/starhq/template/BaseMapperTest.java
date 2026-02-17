package com.github.starhq.template;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.baomidou.mybatisplus.test.autoconfigure.MybatisPlusTest;

@ActiveProfiles("dev")
@MybatisPlusTest
@Import({ TestcontainersConfiguration.class })
@ImportAutoConfiguration({ FlywayAutoConfiguration.class })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class BaseMapperTest {

}
