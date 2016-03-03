import scala.concurrent.Future

import play.api.Application
import play.api.GlobalSettings
import play.api.Logger
import play.api.mvc.RequestHeader
import play.api.mvc.Results.InternalServerError
import play.api.mvc.Results.NotFound
import util.StationsManager

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    StationsManager.init()
  }

  override def onError(request: RequestHeader, ex: Throwable) = {
    Logger.error("システムエラー", ex)

    Future.successful(InternalServerError(
      views.html.error("予期せぬエラーが発生しました")))
  }

  override def onHandlerNotFound(request: RequestHeader) = {
    Logger.error("パス存在エラー")

    Future.successful(NotFound(
      views.html.error("指定されたページは存在しません")))
  }
}