package controllers

import scala.collection.immutable.Nil
import scala.collection.mutable.Map
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
import servicies.StationsManager

class SearchController extends Controller {
  val inputForm = Form(
    mapping("station-name" -> list(text))(InputForm.apply)(InputForm.unapply))

  def index = Action {
    val emptyForm = inputForm.fill(InputForm(Nil))
    Ok(views.html.index(emptyForm, Nil))
  }

  //requestをimplicitで宣言することで、暗黙的にリクエストを引数として受け取る
  def inspection = Action { implicit request =>
    def handle: Result = {
      inputForm.bindFromRequest.fold(

        // バリデーションNG
        validationErrorForm => {
          Logger.debug("バリデーションエラー")
          return BadRequest(views.html.index(validationErrorForm, Nil))
        },

        // バリデーションOK
        form => {
          val filteredNameList = form.names.filter { name => (name != "") }

          // 相関チェック（入力項目数チェック）
          if (filteredNameList.size < 2) {
            Logger.debug("項目数エラー")
            return BadRequest(views.html.index(inputForm.bindFromRequest(), List("2つ以上の出発駅を入力してください。")))
          }

          var errorMsgList = List[String]()
          var stationList = List[Station]()
          var sameNameMap = Map[String, List[String]]()

          filteredNameList.foreach { name =>
            val count = StationsManager.countOf(name)
            if (count == 0) {
              errorMsgList :+= (name + "が見つかりません、駅名が正しいか確認してください。")
            } else if (count >= 2) {
              Logger.debug("複数件ヒット:" + name)
              val sameNameList = StationsManager.getSameNameList(name)
              sameNameMap.update(name, sameNameList)
            } else {
              Logger.debug("一件ヒット:" + name)
              stationList :+= StationsManager.generateStation(name)
            }
          }

          // 相関チェック（キーワード存在チェック）
          if (!errorMsgList.isEmpty) {
            Logger.debug("存在チェックエラー")
            return BadRequest(views.html.index(inputForm.bindFromRequest(), errorMsgList))
          }

          // 同じ名前が複数ある駅が入力された場合は選択画面を表示
          if (!sameNameMap.isEmpty) {
            return Ok(views.html.candidate(stationList, sameNameMap))
          }

          // 検索ロジック実行
          searchLogic(stationList)
        })
    }
    handle
  }

  def search = Action { implicit request =>
    val stationList = inputForm.bindFromRequest().get.names.map { name =>
      Logger.debug("一件ヒット:" + name)
      StationsManager.generateStation(name)
    }

    searchLogic(stationList)
  }

  private def searchLogic(stationList: List[Station]) = {
    val candidateSeq = StationsManager.searchCandidate(stationList)
    Ok(views.html.result(candidateSeq, stationList))
  }
}