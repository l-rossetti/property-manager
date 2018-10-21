package controllers.forms



object PropertyForm {
    import play.api.data.Forms._
    import play.api.data.Form
    import models.Property
    import play.api.data.format.Formats.doubleFormat
    import play.api.data.validation.Constraints.{max, min}


    /**
      * The mapping for the property form.
      */
    val propertyForm: Form[Property] = Form {
        mapping(
            "id" ->             optional(longNumber),
            "address" ->        nonEmptyText,
            "postCode" ->       nonEmptyText.verifying("Number format required", p => p.matches("[0-9]+") ),
            "latitude" ->       of(doubleFormat).verifying(min(-180.0), max(180.0)),
            "longitude" ->      of(doubleFormat).verifying(min(-90.0), max(90.0)),
            "surface" ->        optional(number.verifying(min(0))),
            "bedRoomCount" ->   optional(number.verifying(min(0)))
        )(Property.apply)(Property.unapply)
    }

    /**
      * Get the form with validation
      *
      * @param property the Property if any
      * @Return the empty or filled form
      */
    def preparePropertyForm(property: Option[Property]) = {
        property match {
            case None => propertyForm
            case _    => propertyForm.fill(property.get)
        }
    }

    /**
      * Get the text for the form button
      *
      * @param property the Property if any
      * @Return the text for the form button
      */
    def getFormButtonText(property: Option[Property]) = {
        property match {
            case None => ButtonText.Create.toString
            case _    => ButtonText.Update.toString
        }
    }

    object ButtonText extends Enumeration {
        type ButtonText = Value

        val Create  = Value("Create")
        val Update  = Value("Update")
    }
}
