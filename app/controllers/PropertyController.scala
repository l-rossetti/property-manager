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

    val createAction = "Create"
    val updateAction = "Update"

    /**
      * The mapping for the property form.
      */
    val propertyForm: Form[Property] = Form {
        mapping(
            "id" -> optional(longNumber),
            "address" -> default(nonEmptyText, "via manzoni,30"),
            "postCode" -> default(number.verifying(min(0)), 6160),
            "latitude" -> default(of(doubleFormat), 140.8),
            "longitude" -> default(of(doubleFormat), 198.2),
            "surface" -> optional(number.verifying(min(0))),
            "bedRoomCount" -> optional(number.verifying(min(0)))
        )(Property.apply)(Property.unapply)
    }
    
    /**
      * The load property manager action.
      */
    def loadPropertyManager = Action.async { implicit request =>
        repo.getProperties().map { properties =>  Ok(views.html.property_manager(propertyForm, createAction, properties)) }
    }
    def configure(property: Property) = Action.async { implicit request =>
        repo.getProperties().map { properties =>  Ok(views.html.property_manager(propertyForm, updateAction, properties)) }
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
                    repo.getProperties().map { properties =>  Ok(views.html.property_manager(errorForm, createAction, properties)) }
            },
            // There were no errors in the form, so create the property.
            property => {
                repo.create(property).map {
                    case Success(property) =>
                        // If successful, we simply redirect to the manage page.
                        Redirect(routes.PropertyController.loadPropertyManager()).flashing("success" -> s"property created with id ${property.id}")
                    case Failure(e) =>
                        Redirect(routes.PropertyController.loadPropertyManager()).flashing("error" -> s"cannot create property ${e.getMessage}")
                }
            }
        )
    }

    def deleteProperty(id: Long) = Action.async { implicit request =>
        //Logger.info(s"${request.body.asJson.get("id").as[String]}")

        repo.delete(id).map {
            case false =>
                Logger.info(request.headers.toString())
                Redirect(routes.PropertyController.loadPropertyManager())
                  .flashing("error" -> "property cannot be updated")
            case true =>
                Logger.info(request.headers.toString())
                Redirect(routes.PropertyController.loadPropertyManager())
                  .flashing("success" -> s"property with id ${id} has been deleted")
        }
    }

    def updateProperty = Action.async { implicit request =>
        propertyForm.bindFromRequest.fold(
            errorForm => {
                repo.getProperties().map { properties =>  Ok(views.html.property_manager(errorForm, updateAction, properties)) }
            },
            property => {
                repo.update(property).map {
                    case None => Redirect(routes.PropertyController.loadPropertyManager()).flashing("error" -> s"property with id ${property.id} cannot be updated")
                    case _ => Redirect(routes.PropertyController.loadPropertyManager()).flashing("success" -> s"property with id  ${property.id} updated")
                }
            }
        )
    }

}
