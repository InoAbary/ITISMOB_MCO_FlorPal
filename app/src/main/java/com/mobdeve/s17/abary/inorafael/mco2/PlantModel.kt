package com.mobdeve.s17.abary.inorafael.mco2
import java.io.Serializable // added nov 5

class PlantModel (
    var plantNickName : String,
    var plantName : String,
    var plantPhoto : String,
    var fruitProductionRate : String? = null, // edited Nov 5
    var flowerColor : String,
    var dateCreated : CustomDate,
    var wateredDate : CustomDate,
    var wateringAmount: Double? = null, // added Nov 5
    var location: String? = null,// added Nov 5
    var favorite: Boolean = false,
    var nextWateredDate : CustomDate? = null,
    var plant_id: String? = ""
): Serializable // added nov 5
