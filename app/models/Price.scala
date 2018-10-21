package models

case class Price (id: Option[Long], propertyID: Long, price: Double, date: String)

object Price {

    import play.api.mvc.QueryStringBindable

    implicit def priceBinder(implicit
                             optLongBinder: QueryStringBindable[Option[Long]],
                             longBinder: QueryStringBindable[Long],
                             doubleBinder:  QueryStringBindable[Double],
                             stringBinder:  QueryStringBindable[String],
                             ) =
        new QueryStringBindable[Price] {

            override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Price]] = {
                for {
                    id         <- optLongBinder.bind(key + ".id", params)
                    propertyID <- longBinder.bind(key + ".propertyID", params)
                    price      <- doubleBinder.bind(key + ".price", params)
                    date       <- stringBinder.bind(key + ".date", params)
                } yield {
                    (id, propertyID, price, date)
                    match {
                        case ( Right(id), Right(propertyID), Right(price), Right(date))
                            => Right(Price(id, propertyID, price, date))
                        case _ => Left("Unable to bind Price")
                    }
                }
            }

            override def unbind(key: String, p: Price): String =
                optLongBinder.unbind(key + ".id", p.id) + "&" +
                longBinder.unbind(key + ".propertyID", p.propertyID) + "&" +
                doubleBinder.unbind(key + ".price", p.price) + "&" +
                stringBinder.unbind(key + ".date", p.date)
        }
}