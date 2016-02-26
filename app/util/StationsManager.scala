package util

import java.io.File
import scala.collection.mutable.Map
import com.github.tototoshi.csv.CSVReader
import com.github.tototoshi.csv.defaultCSVFormat
import models.LonLat
import models.Station
import scala.xml.Null
import models.Station

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

  // TODO comment
  def getCode(name: String) = stationsMap.filterKeys { k =>
    k.startsWith(name + "(")
  }.values.toList(0)

  def getLonLat(code: String) = {
    val urlStr = "http://www.ekidata.jp/api/s/" + code + ".xml"
    val xml = new WebAccessor().responseXmlSync(urlStr, "station")

    val lon: Double = (xml \ "lon").text.toDouble
    val lat: Double = (xml \ "lat").text.toDouble

    new LonLat(lon, lat)
  }

  def getNearStationsName(lonLat: LonLat) = {
    val urlStr = "http://map.simpleapi.net/stationapi?x=" + lonLat.lon + "&y= " + lonLat.lat + "&output=xml"
    val xmls = new WebAccessor().responseXmlSync(urlStr, "station")

    // TODO WSの仕様でレスポンスがiso・・・で来る、無理やり個々でUTF-8に変換してる（もっとうまくやりたい）
    xmls.map {
      xml =>
        new String((xml \ "name").text.getBytes("iso-8859-1"), "utf-8")
    }
  }
}