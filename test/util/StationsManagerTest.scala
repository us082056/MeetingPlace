package util

import org.scalatest.BeforeAndAfter
import org.scalatest.Finders
import org.scalatest.FlatSpec

import play.test.Helpers

class StationsManagerTest extends FlatSpec with BeforeAndAfter {

  before {
    StationsManager.init()
  }

  // 駅名から駅コードへ変換（1行目データ）
  "駅名が函館" should "駅コードは1110101か1111922" in {
    val station_cd = StationsManager.nameToCode("函館")
    assert(station_cd == "1110101" || station_cd == "1111922")
  }

  // 駅名から駅コードへ変換（最終行データ）
  "駅名が関門海峡めかり" should "駅コードは9992804" in {
    val station_cd = StationsManager.nameToCode("関門海峡めかり")
    assert(station_cd == "9992804")
  }

  // TODO コードから経緯度への変換単体のテストが必要

  // 駅名から経緯度へ変換
  "駅名が品川" should "経緯度は品川のもの" in {
    val app = Helpers.fakeApplication()

    Helpers.start(app)
    val station_cd = StationsManager.nameToCode("品川")

    val lonLat = StationsManager.codeToLonLat(station_cd)
    assert(lonLat.lon == 139.73809)
    assert(lonLat.lat == 35.628284)

    Helpers.stop(app)
  }

  "品川駅の最寄り駅" should "品川駅らへん" in {
    val app = Helpers.fakeApplication()

    Helpers.start(app)
    val station_cd = StationsManager.nameToCode("品川")
    val lonLat = StationsManager.codeToLonLat(station_cd)

    val stationsName = StationsManager.getNearStationsName(lonLat)
    assert(stationsName(0).startsWith("品川"))

    Helpers.stop(app)
  }
}