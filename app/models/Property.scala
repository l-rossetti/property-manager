package models

import play.api.libs.json.Json


case class Property (id: Option[Long],
                     address: String,
                     postCode: Int,
                     latitude: Double,
                     longitude: Double,
                     surface: Option[Int],
                     bedRoomCount: Option[Int])

object Property {
    implicit val propertyFormat = Json.format[Property]
}