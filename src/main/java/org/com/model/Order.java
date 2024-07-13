package org.com.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record Order (Double price, @JsonProperty("qty") BigDecimal quantity) {}
