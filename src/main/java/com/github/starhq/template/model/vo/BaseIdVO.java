package com.github.starhq.template.model.vo;

import lombok.Data;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.io.Serializable;

@Data
public class BaseIdVO implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
}
