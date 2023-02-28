package sammy.com.data.models


import sammy.com.util.Constants

data class NewWords(val newWords:List<String>):BaseModel(Constants.TYPE_NEW_WORDS)