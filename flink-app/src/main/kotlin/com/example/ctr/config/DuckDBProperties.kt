package com.example.ctr.config

import jakarta.validation.constraints.NotBlank

class DuckDBProperties {
    @field:NotBlank
    var url: String = ""
}
