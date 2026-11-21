package com.reas.tracker2.api.v1

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.reas.tracker2.Conflict
import com.reas.tracker2.Unauthorized
import com.reas.tracker2.UserPrincipal
import com.reas.tracker2.api.v1.primitives.EventV1
import com.reas.tracker2.database.AuthRepository
import com.reas.tracker2.internalsServer.LocalServer
import com.reas.tracker2.shared.Scrobble
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*
import kotlin.time.Instant

class ApiV1Impl : KoinComponent {
    val authRepository: AuthRepository by inject()
    val server: LocalServer by inject()

    lateinit var jwtSecret: String
    lateinit var jwtIssuer: String
    lateinit var jwtAudience: String

    fun setJwtParameters(secret: String, issuer: String, audience: String) {
        jwtSecret = secret
        jwtIssuer = issuer
        jwtAudience = audience
    }

    fun register(username: String, password: String) {
        try {
            authRepository.addUser(username, password)
        } catch (e: AuthRepository.UserAlreadyExistsException) {
            throw Conflict(e.message)
        }
    }

    fun login(username: String, password: String): String {
        if (!authRepository.validate(username, password)) {
            throw Unauthorized("username or password are incorrect")
        }

        val token =
            JWT
                .create()
                .withAudience(jwtAudience)
                .withIssuer(jwtIssuer)
                .withClaim("username", username)
                .withExpiresAt(Date(System.currentTimeMillis() + 36000000))
                .sign(Algorithm.HMAC256(jwtSecret))
        return token
    }

    fun getClients(username: String): List<String> = authRepository.getClients(username)

    fun addClient(username: String, clientName: String) {
        authRepository.addClient(username, clientName)
    }

    fun deleteClient(username: String, clientName: String) {
        authRepository.deleteClient(username, clientName)
    }

    fun getTokens(username: String, clientName: String): List<TokenInfoV1> {
        val tokens = authRepository.getTokens(username, clientName)
        return tokens.map {
            TokenInfoV1(it.prefix, it.hash.toHexString(), it.createdAt, it.expiresAt)
        }
    }

    fun addToken(username: String, clientName: String, expiresAt: Instant?): String =
        authRepository.addToken(username, clientName, expiresAt ?: Instant.DISTANT_FUTURE)

    fun deleteAllTokens(username: String, clientName: String) {
        authRepository.deleteAllTokens(username, clientName)
    }

    fun deleteToken(username: String, token: String) {
        authRepository.deleteToken(username, token)
    }

    fun deleteTokenByHash(username: String, hash: ByteArray) {
        authRepository.deleteTokenByHash(username, hash)
    }

    suspend fun submit(events: List<EventV1>, user: UserPrincipal): List<Scrobble> =
        server.submit(events.map { it.toEvent(user.username, user.clientName) })
}
