package sammy.com

import io.ktor.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import sammy.com.plugins.*

val server = DrawingServer()
fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureSessions()
    configureSerialization()
    configureSockets()
    configureMonitoring()
    configureRouting()
}
