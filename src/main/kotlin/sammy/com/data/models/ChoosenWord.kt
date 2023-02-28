package sammy.com.data.models

import sammy.com.Constants

data class ChoosenWord(
    val choosenWord:String,
    val roomName:String
):BaseModel(Constants.TYPE_CHOOSEN_WORD)
