package sammy.com.data.models

import sammy.com.Constants

data class JoinRoomHandshake(
    val username: String,
    val roomName: String,
    val clientId: String
) : BaseModel(Constants.TYPE_JOIN_ROOM_HANDSHAKE)
