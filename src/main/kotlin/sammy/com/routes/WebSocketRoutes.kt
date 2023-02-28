package sammy.com.routes

import com.google.gson.JsonParser
import io.ktor.http.cio.websocket.*
import io.ktor.routing.*
import io.ktor.sessions.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import sammy.com.util.Constants
import sammy.com.util.Constants.TYPE_ANNOUNCEMENT
import sammy.com.util.Constants.TYPE_CHAT_MESSAGE
import sammy.com.util.Constants.TYPE_DRAW_DATA
import sammy.com.data.Player
import sammy.com.data.Room
import sammy.com.data.models.*
import sammy.com.gson
import sammy.com.server
import sammy.com.session.DrawingSession
import java.lang.Exception


fun Route.gameWebSocketRoute() {
    route("/ws/draw") {
        standardWebSocket { socket, clientId, message, payload ->
            when (payload) {
                is JoinRoomHandshake -> { //comes from a client
                    val room = server.rooms[payload.roomName]
                    if (room == null) {
                        val gameError = GameError(GameError.ERROR_ROOM_NOT_FOUND)
                        socket.send(Frame.Text(gson.toJson(gameError)))
                        return@standardWebSocket
                    }
                    val player = Player(
                        payload.username,
                        socket,
                        payload.clientId
                    )
                    server.playerJoined(player)
                    if (!room.containsPlayer(player.username)) {
                        room.addPlayer(player.clientId, player.username, socket)
                    } else {
                        val playerInRoom = room.players.find { it.clientId == clientId }
                        playerInRoom?.socket = socket
                        playerInRoom?.startPinging()
                    }
                }

                is DrawData -> {
                    val room = server.rooms[payload.roomName] ?: return@standardWebSocket
                    if (room.phase == Room.Phase.GAME_RUNNING) {
                        room.broadcastToAllExcept(message, clientId)
                        room.addSerializedDrawAction(message)
                    }
                    room.lastDrawData = payload

                }

                is ChosenWord -> {
                    val room = server.rooms[payload.roomName] ?: return@standardWebSocket
                    room.setWordAndSwitchGameRunning(payload.choosenWord)
                }

                is ChatMessage -> {
                    val room = server.rooms[payload.roomName] ?: return@standardWebSocket
                    if (!room.checkWordAndNotifyPlayers(payload)) {
                        room.broadcast(message)
                    }
                }

                is Ping -> {
                    server.players[clientId]?.receivedPong()
                }

                is DisconnectRequest -> {
                    server.playerLeft(clientId, true)
                }

                is DrawAction -> {
                    val room = server.getRoomWithClientId(clientId) ?: return@standardWebSocket
                    room.broadcastToAllExcept(message, clientId)
                    room.addSerializedDrawAction(message)
                }
            }
        }
    }
}

fun Route.standardWebSocket(
    handleFrame: suspend (
        socket: DefaultWebSocketServerSession,
        clientId: String,
        message: String,
        payload: BaseModel
    ) -> Unit
) {
    webSocket {
        val session = call.sessions.get<DrawingSession>()
        if (session == null) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No Session"))
            return@webSocket
        }
        try {
            incoming.consumeEach { frame ->
                if (frame is Frame.Text) {
                    val message = frame.readText()
                    val jsonObject = JsonParser.parseString(message).asJsonObject
                    val type = when (jsonObject.get("type").asString) {
                        TYPE_CHAT_MESSAGE -> ChatMessage::class.java
                        TYPE_DRAW_DATA -> DrawData::class.java
                        TYPE_ANNOUNCEMENT -> Announcement::class.java
                        Constants.TYPE_JOIN_ROOM_HANDSHAKE -> JoinRoomHandshake::class.java
                        Constants.TYPE_PHASE_CHANGED -> PhaseChange::class.java
                        Constants.TYPE_CHOOSEN_WORD -> ChosenWord::class.java
                        Constants.TYPE_GAME_STATE -> GameState::class.java
                        Constants.TYPE_PING -> Ping::class.java
                        Constants.TYPE_DISCONNECT_REQUEST -> DisconnectRequest::class.java
                        Constants.TYPE_DRAW_ACTION -> DrawAction::class.java
                        else -> BaseModel::class.java
                    }
                    val payload = gson.fromJson(message, type)
                    handleFrame.invoke(this, session.clientId, message, payload)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            //handle disconnects
            val playerWithClientId = server.getRoomWithClientId(session.clientId)?.players?.find {
                it.clientId == session.clientId
            }
            if (playerWithClientId != null) {
                server.playerLeft(session.clientId)
            }
        }
    }
}