package com.reas.tracker2

import com.auth0.jwt.algorithms.Algorithm
import com.reas.tracker2.database.AuthRepository
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.websocket.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import kotlin.time.Duration.Companion.seconds

fun main(args: Array<String>) {
    EngineMain.main(args)
}

@Serializable
data class UserPrincipal(
    val username: String,
    val clientName: String,
)

fun Application.installPlugins() {
    val authRepository: AuthRepository by inject()

    install(Koin) {
        slf4jLogger()
        modules(module)
    }
    install(ContentNegotiation) {
        json()
    }
    install(WebSockets) {
        pingPeriod = 15.seconds
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
    }
    install(Authentication) {
        jwt("auth-jwt") {
            val secret =
                this@installPlugins
                    .environment.config
                    .property("jwt.secret")
                    .getString()
            val issuer =
                this@installPlugins
                    .environment.config
                    .property("jwt.issuer")
                    .getString()
            val audience =
                this@installPlugins
                    .environment.config
                    .property("jwt.audience")
                    .getString()
            val realm_ =
                this@installPlugins
                    .environment.config
                    .property("jwt.realm")
                    .getString()

            realm = realm_
            verifier(issuer, audience, Algorithm.HMAC256(secret))

            validate { credential ->
                if (authRepository.userExists(credential.payload.getClaim("username").asString())) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }

            challenge { defaultScheme, realm ->
                call.respond(HttpStatusCode.Unauthorized, "Token is not valid or has expired")
            }
        }

        bearer("api-token") {
            authenticate { token ->
                try {
                    val (userId, clientId) = authRepository.validateToken(token.token)
                    UserPrincipal(userId, clientId)
                } catch (e: AuthRepository.TokenValidationException) {
                    null
                }
            }
        }
    }
}
