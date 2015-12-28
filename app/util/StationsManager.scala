package util

import java.io.File
import scala.collection.mutable.Map
import com.github.tototoshi.csv.CSVReader
import com.github.tototoshi.csv.defaultCSVFormat
import models.LonLat
import models.Station
import scala.xml.Null

/*
 * 駅情報関係の情報やり取りを管理するクラス
 * ※重複する駅は上書きで保持（同じ駅で路線が違うだけのデータが含まれるため）
 * TODO Javaっぽい書き方なのでそのうち直したい
 */
object StationsManager {

  //key:駅名、value：駅コード
  private var stationsMap = Map[String, String]()

  def init(): Unit = {
    val reader = CSVReader.open(new File(".//resources//stations.csv"))
    val readDataList = reader.allWithHeaders()

    for (lineMap <- readDataList) {
      stationsMap.update(lineMap("station_name"), lineMap("station_cd"))
    }
  }

  def nameToCode(name: String) = stationsMap(name)

  def codeToLonLat(code: String) = {
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