package models

/** 駅情報 */
case class StationForm(var name: String)
case class LonLat(var lon: Double, var lat: Double)
case class Station(var name: String, var code: String, var lonLat: LonLat)