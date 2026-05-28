package com.github.starhq.template.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.starhq.template.BaseMapperTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.github.starhq.template.entity.SysApiLog;

class SysApiLogMapperTest extends BaseMapperTestConfiguration {

    @Autowired
    private SysApiLogMapper sysApiLogMapper;

    @Test
    void insertApiLog_shouldInsertSuccessfully() {
        SysApiLog apiLog = prepare();

        int result = sysApiLogMapper.insert(apiLog);

        assertThat(result).isEqualTo(1);
        assertThat(apiLog.getId()).isNotNull().isGreaterThan(0L);

        SysApiLog dbApiLog = sysApiLogMapper.selectById(apiLog.getId());
        assertThat(dbApiLog).isNotNull();
        assertThat(dbApiLog.getId()).isEqualTo(101L);
        assertThat(dbApiLog.getTraceId()).isEqualTo("traceId");
    }

    private SysApiLog prepare() {
        SysApiLog apiLog = new SysApiLog();
        apiLog.setId(101L);
        apiLog.setTraceId("traceId");
        apiLog.setMethod("GET");
        apiLog.setUri("/test");
        apiLog.setQueryString("id=1");
        apiLog.setClientIp("127.0.0.1");
        apiLog.setHttpStatus(200);
        apiLog.setDuration(200L);

        // 处理请求信息
        apiLog.setHeaders("HEADERS");
        apiLog.setParams("params");

        apiLog.setRequestBody("request_body");

        // 处理响应信息
        apiLog.setResponseBody("wrong");

        // 处理异常信息
        apiLog.setExceptionMessage("wrong");
        apiLog.setExceptionStack("exception msg");
        return apiLog;
    }

}
