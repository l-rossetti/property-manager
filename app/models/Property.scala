package models

case class Property (id: Option[Long],
                     address: String,
                     postCode: Int,
                     latitude: Double,
                     longitude: Double,
                     surface: Option[Int],
                     bedRoomCount: Option[Int])

object Property {

    import play.api.mvc.QueryStringBindable

    implicit def propertyBinder(implicit
                                    optLongBinder: QueryStringBindable[Option[Long]],
                                    stringBinder:  QueryStringBindable[String],
                                    doubleBinder:  QueryStringBindable[Double],
                                    optIntBinder:  QueryStringBindable[Option[Int]],
                                    intBinder:     QueryStringBindable[Int]
                               ) =
        new QueryStringBindable[Property] {

            override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Property]] = {
                for {
                    id              <- optLongBinder.bind(key + ".id", params)
                    address         <- stringBinder.bind(key + ".address", params)
                    postCode        <- intBinder.bind(key + ".postCode", params)
                    latitude        <- doubleBinder.bind(key + ".latitude", params)
                    longitude       <- doubleBinder.bind(key + ".longitude", params)
                    surface         <- optIntBinder.bind(key + ".surface", params)
                    bedRoomCount    <- optIntBinder.bind(key + ".bedRoomCount", params)
                } yield {
                    (id, address, postCode, latitude, longitude, surface, bedRoomCount)
                        match {
                            case ( Right(id), Right(address), Right(postCode),
                                   Right(latitude), Right(longitude),
                                   Right(surface), Right(bedRoomCount))
                                        => Right(Property(id, address, postCode, latitude, longitude,surface, bedRoomCount))
                            case _ => Left("Unable to bind Property")
                    }
                }
            }

            override def unbind(key: String, p: Property): String =
                optLongBinder.unbind(key + ".id", p.id) + "&" +
                stringBinder.unbind(key + ".address", p.address) + "&" +
                intBinder.unbind(key + ".postCode", p.postCode) + "&" +
                doubleBinder.unbind(key + ".latitude", p.latitude) + "&" +
                doubleBinder.unbind(key + ".longitude", p.longitude) +
                ( p.surface match {
                    case None => ""
                    case _    => "&" + optIntBinder.unbind(key + ".surface", p.surface)
                }) +
                ( p.bedRoomCount match {
                    case None => ""
                    case _    => "&" + optIntBinder.unbind(key + ".bedRoomCount", p.bedRoomCount)
                })


        }
}