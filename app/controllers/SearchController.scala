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
          val filteredNameList = form.names.filter { name => name != "" }

          // TODO 同じフィルター条件をまとめたい
          // 相関チェック（入力項目数チェック）
          if (filteredNameList.size < 2) {
            Logger.debug("項目数エラー")
            return BadRequest(views.html.index(inputForm.bindFromRequest(), List("2つ以上の出発駅を入力してください")))
          }

          var errorMsgList = List[String]()
          var stationList = List[Station]()
          var sameNameMap = Map[String, List[String]]()

          filteredNameList.foreach { name =>
            val count = StationsManager.countOf(name)

            if (count == 0) {
              errorMsgList :+= (name + "が見つかりません、駅名が正しいか確認してください")
            } else if (count >= 2) {
              Logger.debug("複数件ヒット:" + name)

              val sameNameList = StationsManager.getSameNameList(name)
              sameNameMap.update(name, sameNameList)
            } else {
              Logger.debug("一件ヒット:" + name)

              // TODO 下記3件のvalはまとめられそう
              val code = StationsManager.getCode(name)
              val lonLat = StationsManager.getLonLat(code)
              val station = new Station(name, code, lonLat)
              stationList :+= station
            }
          }

          // 相関チェック（キーワード存在チェック）
          if (errorMsgList.size != 0) {
            Logger.debug("存在チェックエラー")
            return BadRequest(views.html.index(inputForm.bindFromRequest(), errorMsgList))
          }

          //TODO 複数候補ある場合
          if (sameNameMap.size != 0) {
            return Ok(views.html.candidate(stationList, sameNameMap))
          }

          val nearStationsName = StationsManager.searchCenterStationName(stationList)

          //出力 TODO(仮）
          return Ok(views.html.result(nearStationsName))
        })
    }
    handle
  }

  def search2 = Action { implicit request =>
    Logger.debug("■search2")
    val stationList = inputForm.bindFromRequest().get.names.map { name =>
      Logger.debug("一件ヒット:" + name)
      val code = StationsManager.getCode(name)
      val lonLat = StationsManager.getLonLat(code)
      new Station(name, code, lonLat)
    }

    val nearStationsName = StationsManager.searchCenterStationName(stationList)

    //出力 TODO(仮）
    Ok(views.html.result(nearStationsName))
  }
}