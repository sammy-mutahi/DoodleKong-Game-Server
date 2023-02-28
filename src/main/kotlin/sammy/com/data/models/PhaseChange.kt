package sammy.com.data.models

import sammy.com.Constants
import sammy.com.data.Room

data class PhaseChange(
    var phase: Room.Phase?,
    var time:Long,
    var drawingPlayer:String = ""

):BaseModel(Constants.TYPE_PHASE_CHANGED)