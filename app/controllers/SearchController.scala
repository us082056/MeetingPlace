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
import util.MPLogger

class SearchController extends Controller {
  val inputForm = Form(
    mapping("station-name" -> list(text))(InputForm.apply)(InputForm.unapply))

  def index = Action {
    val emptyForm = inputForm.fill(InputForm(Nil))
    Ok(views.html.index(emptyForm, Nil))
  }

  //requestをimplicitで宣言することで、暗黙的にリクエストを引数として受け取る
  def inspection = Action { implicit request =>
    MPLogger.info(this, "入力値の検証開始")
    def handle: Result = {
      inputForm.bindFromRequest.fold(

        // バリデーションNG
        validationErrorForm => {
          return BadRequest(views.html.index(validationErrorForm, Nil))
        },

        // バリデーションOK
        form => {
          val filteredNameList = form.names.filter { name => (name != "") }

          // 相関チェック（入力項目数チェック）
          if (filteredNameList.size < 2) {
            return BadRequest(views.html.index(inputForm.bindFromRequest(), List("2つ以上の出発駅を入力してください。")))
          }

          var notFoundMsgList = List[String]()
          var stationList = List[Station]()
          var sameNameMap = Map[String, List[String]]()

          filteredNameList.foreach { name =>
            val count = StationsManager.countOf(name)
            if (count == 0) {
              MPLogger.info(this, "候補なし:" + name)
              notFoundMsgList :+= (name + "が見つかりません、駅名が正しいか確認してください。")
            } else if (count >= 2) {
              MPLogger.info(this, "複数件hit:" + name)
              val sameNameList = StationsManager.getSameNameList(name)
              sameNameMap.update(name, sameNameList)
            } else {
              MPLogger.info(this, "一件hit:" + name)
              stationList :+= StationsManager.generateStation(name)
            }
          }

          // 相関チェック（キーワード存在チェック）
          if (!notFoundMsgList.isEmpty) {
            return BadRequest(views.html.index(inputForm.bindFromRequest(), notFoundMsgList))
          }

          // 同じ名前が複数ある駅が入力された場合は選択画面を表示
          if (!sameNameMap.isEmpty) {
            MPLogger.info(this, "複数候補の選択画面表示")
            return Ok(views.html.candidate(stationList, sameNameMap))
          }

          // 検索ロジック実行
          searchLogic(stationList)
        })
    }
    handle
  }

  def search = Action { implicit request =>
    MPLogger.info(this, "複数候補選択完了、入力値取得開始")
    val stationList = inputForm.bindFromRequest().get.names.map { name =>
      MPLogger.info(this, "入力値:" + name)
      StationsManager.generateStation(name)
    }
    searchLogic(stationList)
  }

  private def searchLogic(stationList: List[Station]) = {
    MPLogger.info(this, "検索ロジック開始")
    val candidateSeq = StationsManager.searchCandidate(stationList)
    MPLogger.info(this, "検索ロジック終了")
    Ok(views.html.result(candidateSeq, stationList))
  }
}