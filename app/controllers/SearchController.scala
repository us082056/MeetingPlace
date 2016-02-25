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
import scala.collection.immutable.Nil
import play.api.mvc.Result

class SearchController extends Controller {
  val inputForm = Form(
    mapping("station-name" -> list(text))(InputForm.apply)(InputForm.unapply))

  def index = Action {
    val emptyForm = inputForm.fill(InputForm(Nil))
    Ok(views.html.index(emptyForm, Nil))
  }

  //requestをimplicitで宣言することで、暗黙的にリクエストを引数として受け取る
  def search = Action { implicit request =>
    def handle: Result = {
      inputForm.bindFromRequest.fold(

        // バリデーションNG
        validationErrorForm => {
          Logger.debug("validation error")
          return BadRequest(views.html.index(validationErrorForm, Nil))
        },

        // バリデーションOK
        form => {
          var inputStations = form.names.filter(x => x != "").map(x => new Station(x, "", null))

          // 相関チェック（入力項目数チェック）
          if (inputStations.size < 2) {
            Logger.debug("項目数エラー")
            return BadRequest(views.html.index(inputForm.bindFromRequest(), List("2つ以上の出発駅を入力してください")))
          }

          // 相関チェック（キーワード存在チェック）
          var errorMsgList = inputStations.filter { x => !StationsManager.exists(x) }.map { x => x.name + "が見つかりません、駅名が正しいか確認してください" }
          if (errorMsgList.size != 0) {
            Logger.debug("存在チェックエラー")
            return BadRequest(views.html.index(inputForm.bindFromRequest(), errorMsgList))
          }

          //経緯度取得
          // TODO webじゃなくてCSVのデータだけで完結できる
          // TODO 複数ヒットした時の仕組み必要
          // TODO Manager内に隠蔽したい（Stationクラス単位の処理はManager内に閉じたい）
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

          //出力 TODO(仮）
          return Ok(views.html.result(nearStationsName(0)))
        })
    }
    handle
  }
}