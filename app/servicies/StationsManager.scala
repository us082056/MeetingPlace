package servicies

import java.io.File
import scala.collection.mutable.Map
import com.github.tototoshi.csv.CSVReader
import com.github.tototoshi.csv.defaultCSVFormat
import models.LonLat
import models.Station
import models.Station
import util.LonLatCalculator
import util.WebAccessor
import models.CandidateStation
import util.MPLogger

/*
 * 駅情報を扱う
 * TODO startsWithの"("をどうにかする
 */
object StationsManager {

  //key:駅名（県名）、value：駅コード
  private var stationsMap = Map[String, String]()

  def init(): Unit = {
    var prefsMap = Map[String, String]()

    var reader = CSVReader.open(new File(".//resources//prefs.csv"))
    reader.allWithHeaders().foreach { f =>
      prefsMap.update(f("pref_cd"), f("pref_name"))
    }

    reader = CSVReader.open(new File(".//resources//stations.csv"))
    reader.allWithHeaders().foreach { f =>
      val key = f("station_name") + "(" + prefsMap(f("pref_cd")) + ")"
      val value = f("station_g_cd")
      stationsMap.update(key, value)
    }
  }

  def countOf(name: String) = stationsMap.filterKeys { k =>
    k.startsWith(name + "(")
  }.size

  def getSameNameList(name: String) = stationsMap.filterKeys { k =>
    k.startsWith(name + "(")
  }.keys.toList

  // countOfの結果が1件だけの場合に使用可能
  def generateStation(name: String) = {
    val fixedName = {
      if (name.contains("(")) {
        // "("が含まれる場合は県名まで特定できているのでnameをそのまま使用
        name
      } else {
        // 名前解決（県名まで特定）
        stationsMap.filterKeys { key =>
          key.startsWith(name + "(")
        }.keys.toList(0)
      }
    }

    val code = stationsMap(fixedName)
    val lonLat = StationsManager.getLonLat(code)
    new Station(fixedName, code, lonLat)
  }

  private def getLonLat(code: String) = {
    val urlStr = "http://www.ekidata.jp/api/s/" + code + ".xml"
    val xml = new WebAccessor().responseXmlSync(urlStr, "station")

    val lon: Double = (xml \ "lon").text.toDouble
    val lat: Double = (xml \ "lat").text.toDouble

    new LonLat(lon, lat)
  }

  def searchCandidate(stations: List[Station]) = {
    val centerLonLat = new LonLatCalculator().calcCenterLonLat(stations)
    val urlStr = "http://map.simpleapi.net/stationapi?x=" + centerLonLat.lon + "&y= " + centerLonLat.lat + "&output=xml"
    val stationXmls = new WebAccessor().responseXmlSync(urlStr, "station")

    // WSの仕様でレスポンスの文字コードがisoなのでUTF-8に変換
    stationXmls.map { xml =>
      val name = new String((xml \ "name").text.getBytes("iso-8859-1"), "utf-8").replace("駅", "")
      val line = new String((xml \ "line").text.getBytes("iso-8859-1"), "utf-8")
      new CandidateStation(name, line)
    }.take(3)
  }
}
