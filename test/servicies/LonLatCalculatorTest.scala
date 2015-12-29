package servicies

import org.scalatest.FlatSpec
import models._

class LonLatCalculatorTest extends FlatSpec {

  "3点の経緯度" should "3点の重心" in {
    val station1 = new Station("札幌", "1110315", new LonLat(141.350768, 43.068612))
    val station2 = new Station("大麻", "1110320", new LonLat(141.496925, 43.072382))
    val station3 = new Station("美唄", "1110330", new LonLat(141.862157, 43.330751))
    val stations = List(station1, station2, station3)

    val center = new LonLatCalculator().calcCenterLonLat(stations)

    assert(center.lon == 141.56994999999998)
    assert(center.lat == 43.157248333333335)
  }
}