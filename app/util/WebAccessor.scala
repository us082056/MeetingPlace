package util

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.xml.NodeSeq
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws.WS
import play.api.libs.ws.WSRequest
import play.Logger

class WebAccessor {
  def responseXmlSync(urlStr: String, topTag: String) = {
    Logger.debug("URL: " + urlStr)
    val request: WSRequest = WS.url(urlStr)

    // リクエスト成功時、指定タグでNodeSeqを取得
    val futureResult: Future[NodeSeq] = request.get().map {
      response => response.xml \\ topTag
    }

    //　Futureの処理が完了するまで待つ
    Await.result(futureResult, Duration.Inf)
  }
}