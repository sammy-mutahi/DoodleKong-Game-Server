package sammy.com.data.models

import sammy.com.util.Constants

data class GameError(
    val errorType:Int
):BaseModel(Constants.TYPE_GAME_ERROR){
    companion object{
        const val ERROR_ROOM_NOT_FOUND = 0
    }
}
