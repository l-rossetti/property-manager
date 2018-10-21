import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneServerPerTest

/**
 * Runs a browser test using Fluentium against a play application on a server port.
 */
class BrowserSpec extends PlaySpec
    with OneBrowserPerTest
    with GuiceOneServerPerTest
    with HtmlUnitFactory
    with ServerProvider {

    "Application" should {

        "work from within a browser" in {
            go to ("http://localhost:" + port)
            pageSource must include ("Property Manager")
        }

        "load property-manager with property data" in {
            go to ("http://localhost:" + port + "/?property.id=1&property.address=Via+Manzoni%2C+30&property.postCode=20821&property.latitude=2.0&property.longitude=2.5555665&property.surface=10&property.bedRoomCount=10")
            pageSource must include ("Property Manager")
            pageSource must include ("Manzoni")
        }

        "load price-manager" in {
            go to ("http://localhost:" + port + "/price-manager?propertyID=99999")
            pageSource must include ("Price Manager")
            pageSource must include ("Prices for property id 99999")
        }
    }

}
