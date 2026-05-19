package com.github.starhq.template.model.dto;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.github.starhq.template.config.json.serializer.SensitivePropertyFilter;

@JsonFilter(SensitivePropertyFilter.FILTER_NAME)
public abstract class SensitiveDTO implements Serializable {
}
