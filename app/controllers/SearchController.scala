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
          var inputStations = form.names.filter(v => (v != "")).map {
            v => new Station(v, "", null)
          }

          // 相関チェック（入力項目数チェック）
          if (inputStations.size < 2) {
            Logger.debug("項目数エラー")
            return BadRequest(views.html.index(inputForm.bindFromRequest(), List("2つ以上の出発駅を入力してください")))
          }

          // 相関チェック（キーワード存在チェック）
          var errorMsgList = inputStations.filter { s => StationsManager.countOf(s) == 0 }.map { s =>
            s.name + "が見つかりません、駅名が正しいか確認してください"
          }

          if (errorMsgList.size != 0) {
            Logger.debug("存在チェックエラー")
            return BadRequest(views.html.index(inputForm.bindFromRequest(), errorMsgList))
          }

          //経緯度取得
          // TODO 複数ヒットした時の仕組み必要
          // TODO よく考えればきれいに書けそう
          // TODO Manager内に隠蔽したい（Stationクラス単位の処理はManager内に閉じたい）
          inputStations.foreach { s =>
            if (StationsManager.countOf(s) == 1) {
              Logger.debug("一件ヒット")
              Logger.debug(s.name)
              s.code = StationsManager.nameToCode(s.name)

              //webAPIで駅コードの経緯度取得する
              s.lonLat = StationsManager.codeToLonLat(s.code)
            } else {
              StationsManager.getSameNameList(s.name).foreach { v =>
                Logger.debug("複数件ヒット")
                Logger.debug(v)
              }
              // TODO 以下暫定 
              s.code = StationsManager.nameToCode(s.name)
              s.lonLat = StationsManager.codeToLonLat(s.code)
            }
          }

          //TODO 複数候補ある場合

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