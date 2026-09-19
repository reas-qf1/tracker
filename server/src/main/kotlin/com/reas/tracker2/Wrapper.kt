package com.reas.tracker2

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

open class StatusCodeException(
    val status: HttpStatusCode,
    message: String? = null,
) : Exception(message)

open class Conflict(
    message: String? = null,
) : StatusCodeException(HttpStatusCode.Conflict, message)

open class Unauthorized(
    message: String? = null,
) : StatusCodeException(HttpStatusCode.Unauthorized, message)

suspend inline fun RoutingContext.wrap(block: RoutingContext.() -> Any?) {
    try {
        val response = block()
        if (response is StatusCodeException) {
            call.respond(response.status, response.message.orEmpty())
        } else if (response == null) {
            call.respond(HttpStatusCode.OK)
        } else {
            call.respond(HttpStatusCode.OK, response)
        }
    } catch (e: StatusCodeException) {
        call.respond(e.status, e.message.orEmpty())
    }
}
