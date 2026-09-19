package com.reas.tracker2.database

import at.favre.lib.crypto.bcrypt.BCrypt
import com.reas.tracker2.database.TokenTable.client
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*
import kotlin.io.encoding.Base64
import kotlin.time.Clock
import kotlin.time.Instant

data class Token(
    val prefix: String,
    val hash: ByteArray,
    val createdAt: Instant,
    val expiresAt: Instant,
)

interface AuthRepository {
    fun addUser(
        username: String,
        password: String,
    )

    fun validate(
        username: String,
        password: String,
    ): Boolean

    fun userExists(username: String): Boolean

    fun deleteUser(username: String)

    fun addClient(
        username: String,
        clientName: String,
    )

    fun getClients(username: String): List<String>

    fun clientExists(
        username: String,
        clientName: String,
    ): Boolean

    fun deleteClient(
        username: String,
        clientName: String,
    )

    fun deleteAllClients(username: String)

    fun addToken(
        username: String,
        clientName: String,
        expiresAt: Instant,
    ): String

    fun getTokens(
        username: String,
        clientName: String,
    ): List<Token>

    fun validateToken(token: String): Pair<String, String>

    fun deleteToken(
        username: String,
        token: String,
    )

    fun deleteTokenByHash(
        username: String,
        tokenHash: ByteArray,
    )

    fun deleteAllTokens(
        username: String,
        clientName: String,
    )

    fun deleteAllTokens(username: String)

    class UserNotFoundException(
        val user: String,
    ) : RuntimeException() {
        override val message: String = "User $user not found"
    }

    class UserAlreadyExistsException(
        val user: String,
    ) : RuntimeException() {
        override val message: String = "User $user already exists"
    }

    class ClientNotFoundException(
        val clientName: String,
    ) : RuntimeException() {
        override val message: String = "Client $client not found"
    }

    class TokenValidationException : RuntimeException() {
        override val message: String = "Invalid token"
    }
}

class DatabaseAuthRepository(
    private val db: Database,
) : AuthRepository {
    private val bcrypt = BCrypt.withDefaults()
    private val verifier = BCrypt.verifyer()
    private val random = SecureRandom()

    override fun addUser(
        username: String,
        password: String,
    ) {
        try {
            transaction(db) {
                val passwordHash = bcrypt.hashToString(12, password.toCharArray())
                UserTable.insert {
                    it[UserTable.name] = username
                    it[UserTable.passwordHash] = passwordHash
                }
            }
        } catch (e: ExposedSQLException) {
            throw AuthRepository.UserAlreadyExistsException(username)
        }
    }

    override fun validate(
        username: String,
        password: String,
    ): Boolean =
        transaction(db) {
            val passwordHash =
                UserTable
                    .select(UserTable.passwordHash)
                    .where(UserTable.name eq username)
                    .singleOrNull()
                    ?.get(UserTable.passwordHash) ?: return@transaction false
            verifier.verify(password.toCharArray(), passwordHash.toCharArray()).verified
        }

    override fun userExists(username: String): Boolean =
        transaction(db) {
            !UserTable.select(UserTable.id).where { UserTable.name eq username }.empty()
        }

    override fun deleteUser(username: String) {
        transaction(db) {
            val userId = userId(username)
            UserTable.deleteWhere { UserTable.id eq userId }
            ClientTable.deleteWhere { ClientTable.user eq userId }
            TokenTable.deleteWhere { TokenTable.user eq userId }
        }
    }

    override fun addClient(
        username: String,
        clientName: String,
    ) {
        transaction(db) {
            val userId = userId(username)
            ClientTable.insert {
                it[ClientTable.name] = clientName
                it[ClientTable.user] = userId
            }
        }
    }

    override fun getClients(username: String): List<String> =
        transaction(db) {
            val userId = userId(username)
            return@transaction ClientTable
                .select(ClientTable.name)
                .where { ClientTable.user eq userId }
                .map { it[ClientTable.name] }
        }

    override fun clientExists(
        username: String,
        clientName: String,
    ): Boolean =
        transaction(db) {
            val userId = userId(username)
            ClientTable
                .selectAll()
                .where {
                    (ClientTable.name eq clientName) and (ClientTable.user eq userId)
                }.singleOrNull() != null
        }

    override fun deleteClient(
        username: String,
        clientName: String,
    ) {
        transaction(db) {
            val userId = userId(username)
            val clientId = clientId(userId, clientName)
            TokenTable.deleteWhere {
                (TokenTable.client eq clientId) and (TokenTable.user eq userId)
            }
            ClientTable.deleteWhere {
                (ClientTable.name eq clientName) and (ClientTable.user eq userId)
            }
        }
    }

    override fun deleteAllClients(username: String) {
        transaction(db) {
            val userId = userId(username)
            TokenTable.deleteWhere { TokenTable.user eq userId }
            ClientTable.deleteWhere { ClientTable.user eq userId }
        }
    }

    override fun addToken(
        username: String,
        clientName: String,
        expiresAt: Instant,
    ): String =
        transaction(db) {
            val userId = userId(username)
            val clientId = clientId(userId, clientName)

            val bytes = ByteArray(32)
            random.nextBytes(bytes)
            val token = Base64.UrlSafe.encode(bytes)
            val tokenHash = tokenHash(token)

            TokenTable.insert {
                it[TokenTable.user] = userId
                it[TokenTable.client] = clientId
                it[TokenTable.tokenPrefix] = token.take(8)
                it[TokenTable.tokenHash] = tokenHash
                it[TokenTable.createdAt] = Clock.System.now().toEpochMilliseconds()
                it[TokenTable.expiresAt] = expiresAt.toEpochMilliseconds()
            }
            return@transaction token
        }

    override fun validateToken(token: String): Pair<String, String> =
        transaction(db) {
            val tokenHash = tokenHash(token)
            val tokenObj =
                TokenTable
                    .selectAll()
                    .where { TokenTable.tokenHash eq tokenHash }
                    .singleOrNull() ?: throw AuthRepository.TokenValidationException()

            val expiresAt = Instant.fromEpochMilliseconds(tokenObj[TokenTable.expiresAt])
            if (Clock.System.now() > expiresAt) {
                throw AuthRepository.TokenValidationException()
            }

            val userId = tokenObj[TokenTable.user]
            val username =
                UserTable
                    .select(UserTable.name)
                    .where { UserTable.id eq userId }
                    .singleOrNull()
                    ?.get(UserTable.name) ?: throw AuthRepository.TokenValidationException()

            val clientId = tokenObj[TokenTable.client]
            val clientName =
                ClientTable
                    .select(ClientTable.name)
                    .where { ClientTable.id eq clientId }
                    .singleOrNull()
                    ?.get(ClientTable.name) ?: throw AuthRepository.TokenValidationException()

            return@transaction username to clientName
        }

    override fun getTokens(
        username: String,
        clientName: String,
    ): List<Token> =
        transaction(db) {
            val userId = userId(username)
            val clientId = clientId(userId, clientName)
            return@transaction TokenTable
                .select(TokenTable.tokenPrefix, TokenTable.tokenHash, TokenTable.createdAt, TokenTable.expiresAt)
                .where { TokenTable.user eq userId }
                .andWhere { TokenTable.client eq clientId }
                .map {
                    Token(
                        it[TokenTable.tokenPrefix],
                        it[TokenTable.tokenHash],
                        Instant.fromEpochMilliseconds(it[TokenTable.createdAt]),
                        Instant.fromEpochMilliseconds(it[TokenTable.expiresAt]),
                    )
                }
        }

    override fun deleteToken(
        username: String,
        token: String,
    ) {
        deleteTokenByHash(username, tokenHash(token))
    }

    override fun deleteTokenByHash(
        username: String,
        tokenHash: ByteArray,
    ) {
        transaction(db) {
            val userId = userId(username)
            TokenTable.deleteWhere {
                (TokenTable.tokenHash eq tokenHash) and (TokenTable.user eq userId)
            }
        }
    }

    override fun deleteAllTokens(
        username: String,
        clientName: String,
    ) {
        transaction(db) {
            val userId = userId(username)
            val clientId = clientId(userId, clientName)
            TokenTable.deleteWhere {
                (TokenTable.user eq userId) and (TokenTable.client eq clientId)
            }
        }
    }

    override fun deleteAllTokens(username: String) {
        transaction(db) {
            val userId = userId(username)
            TokenTable.deleteWhere { TokenTable.user eq userId }
        }
    }

    private fun userId(username: String): EntityID<UUID> =
        UserTable
            .select(UserTable.id)
            .where { UserTable.name eq username }
            .singleOrNull()
            ?.get(UserTable.id) ?: throw AuthRepository.UserNotFoundException(username)

    private fun clientId(
        userId: EntityID<UUID>,
        clientName: String,
    ): EntityID<UUID> =
        ClientTable
            .select(ClientTable.id)
            .where { ClientTable.user eq userId }
            .andWhere { ClientTable.name eq clientName }
            .singleOrNull()
            ?.get(ClientTable.id) ?: throw AuthRepository.ClientNotFoundException(clientName)

    private fun tokenHash(password: String) = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
}
