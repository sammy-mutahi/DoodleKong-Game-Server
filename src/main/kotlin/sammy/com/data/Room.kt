package sammy.com.data

import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.*
import sammy.com.data.models.Announcement
import sammy.com.data.models.ChoosenWord
import sammy.com.data.models.GameState
import sammy.com.data.models.PhaseChange
import sammy.com.gson
import sammy.com.util.transformToUnderScores
import sammy.com.util.words

class Room(
    val name: String,
    val maxPlayers: Int,
    var players: List<Player> = listOf()
) {

    private var phaseChangedListener: ((Phase) -> Unit)? = null
    private var timerJob: Job? = null
    private var drawingPlayer: Player? = null
    private var winningPlayers = listOf<String>()
    private var word: String? = null
    private var curWords: List<String>? = null
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

    suspend fun addPlayer(clientId: String, username: String, socket: WebSocketSession): Player {
        val player = Player(username, socket, clientId)
        players = players + player
        if (players.size == 1) {
            phase = Phase.WAITING_FOR_PLAYERS
        } else if (players.size == 2 && phase == Phase.WAITING_FOR_PLAYERS) {
            phase = Phase.WAITING_FOR_START
            players = players.shuffled()
        } else if (phase == Phase.WAITING_FOR_START && players.size == maxPlayers) {
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

    private fun timeAndNotify(ms: Long) {
        timerJob?.cancel()
        timerJob = GlobalScope.launch {
            val phaseChange = PhaseChange(
                phase,
                ms,
                drawingPlayer?.username ?: ""
            )
            repeat((ms / UPDATE_TIME_FREQUENCY).toInt()) {
                if (it != 0) {
                    phaseChange.phase = null
                }
                broadcast(gson.toJson(phaseChange))
                phaseChange.time -= UPDATE_TIME_FREQUENCY
                delay(UPDATE_TIME_FREQUENCY)
            }
            phase = when (phase) {
                Phase.WAITING_FOR_START -> Phase.NEW_ROUND
                Phase.GAME_RUNNING -> Phase.SHOW_WORD
                Phase.SHOW_WORD -> Phase.NEW_ROUND
                Phase.NEW_ROUND -> Phase.GAME_RUNNING
                else -> Phase.WAITING_FOR_PLAYERS
            }
        }
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

    fun setWordAndSwitchGameRunning(word: String) {
        this.word = word
        phase = Phase.GAME_RUNNING
    }

    private fun waitingForPlayers() {
        GlobalScope.launch {
            val phaseChange = PhaseChange(
                Phase.WAITING_FOR_PLAYERS,
                DELAY_WAITING_FOR_START_TO_NEW_ROUND
            )
            broadcast(gson.toJson(phaseChange))
        }
    }

    private fun waitingForStart() {
        timeAndNotify(DELAY_WAITING_FOR_START_TO_NEW_ROUND)
        GlobalScope.launch {
            val phaseChange = PhaseChange(
                Phase.WAITING_FOR_PLAYERS,
                DELAY_WAITING_FOR_START_TO_NEW_ROUND
            )
            broadcast(gson.toJson(phaseChange))
        }
    }

    private fun newRound() {

    }

    private fun gameRunning() {
        winningPlayers = listOf()
        val wordsToSend = word ?: curWords?.random() ?: words.random()
        val wordWithUnderscores = wordsToSend.transformToUnderScores()
        val drawingUsername = (drawingPlayer ?: players.random()).username
        val gameStateForDrawingPlayer = GameState(
            drawingUsername,
            wordsToSend
        )
        val gameStateForGuessingPlayers = GameState(
            drawingUsername,
            wordWithUnderscores
        )
        GlobalScope.launch {
            broadcastToAllExcept(
                gson.toJson(gameStateForGuessingPlayers),
                drawingPlayer?.clientId ?: players.random().clientId
            )
            drawingPlayer?.socket?.send(Frame.Text(gson.toJson(gameStateForDrawingPlayer)))
            timeAndNotify(DELAY_GAME_RUNNING_TO_SHOW_WORD)
            println("Drawing phase in room $name started. It'LL last ${DELAY_GAME_RUNNING_TO_SHOW_WORD / 1000}s")
        }
    }

    private fun showWord() {
        GlobalScope.launch {
            if (winningPlayers.isEmpty()) {
                drawingPlayer?.let {
                    it.score -= PENALTY_NOBODY_GUESSED_IT
                }
            }
            word?.let {
                val choosenWord = ChoosenWord(
                    it,
                    name
                )
                broadcast(gson.toJson(choosenWord))
            }
            timeAndNotify(DELAY_SHOW_WORD_TO_NEW_ROUND)
            val phaseChange = PhaseChange(
                Phase.SHOW_WORD, DELAY_SHOW_WORD_TO_NEW_ROUND
            )
            broadcast(gson.toJson(phaseChange))
        }
    }

    enum class Phase {
        WAITING_FOR_PLAYERS,
        WAITING_FOR_START,
        NEW_ROUND,
        GAME_RUNNING,
        SHOW_WORD
    }

    companion object {
        const val UPDATE_TIME_FREQUENCY = 1000L
        const val DELAY_WAITING_FOR_START_TO_NEW_ROUND = 10000L
        const val DELAY_NEW_ROUND_TO_GAME_RUNNING = 20000L
        const val DELAY_GAME_RUNNING_TO_SHOW_WORD = 60000L
        const val DELAY_SHOW_WORD_TO_NEW_ROUND = 10000L
        const val PENALTY_NOBODY_GUESSED_IT = 50
    }
}