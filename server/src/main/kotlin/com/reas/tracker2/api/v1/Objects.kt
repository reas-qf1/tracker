package com.reas.tracker2.api.v1

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class LoginInfoV1(
    val username: String,
    val password: String,
)

@Serializable
data class TokenInfoV1(
    val prefix: String,
    val hash: String,
    val createdAt: Instant,
    val expiresAt: Instant,
)
