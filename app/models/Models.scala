package models

case class InputForm(val names: List[String])
case class LonLat(val lon: Double, val lat: Double)
case class Station(val name: String, val code: String, val lonLat: LonLat)
case class CandidateStation(val name: String, val line: String)
