package sammy.com.data.models

import sammy.com.util.Constants

data class PlayersList(
    val players: List<PlayerData>
) : BaseModel(Constants.TYPE_PLAYERS_LIST)
