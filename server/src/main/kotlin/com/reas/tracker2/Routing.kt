package com.reas.tracker2

import com.reas.tracker2.api.v1.apiV1
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        singlePageApplication {
            react("server/webApp/dist")
        }

        route("/api") {
            route("/v1") {
                apiV1()
            }
        }
    }
}
