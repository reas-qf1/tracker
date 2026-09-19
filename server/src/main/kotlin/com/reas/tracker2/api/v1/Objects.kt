package com.reas.tracker2.api.v1

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class V1LoginInfo(
    val username: String,
    val password: String,
)

@Serializable
data class V1TokenInfo(
    val prefix: String,
    val hash: String,
    val createdAt: Instant,
    val expiresAt: Instant,
)
