package sammy.com.data.models

import sammy.com.util.Constants

data class ChosenWord(
    val choosenWord:String,
    val roomName:String
):BaseModel(Constants.TYPE_CHOOSEN_WORD)
