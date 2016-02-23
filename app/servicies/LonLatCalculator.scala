package servicies

import models._
import play.api.data.Mapping

class LonLatCalculator {
  def calcCenterLonLat(stations: List[Station]) = {

    // 両変数ともに初期値を設定
    var lonSum, latSum = 0.0
    
    stations.foreach { x => 
      lonSum += x.lonLat.lon
      latSum += x.lonLat.lat
    }

    new LonLat(lonSum / stations.length, latSum / stations.length)
  }
}