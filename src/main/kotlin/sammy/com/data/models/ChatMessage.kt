package sammy.com.data.models

import sammy.com.Constants

data class ChatMessage(
    val from: String,
    val roomName: String,
    val message: String,
    val timestamp: Long
) : BaseModel(Constants.TYPE_CHAT_MESSAGE)
