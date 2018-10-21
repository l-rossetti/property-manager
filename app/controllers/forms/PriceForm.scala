package controllers.forms


object PriceForm {

    import play.api.data.Forms._
    import play.api.data.Form
    import models.Price
    import play.api.data.format.Formats.doubleFormat
    import play.api.data.validation.Constraints.{min}


    /**
      * The mapping for the price form.
      */
    val priceForm: Form[Price] = Form {
        mapping(
            "id" ->         optional(longNumber),
            "propertyID" -> longNumber,
            "price" ->      of(doubleFormat).verifying(min(1000.0)),
            // date: dd-mm-yyyy (eg. 31/12/1999)
            "date" ->       nonEmptyText.verifying("Date format dd-mm-yyyy", p => p.matches("""((0[1-9]|[12]\d|3[01])-(0[1-9]|1[0-2])-[12]\d{3})"""))
        )(Price.apply)(Price.unapply)
    }

    /**
      * Get the form with validation
      *
      * @param price the Price if any
      * @Return the empty or filled form
      */
    def preparePriceForm(price: Option[Price]) = {
        price match {
            case None => priceForm
            case _    => priceForm.fill(price.get)
        }
    }

    /**
      * Get the text for the form button
      *
      * @param price the Price if any
      * @Return the text for the form button
      */
    def getFormButtonText(price: Option[Price]) = {
        price match {
            case None => ButtonText.Create.toString
            case _    => ButtonText.Update.toString
        }
    }

    object ButtonText extends Enumeration {
        type ButtonText = Value
        val Create  = Value("Create new")
        val Update  = Value("Update price")
    }

}
