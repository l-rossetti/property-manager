package controllers

import javax.inject.{Inject, Singleton}
import models.Property
import play.api.mvc.{MessagesAbstractController, MessagesControllerComponents}
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.data.format.Formats._
import play.api.libs.json.Json
import play.api.Logger
import services.PropertyRepository

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class PropertyController @Inject() (repo: PropertyRepository,
                                    cc: MessagesControllerComponents
                                   ) (implicit ec: ExecutionContext)
  extends MessagesAbstractController(cc) {

    /**
      * The mapping for the property form.
      */
    val propertyForm: Form[Property] = Form {
        mapping(
            "id" -> optional(longNumber),
            "address" -> nonEmptyText,
            "postCode" -> number.verifying(min(0)),
            "latitude" -> of(doubleFormat),
            "longitude" -> of(doubleFormat),
            "surface" -> optional(number.verifying(min(0))),
            "bedRoomCount" -> optional(number.verifying(min(0)))
        )(Property.apply)(Property.unapply)
    }

    /**
      * The manage action.
      */
    def manage = Action { implicit request =>
        Ok(views.html.manage(propertyForm))
    }

    /**
      * A REST endpoint that gets all the properties as JSON.
      */
    def getProperties = Action.async { implicit request =>
        repo.getProperties().map { properties =>
            Ok(views.html.propertieslist(properties.toList))
        }
    }

    /**
      * The add property action.
      *
      * This is asynchronous, since we're invoking the asynchronous methods on PropertyRepository.
      */
    def createProperty = Action.async { implicit request =>
        // Bind the form first, then fold the result, passing a function to handle errors, and a function to handle success.
        propertyForm.bindFromRequest.fold(
            // The error function. We return the index page with the error form, which will render the errors.
            // We also wrap the result in a successful future, since this action is synchronous, but we're required to return
            // a future because the property creation function returns a future.
            errorForm => {
                Future.successful(Ok(views.html.manage(errorForm)))
            },
            // There were no errors in the form, so create the property.
            property => {
                repo.create(property).map {
                    case Success(property) =>
                        // If successful, we simply redirect to the manage page.
                        Redirect(routes.PropertyController.manage()).flashing("success" -> s"property created with id ${property.id}")
                    case Failure(e) =>
                        Redirect(routes.PropertyController.manage()).flashing("error" -> s"cannot create property ${e.getMessage}")
                }
            }
        )
    }

    def deleteProperty = Action.async { implicit request =>
        propertyForm.bindFromRequest.fold(
            errorForm => {
                Future.successful(Ok(views.html.manage(errorForm)))
            },
            property => {
                repo.delete(property.id.get).map {
                    case false => Redirect(routes.PropertyController.manage()).flashing("error" -> "property cannot be updated")
                    case true => Redirect(routes.PropertyController.manage()).flashing("success" -> "property.deleted")
                }
            }
        )
    }

    def updateProperty = Action.async { implicit request =>
        propertyForm.bindFromRequest.fold(
            errorForm => {
                Future.successful(Ok(views.html.manage(errorForm)))
            },
            property => {
                repo.update(property).map {
                    case None => Redirect(routes.PropertyController.manage()).flashing("error" -> s"property with id ${property.id} cannot be updated")
                    case _ => Redirect(routes.PropertyController.manage()).flashing("success" -> s"property with id  ${property.id} updated")
                }
            }
        )
    }

}
