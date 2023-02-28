package sammy.com.plugins

import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import sammy.com.routes.createRoomRoute
import sammy.com.routes.gameWebSocketRoute
import sammy.com.routes.getRoomsRoute
import sammy.com.routes.joinRoomRoute

fun Application.configureRouting() {
    install(Routing) {
        createRoomRoute()
        getRoomsRoute()
        joinRoomRoute()
        gameWebSocketRoute()
    }
}
