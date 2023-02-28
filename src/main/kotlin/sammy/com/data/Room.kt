package sammy.com.data

import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.isActive

class Room(
    val name: String,
    val maxPlayers: Int,
    var players: List<Player> = listOf()
) {
    suspend fun broadcast(message: String) {
        players.forEach {
            if (it.socket.isActive) {
                it.socket.send(Frame.Text(message))
            }
        }
    }

    suspend fun broadcastToAllExcept(message: String, clientId: String) {
        players.forEach {
            if (it.clientId != clientId && it.socket.isActive) {
                it.socket.send(Frame.Text(message))
            }
        }
    }
}