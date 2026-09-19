package com.reas.tracker2.database

import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable

object UserTable : UUIDTable("users") {
    val name = text("name").uniqueIndex()
    val passwordHash = text("password_hash")
}

object ClientTable : UUIDTable("clients") {
    val user = reference("user", UserTable.id)
    val name = text("name").index()

    val index = uniqueIndex(user, name)
}

object TokenTable : UUIDTable("tokens") {
    val user = reference("user", UserTable.id)
    val client = reference("client", ClientTable.id)
    val tokenPrefix = varchar("token_prefix", 8)
    val tokenHash = binary("token_hash", 32)
    val createdAt = long("created_at")
    val expiresAt = long("expires_at")
}
