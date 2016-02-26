package controllers

import scala.collection.immutable.Nil
import models.InputForm
import models.Station
import play.Logger
import play.api.data.Form
import play.api.data.Forms.list
import play.api.data.Forms.mapping
import play.api.data.Forms.text
import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.mvc.Result
import servicies.LonLatCalculator
import util.StationsManager
import akka.dispatch.SaneRejectedExecutionHandler
import scala.collection.mutable.Map

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

          // TODO 同じフィルター条件をまとめたい
          // 相関チェック（入力項目数チェック）
          if (form.names.filter(name => name != "").size < 2) {
            Logger.debug("項目数エラー")
            return BadRequest(views.html.index(inputForm.bindFromRequest(), List("2つ以上の出発駅を入力してください")))
          }

          // 相関チェック（キーワード存在チェック）
          val errorMsgList = form.names.filter(name => name != "").filter { name => StationsManager.countOf(name) == 0 }.map { name =>
            name + "が見つかりません、駅名が正しいか確認してください"
          }
          if (errorMsgList.size != 0) {
            Logger.debug("存在チェックエラー")
            return BadRequest(views.html.index(inputForm.bindFromRequest(), errorMsgList))
          }

          var inputStations = List[Station]()
          var sameNameMap = Map[String, List[String]]()

          form.names.filter(name => name != "").foreach { name =>
            if (StationsManager.countOf(name) == 1) {
              Logger.debug("一件ヒット:" + name)

              val code = StationsManager.getCode(name)
              val lonLat = StationsManager.getLonLat(code)
              val station = new Station(name, code, lonLat)
              inputStations :+= station
            } else {
              Logger.debug("複数件ヒット:" + name)

              val sameNameList = StationsManager.getSameNameList(name)
              sameNameMap.update(name, sameNameList)

              // TODO 以下暫定
              {
                val code = StationsManager.getCode(name)
                val lonLat = StationsManager.getLonLat(code)
                val station = new Station(name, code, lonLat)
                inputStations :+= station
              }
            }
          }

          //TODO 複数候補ある場合（暫定）
          {
            sameNameMap.keys.foreach { key =>
              Logger.debug("key:" + key)
              sameNameMap(key).foreach { name => Logger.debug("name:" + name) }
            }
          }

          val centerLonLat = new LonLatCalculator().calcCenterLonLat(inputStations)
          val nearStationsName = StationsManager.getNearStationsName(centerLonLat)

          //出力 TODO(仮）
          return Ok(views.html.result(nearStationsName(0)))
        })
    }
    handle
  }
}