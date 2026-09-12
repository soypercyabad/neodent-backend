package com.neodent.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TurnstileResponse(
    boolean success
) {}