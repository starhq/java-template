package com.github.starhq.template.model.vo.user;

import com.github.starhq.template.common.enums.UserStatus;

import com.github.starhq.template.model.vo.BaseAuditVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class UserPageVO extends BaseAuditVO {

    private String username;
    private UserStatus status;
}
