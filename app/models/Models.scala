package models

/** 駅情報 */
case class LonLat(lon: Double, lat: Double)
case class Station(var name: String, var code: String, var lonLat: LonLat)