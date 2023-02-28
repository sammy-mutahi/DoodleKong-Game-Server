package sammy.com.plugins

import io.ktor.application.*
import io.ktor.sessions.*
import io.ktor.util.*
import sammy.com.session.DrawingSession

/*
* Attach a session using a cookie
* Intercept a session whenever a client makes a request, check if there is already a session if not set a new one by attaching a client id
* */

fun Application.configureSessions(){
    install(Sessions){
        cookie<DrawingSession>("SESSION") //attach a session
    }
    intercept(ApplicationCallPipeline.Features){ //gets called whenever a client makes a request
        if (call.sessions.get<DrawingSession>() == null){
            val cliendId = call.parameters["client_id"] ?: ""
            call.sessions.set(DrawingSession(cliendId, generateNonce()))
        }
    }
}