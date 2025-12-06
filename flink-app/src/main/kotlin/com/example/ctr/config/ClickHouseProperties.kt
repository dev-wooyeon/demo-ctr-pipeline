package com.example.ctr.config

import jakarta.validation.constraints.NotBlank

class ClickHouseProperties {
    @field:NotBlank
    var url: String = ""

    @field:NotBlank
    var driver: String = ""
}
