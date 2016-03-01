package models

case class InputForm(var names: List[String])
case class LonLat(var lon: Double, var lat: Double)
case class Station(var name: String, var code: String, var lonLat: LonLat)