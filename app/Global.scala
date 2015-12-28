import play.api._
import util.StationsManager

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    StationsManager.init()
  }

}