package com.example.ctr.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClickHouseProperties {

    @NotBlank
    private String url;

    @NotBlank
    private String driver;
}
