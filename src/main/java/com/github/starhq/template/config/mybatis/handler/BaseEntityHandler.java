package com.github.starhq.template.config.mybatis.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.github.starhq.template.common.constant.ProfileConstants;
import com.github.starhq.template.common.util.SecurityContextUtils;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class BaseEntityHandler implements MetaObjectHandler {

    private final Environment environment;

    @Override
    public void insertFill(MetaObject metaObject) {
        OffsetDateTime now = OffsetDateTime.now();
        Long currentUserId = getCurrentUserId();

        // Fill creation timestamp if not already set
        this.strictInsertFill(metaObject, "createdAt", OffsetDateTime.class, now);
        // Fill creator user ID if not already set
        this.strictInsertFill(metaObject, "createdBy", Long.class, currentUserId);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        OffsetDateTime now = OffsetDateTime.now();
        Long currentUserId = getCurrentUserId();

        // Fill update timestamp
        this.strictUpdateFill(metaObject, "updatedAt", OffsetDateTime.class, now);
        // Fill updater user ID
        this.strictUpdateFill(metaObject, "updatedBy", Long.class, currentUserId);
    }

    private Long getCurrentUserId(){
        boolean isProd = environment.acceptsProfiles(Profiles.of(ProfileConstants.PROD));
        return isProd ? SecurityContextUtils.getRequiredUserId() : 1L;
    }
}
