package com.example.ctr.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RedisProperties {

    @NotBlank
    private String host;

    @Positive
    private int port;
}
