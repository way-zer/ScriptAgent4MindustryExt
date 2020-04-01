package coreLibrary.lib

import cf.wayzer.placehold.PlaceHoldApi
import cf.wayzer.placehold.PlaceHoldContext

val PlaceHoldApi = PlaceHoldApi
typealias PlaceHoldString = PlaceHoldContext

fun String.with(vararg arg:Pair<String,Any>):PlaceHoldString = PlaceHoldApi.getContext(this,arg.toMap())