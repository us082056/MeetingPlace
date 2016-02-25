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
import play.api.i18n.Messages.Implicits._
import models.Station
import play.Logger

class SearchController extends Controller {
  val inputForm = Form(
    mapping("station-name" -> list(text))(InputForm.apply)(InputForm.unapply))

  def index = Action {
    val emptyForm = inputForm.fill(InputForm(Nil))
    Ok(views.html.index(emptyForm))
  }

  //requestをimplicitで宣言することで、暗黙的にリクエストを引数として受け取る
  def search = Action { implicit request =>
    inputForm.bindFromRequest.fold(

      // バリデーションNG
      validationErrorForm => {
        Logger.debug("validation error")
        BadRequest(views.html.index(validationErrorForm))
      },

      // バリデーションOK
      form => {
        var inputStations = form.names.filter(x => x != "").map(x => new Station(x, "", null))

        // 相関チェック
        if (inputStations.size < 2) {
          BadRequest(views.html.index(inputForm.bindFromRequest(), "最低でも2つ"))
        } else {
          //経緯度取得
          // TODO webじゃなくてCSVのデータだけで完結できる
          // TODO 複数ヒットした時の仕組み必要
          inputStations.foreach { x =>
            Logger.debug(x.name)
            x.code = StationsManager.nameToCode(x.name)

            //webAPIで駅コードの経緯度取得する
            x.lonLat = StationsManager.codeToLonLat(x.code)
          }

          //中心経緯度計算
          val centerLonLat = new LonLatCalculator().calcCenterLonLat(inputStations)

          //最寄駅取得
          val nearStationsName = StationsManager.getNearStationsName(centerLonLat)

          //出力
          //TODO 画面出力する(仮）
          Ok(views.html.result(nearStationsName(0)))
        }
      })
  }
}