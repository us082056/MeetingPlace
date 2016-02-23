package controllers

import models.Station
import models.InputForm
import play.api.data._
import play.api.data.Forms._
import play.api.mvc.Action
import play.api.mvc.Controller
import servicies.LonLatCalculator
import util.StationsManager
import play.api.Play.current
import play.api.i18n.Messages.Implicits._ //・・・TODO おまじない
import models.Station
import play.Logger

class SearchController extends Controller {
  val inputForm = Form(
    mapping("station-name" -> list(text))(InputForm.apply)(InputForm.unapply))

  def init = Action {
    Ok(views.html.index())
  }

  //requestをimplicitで宣言することで、暗黙的にリクエストを引数として受け取る
  def search = Action { implicit request =>
    //TODO バリデーション
    var inputStations = inputForm.bindFromRequest.get.names.map(x => new Station(x, "", null))

    //経緯度取得
    // TODO webじゃなくてCSVのデータだけで完結できる
    // TODO 複数ヒットした時の仕組み必要
    for (station <- inputStations) {
      Logger.debug(station.name)
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
    Ok(views.html.result(nearStationsName(0)))
  }
}