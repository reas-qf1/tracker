package com.reas.tracker2.api.v1.primitives

import kotlinx.serialization.Serializable

@Serializable
data class IDObjectV1(
    val id: Long,
    val name: String,
)
