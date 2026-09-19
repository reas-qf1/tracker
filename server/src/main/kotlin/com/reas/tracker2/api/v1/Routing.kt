package com.reas.tracker2.api.v1

import com.reas.tracker2.UserPrincipal
import com.reas.tracker2.wrap
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import kotlin.time.Instant

private fun RoutingCall.usernameFromJwt(): String = principal<JWTPrincipal>()!!.payload.getClaim("username").asString()

fun Route.apiV1() {
    val api: ApiV1Impl by inject()
    api.setJwtParameters(
        environment.config.property("jwt.secret").getString(),
        environment.config.property("jwt.issuer").getString(),
        environment.config.property("jwt.audience").getString(),
    )

    post("/register") {
        wrap {
            val (username, password) = call.receive<V1LoginInfo>()
            api.register(username, password)
        }
    }
    post("/login") {
        wrap {
            val (username, password) = call.receive<V1LoginInfo>()
            api.login(username, password)
        }
    }

    authenticate("auth-jwt") {
        route("/clients") {
            get {
                wrap {
                    val username = call.usernameFromJwt()
                    api.getClients(username)
                }
            }
            post("/{name}") {
                wrap {
                    val username = call.usernameFromJwt()
                    val clientName = call.requirePathParameter("name")
                    api.addClient(username, clientName)
                }
            }
            delete("/{name}") {
                wrap {
                    val username = call.usernameFromJwt()
                    val clientName = call.requirePathParameter("name")
                    api.deleteClient(username, clientName)
                }
            }
        }
        route("/tokens/{clientName}") {
            get {
                wrap {
                    val username = call.usernameFromJwt()
                    val clientName = call.requirePathParameter("clientName")
                    api.getTokens(username, clientName)
                }
            }
            post {
                wrap {
                    val username = call.usernameFromJwt()
                    val clientName = call.requirePathParameter("clientName")
                    val expiresAt =
                        call.request.queryParameters["expiresAt"]
                            ?.toLongOrNull()
                            ?.let { Instant.fromEpochSeconds(it, 0) }
                    api.addToken(username, clientName, expiresAt)
                }
            }
            delete {
                wrap {
                    val username = call.usernameFromJwt()
                    val clientName = call.requirePathParameter("clientName")
                    api.deleteAllTokens(username, clientName)
                }
            }
        }
        delete("/token") {
            wrap {
                val username = call.usernameFromJwt()
                val token = call.receiveText()
                api.deleteToken(username, token)
            }
        }
        delete("/tokenHash") {
            wrap {
                val username = call.usernameFromJwt()
                val token = call.receiveText().hexToByteArray()
                api.deleteTokenByHash(username, token)
            }
        }
    }

    authenticate("api-token") {
        get("/test") {
            wrap {
                val principal = call.principal<UserPrincipal>()!!
                api.test(principal)
            }
        }
    }
}
