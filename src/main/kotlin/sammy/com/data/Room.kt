package sammy.com.data

import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.isActive
import sammy.com.data.models.Announcement
import sammy.com.gson

class Room(
    val name: String,
    val maxPlayers: Int,
    var players: List<Player> = listOf()
) {

    private var phaseChangedListener: ((Phase) -> Unit)? = null
    var phase = Phase.WAITING_FOR_PLAYERS
        set(value) {
            synchronized(field) {
                field = value
                phaseChangedListener?.let {
                    it.invoke(value)
                }
            }
        }

    private fun setPhaseChangedListener(listener: (Phase) -> Unit) {
        phaseChangedListener = listener
    }

    init {
        setPhaseChangedListener { newPhase ->
            when (newPhase) {
                Phase.WAITING_FOR_PLAYERS -> waitingForPlayers()
                Phase.WAITING_FOR_START -> waitingForStart()
                Phase.NEW_ROUND -> newRound()
                Phase.GAME_RUNNING -> gameRunning()
                Phase.SHOW_WORD -> showWord()
            }
        }
    }

    suspend fun addPlayer(clientId: String,username: String,socket:WebSocketSession):Player{
        val player = Player(username,socket,clientId)
        players = players + player
        if (players.size == 1){
            phase = Phase.WAITING_FOR_PLAYERS
        }else if(players.size == 2 && phase == Phase.WAITING_FOR_PLAYERS){
            phase = Phase.WAITING_FOR_START
            players = players.shuffled()
        }else if (phase == Phase.WAITING_FOR_START && players.size == maxPlayers){
            phase = Phase.NEW_ROUND
            players = players.shuffled()
        }

        val announcement = Announcement(
            message = "$username joined the party!",
            timestamp = System.currentTimeMillis(),
            announcementType = Announcement.TYPE_PLAYER_JOINED
        )

        broadcast(gson.toJson(announcement))

        return player
    }

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

    fun containsPlayer(username: String): Boolean {
        return players.find { it.username == username } != null
    }

    private fun waitingForPlayers() {

    }

    private fun waitingForStart() {

    }

    private fun newRound() {

    }

    private fun gameRunning() {

    }

    private fun showWord() {

    }

    enum class Phase {
        WAITING_FOR_PLAYERS,
        WAITING_FOR_START,
        NEW_ROUND,
        GAME_RUNNING,
        SHOW_WORD
    }
}