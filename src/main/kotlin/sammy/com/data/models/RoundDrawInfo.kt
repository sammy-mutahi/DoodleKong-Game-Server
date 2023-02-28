package sammy.com.data.models

import sammy.com.util.Constants

data class RoundDrawInfo(
    val data:List<String>
):BaseModel(Constants.TYPE_CUR_ROUND_DRAW_INFO)