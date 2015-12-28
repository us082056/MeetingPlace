package controllers

import models.Station
import play.api.mvc.Action
import play.api.mvc.Controller
import servicies.LonLatCalculator
import util.StationsManager

class SearchController extends Controller {
  def index = Action {
    //TODO バリデーション

    //駅名取得（入力値取得）
    var inputStations: List[Station] = null //・・・（仮） 
    //TODO 画面から入力値を取得し、リストに詰める

    //経緯度取得
    for (station <- inputStations) {
      //駅名を駅コードに変換する
      station.code = StationsManager.nameToCode(station.name)

      //webAPIで駅コードの経緯度取得する
      station.lonLat = StationsManager.codeToLonLat(station.code)
    }

    //中心経緯度計算
    val centerLonLat = new LonLatCalculator().calcCenterLonLat(inputStations)

    //最寄駅取得
    val nearStationsName = StationsManager.getNearStationsName(centerLonLat)

    //出力
    //TODO 画面出力する
    Ok(views.html.index("Your new application is ready.")) //・・・（仮）
  }
}