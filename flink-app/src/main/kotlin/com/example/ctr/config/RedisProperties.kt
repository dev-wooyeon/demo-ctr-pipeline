package com.example.ctr.config

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive

class RedisProperties {
    @field:NotBlank
    var host: String = ""

    @field:Positive
    var port: Int = 6379
}
