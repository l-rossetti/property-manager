package controllers

import javax.inject.{Inject, Singleton}
import models.Property
import play.api.mvc.{MessagesAbstractController, MessagesControllerComponents}
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.data.format.Formats._
import play.api.Logger
import services.PropertyRepository

import scala.concurrent.{ExecutionContext}
import scala.util.{Failure, Success}

@Singleton
class PropertyController @Inject() (repo: PropertyRepository,
                                    cc: MessagesControllerComponents
                                   ) (implicit ec: ExecutionContext)
  extends MessagesAbstractController(cc) {

    object ButtonText extends Enumeration {
        type ButtonText = Value

        val Create  = Value("Create")
        val Update  = Value("Update")
    }

    /**
      * The mapping for the property form.
      */
    val propertyForm: Form[Property] = Form {
        mapping(
            "id" ->             optional(longNumber),
            "address" ->        nonEmptyText,
            "postCode" ->       number.verifying(min(0)),
            "latitude" ->       of(doubleFormat).verifying(min(0.0), max(100.0)),
            "longitude" ->      of(doubleFormat).verifying(min(0.0)),
            "surface" ->        optional(number.verifying(min(0))),
            "bedRoomCount" ->   optional(number.verifying(min(0)))
        )(Property.apply)(Property.unapply)
    }

    def getPropertyForm(property: Option[Property]) = {
        property match {
            case None => propertyForm
            case _    => propertyForm.fill(property.get)
        }
    }
    /**
      * The load property manager action.
      */
    def loadPropertyManager = Action.async { implicit request =>
        val createAction = routes.PropertyController.create()
        repo.getProperties().map {
            properties =>  Ok(views.html.property_manager(
                getPropertyForm(None),
                createAction,
                ButtonText.Create.toString,
                properties
            ))
        }
    }

    def fillForm(property: Property) = Action.async { implicit request =>
        val updateAction = routes.PropertyController.update(property)
        repo.getProperties().map {
            properties =>  Ok(views.html.property_manager(
                getPropertyForm(Some(property)),
                updateAction,
                ButtonText.Update.toString,
                properties
            ))
        }
    }

    /**
      * The add property action.
      *
      * This is asynchronous, since we're invoking the asynchronous methods on PropertyRepository.
      */
    def create = Action.async { implicit request =>
        // Bind the form first, then fold the result, passing a function to handle errors, and a function to handle success.
        propertyForm.bindFromRequest.fold(
            // The error function. We return the index page with the error form, which will render the errors.
            // We also wrap the result in a successful future, since this action is synchronous, but we're required to return
            // a future because the property creation function returns a future.
            errorForm => {
                val createAction = routes.PropertyController.create()
                repo.getProperties().map {
                    properties =>  Ok(views.html.property_manager(
                        errorForm, createAction,
                        ButtonText.Create.toString,
                        properties
                    ))
                }
            },
            // There were no errors in the form, so create the property.
            property => {
                repo.create(property).map {
                    case Success(property) =>
                        // If successful, we simply redirect to the main page.
                        Redirect(routes.PropertyController.loadPropertyManager())
                          .flashing("success" -> s"new property created with id ${property.id.get}")
                    case Failure(e) =>
                        Redirect(routes.PropertyController.loadPropertyManager())
                          .flashing("error" -> s"cannot create property ${e.getMessage}")
                }
            }
        )
    }

    def delete(id: Long) = Action.async { implicit request =>
        repo.delete(id).map {
            case false =>
                Redirect(routes.PropertyController.loadPropertyManager())
                  .flashing("error" -> "property cannot be updated")
            case true =>
                Redirect(routes.PropertyController.loadPropertyManager())
                  .flashing("success" -> s"property with id ${id} has been deleted")
        }
    }

    /**
      * The update property action.
      */
    def update(property: Property) = Action.async { implicit request =>

        propertyForm.bindFromRequest.fold(
            errorForm => {
                val updateAction = routes.PropertyController.update(property)
                repo.getProperties().map {
                    properties =>  Ok(views.html.property_manager(
                        errorForm,
                        updateAction,
                        ButtonText.Update.toString,
                        properties
                    ))
                }
            },
            property => {
                repo.update(property).map {
                    case None =>
                        Redirect(routes.PropertyController.loadPropertyManager())
                          .flashing("error" -> s"property with id ${property.id.get} cannot be updated")
                    case _ =>
                        Redirect(routes.PropertyController.loadPropertyManager())
                          .flashing("success" -> s"property with id  ${property.id.get} updated")
                }
            }
        )
    }

}
