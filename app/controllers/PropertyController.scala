package controllers


import javax.inject.{Inject, Singleton}
import play.api.mvc.{MessagesAbstractController, MessagesControllerComponents}
import services.PropertyRepository

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

@Singleton
class PropertyController @Inject() (repo: PropertyRepository,
                                    cc: MessagesControllerComponents
                                   ) (implicit ec: ExecutionContext)
  extends MessagesAbstractController(cc) {


    import forms.PropertyForm._
    import models.Property

    def getCreateUpdateRoute(property: Option[Property]) = {
        property match {
            case None => routes.PropertyController.create
            case _    => routes.PropertyController.update(property.get)
        }
    }

    /**
      * The load property manager action.
      */
    def loadPropertyManager(property: Option[Property]) = Action.async { implicit request =>
        repo.getProperties().map {
            properties =>  Ok(views.html.property_manager(
                preparePropertyForm(property),
                getCreateUpdateRoute(property),
                getFormButtonText(property),
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
                repo.getProperties().map {
                    properties =>  BadRequest(views.html.property_manager(
                        errorForm,
                        getCreateUpdateRoute(None),
                        getFormButtonText(None),
                        properties
                    ))
                }
            },
            // There were no errors in the form, so create the property.
            property => {
                repo.create(property).map {
                    case Success(property) =>
                        // If successful, we simply redirect to the main page.
                        Redirect(routes.PropertyController.loadPropertyManager(None))
                          .flashing("success" -> s"new property created with id ${property.id.get}")
                    case Failure(e) =>
                        Redirect(routes.PropertyController.loadPropertyManager(None))
                          .flashing("error" -> s"cannot create property ${e.getMessage}")
                }
            }
        )
    }

    def delete(id: Long) = Action.async { implicit request =>
        repo.delete(id).map {
            case false =>
                Redirect(routes.PropertyController.loadPropertyManager(None))
                  .flashing("error" -> "property cannot be updated")
            case true =>
                Redirect(routes.PropertyController.loadPropertyManager(None))
                  .flashing("success" -> s"property with id ${id} has been deleted")
        }
    }

    /**
      * The update property action.
      */
    def update(property: Property) = Action.async { implicit request =>

        propertyForm.bindFromRequest.fold(
            errorForm => {
                repo.getProperties().map {
                    properties =>  BadRequest(views.html.property_manager(
                        errorForm,
                        getCreateUpdateRoute(Some(property)),
                        getFormButtonText(Some(property)),
                        properties
                    ))
                }
            },
            property => {
                repo.update(property).map {
                    case None =>
                        Redirect(routes.PropertyController.loadPropertyManager(Some(property)))
                          .flashing("error" -> s"property with id ${property.id.get} cannot be updated")
                    case _ =>
                        Redirect(routes.PropertyController.loadPropertyManager(Some(property)))
                          .flashing("success" -> s"property with id  ${property.id.get} updated")
                }
            }
        )
    }

}
