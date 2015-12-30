package controllers

import models.Station
import models.StationForm
import play.api.data._
import play.api.data.Forms._
import play.api.mvc.Action
import play.api.mvc.Controller
import servicies.LonLatCalculator
import util.StationsManager
import play.api.Play.current //・・・TODO　おまじない
import play.api.i18n.Messages.Implicits._ //・・・TODO おまじない

class SearchController extends Controller {
  val stationForm = Form(
    mapping("name" -> text)(StationForm.apply)(StationForm.unapply))

  def init = Action {
    val inputForm = stationForm.fill(StationForm("user name"))
    Ok(views.html.index(inputForm))
  }

  def search = Action {
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
    //TODO 画面出力する(仮）
    val inputForm = stationForm.fill(StationForm("user name"))
    Ok(views.html.index(inputForm))
  }
}