package models

case class Price (id: Option[Long], propertyID: Long, price: Double, date: String)
case class PriceEvolution (evolution: Price)
