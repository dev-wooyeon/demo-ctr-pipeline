package com.example.ctr.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DuckDBProperties {

    @NotBlank
    private String url;
}
