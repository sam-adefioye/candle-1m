package org.com.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Message (
    String method,
    String channel,
    String type,
    Map<String, Object> params,
    boolean success,
    String text,
    String error,
    List<Map<String, Object>> data,
    Map<String, Object> result
) {}
