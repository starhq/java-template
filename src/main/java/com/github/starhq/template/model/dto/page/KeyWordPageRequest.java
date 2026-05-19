package com.github.starhq.template.model.dto.page;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 分页请求DTO
 *
 * @author starhq
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class KeyWordPageRequest extends PageRequest {


    @Serial
    private static final long serialVersionUID = 8515119070863756538L;

    private String keyword;
}
