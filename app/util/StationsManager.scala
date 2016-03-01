package util

import java.io.File
import scala.collection.mutable.Map
import com.github.tototoshi.csv.CSVReader
import com.github.tototoshi.csv.defaultCSVFormat
import models.LonLat
import models.Station
import scala.xml.Null
import play.Logger
import models.Station
import servicies.LonLatCalculator

/*
 * 駅情報関係の情報やり取りを管理するクラス
 * TODO Javaっぽい書き方なのでそのうち直したい
 * TODO startsWithの"("をどうにかする
 * TODO 扱うデータ範囲の思想がぶれてる
 */
object StationsManager {

  //key:駅名（県名）、value：駅コード
  private var stationsMap = Map[String, String]()

  // TODO comment
  def init(): Unit = {
    var prefsMap = Map[String, String]()

    var reader = CSVReader.open(new File(".//resources//prefs.csv"))
    reader.allWithHeaders().foreach { f =>
      prefsMap.update(f("pref_cd"), f("pref_name"))
    }

    reader = CSVReader.open(new File(".//resources//stations.csv"))
    reader.allWithHeaders().foreach { f =>
      val key = f("station_name") + "(" + prefsMap(f("pref_cd")) + ")"
      val value = f("station_cd")
      stationsMap.update(key, value)
    }
  }

  // comment
  def countOf(name: String) = stationsMap.filterKeys { k =>
    k.startsWith(name + "(")
  }.size

  def getSameNameList(name: String) = stationsMap.filterKeys { k =>
    k.startsWith(name + "(")
  }.keys.toList

  def generateStation(name: String) = {
    val code = StationsManager.getCode(name)
    val lonLat = StationsManager.getLonLat(code)
    new Station(name, code, lonLat)
  }

  private def getCode(name: String) = stationsMap.filterKeys { k =>
    // TODO いけてない
    if (k.contains("(")) {
      k.startsWith(name)
    } else {
      k.startsWith(name + "(")
    }
  }.values.toList(0)

  private def getLonLat(code: String) = {
    val urlStr = "http://www.ekidata.jp/api/s/" + code + ".xml"
    val xml = new WebAccessor().responseXmlSync(urlStr, "station")

    val lon: Double = (xml \ "lon").text.toDouble
    val lat: Double = (xml \ "lat").text.toDouble

    new LonLat(lon, lat)
  }

  // TODO
  def searchCenterStationName(stations: List[Station]): String = {
    val centerLonLat = new LonLatCalculator().calcCenterLonLat(stations)
    val urlStr = "http://map.simpleapi.net/stationapi?x=" + centerLonLat.lon + "&y= " + centerLonLat.lat + "&output=xml"
    val xmls = new WebAccessor().responseXmlSync(urlStr, "station")

    // TODO WSの仕様でレスポンスがiso・・・で来る、無理やり個々でUTF-8に変換してる（もっとうまくやりたい）
    // simpleAPIの仕様で、近隣に駅がないと変な感じで帰ってくる、下記は対応できているがもっとうまく書きたい
    val seq = xmls.map { xml =>
      new String((xml \ "name").text.getBytes("iso-8859-1"), "utf-8")
    }
    seq.foreach { x => Logger.debug(x) }

    if (seq.size == 0) {
      return "中間地点近くに駅がありません"
    } else {
      seq(0)
    }
  }
}
