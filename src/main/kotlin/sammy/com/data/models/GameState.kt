package sammy.com.data.models

import sammy.com.util.Constants

data class GameState(
    val drawingPlayer:String,
    val word:String
):BaseModel(Constants.TYPE_GAME_STATE)
