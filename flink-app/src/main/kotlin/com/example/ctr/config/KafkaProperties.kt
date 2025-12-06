package com.example.ctr.config

import jakarta.validation.constraints.NotBlank

class KafkaProperties {
    @field:NotBlank
    var bootstrapServers: String = ""

    @field:NotBlank
    var groupId: String = ""

    var topics: Topics = Topics()

    class Topics {
        @field:NotBlank
        var impression: String = ""

        @field:NotBlank
        var click: String = ""
    }
}
