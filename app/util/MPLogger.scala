package util

import play.Logger

object MPLogger extends Logger {
  private def createMessage(obj: Object, message: String) = {
    "[class]:" + obj.getClass + " - [message]:" + message
  }

  def info(obj: Object, message: String) = {
    Logger.info(createMessage(obj, message))
  }

  def error(obj: Object, message: String, ex: Throwable = null) = {
    if (ex == null) {
      Logger.error(createMessage(obj, message))
    } else {
      Logger.error(createMessage(obj, message), ex)
    }
  }
}