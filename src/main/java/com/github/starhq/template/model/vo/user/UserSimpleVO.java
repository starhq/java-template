package com.github.starhq.template.model.vo.user;

import com.github.starhq.template.common.enums.UserStatus;

import com.github.starhq.template.model.vo.BaseIdVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class UserSimpleVO extends BaseIdVO {

    private String username;
    private UserStatus status;
}