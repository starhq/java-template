package com.github.starhq.template;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class JavaTemplateApplicationTests {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaTemplateApplicationTests.class);

    @Test
    void contextLoads() {
        LOGGER.trace("trace");
        LOGGER.debug("debug");
        LOGGER.info("info");
        LOGGER.warn("warn");
        LOGGER.error("error≤");
    }

}
