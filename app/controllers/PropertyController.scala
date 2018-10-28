package controllers


import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.data.{Form}
import play.api.mvc._
import services.PropertyRepository

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class PropertyController @Inject() (repo: PropertyRepository,
                                    cc: MessagesControllerComponents
                                   ) (implicit ec: ExecutionContext)
  extends MessagesAbstractController(cc) {


    import forms.PropertyForm._
    import models.Property

    /**
      * Handle services results in case of valid form
      *
      * @param result
      * @param actionType shown in the message
      * @return the HTML view
      */
    private def handleServiceResult(result: Future[Try[Any]], actionType: Actions.ActionName): Future[Result] = {
        result.map {
            case Failure(exc) =>
                Redirect(routes.PropertyController.loadPropertyManager(None))
                  .flashing("error" -> s"cannot ${actionType} property: ${exc.getMessage}")
            case Success(_) =>
                Redirect(routes.PropertyController.loadPropertyManager(None))
                  .flashing("success" -> s"property ${actionType}d")
        }
    }

    /**
      * Handle the case of validation errors for the property form
      *
      * @param errorForm form filled with errors
      * @param result the list of properties to populate the table
      * @param property optional Property (None in Create flow)
      * @return the HTML view
      */
    private def handleFormError(errorForm: Form[Property], result: Future[Try[Seq[Property]]], property: Option[Property])
                               (implicit request: MessagesRequestHeader): Future[Result] = {
        result.map {
            case Failure(exc) =>
                BadRequest(getPropertyManagerView(errorForm, Seq[Property](), property))
                  .flashing("error" -> s"error while getting list of properties: ${exc.getMessage}")
            case Success(properties) =>
                BadRequest(getPropertyManagerView(errorForm, properties, property))
        }
    }

    /**
      * Build the property-manager view filled with data
      *
      * @param propertyForm handles both success and error form cases
      * @param properties the list of properties to populate the table
      * @param property optional Property (None in Create flow)
      * @return the HTML view
      */
    private def getPropertyManagerView(propertyForm: Form[Property], properties: Seq[Property], property: Option[Property])
                                      (implicit request: MessagesRequestHeader) = {
        views.html.property_manager(
            propertyForm,
            getCreateUpdateRoute(property),
            getFormButtonText(property),
            properties
        )(request)
    }

    /**
      * The load property manager action.
      */
    def loadPropertyManager(property: Option[Property]) = Action.async { implicit request =>
        repo.getProperties().map {
            case Success(properties) =>
                Ok(getPropertyManagerView(preparePropertyForm(property), properties, property))

            case Failure(exc) =>
                BadRequest(getPropertyManagerView(preparePropertyForm(property), Seq[Property](), property))
                  .flashing("error" -> s"cannot load properties ${exc.getMessage}")

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
                handleFormError(errorForm, repo.getProperties(), None)
            },
            property => {
                handleServiceResult(repo.create(property), Actions.Create)
            }
        )
    }

    /**
      * The delete property action.
      */
    def delete(id: Long) = Action.async { implicit request =>
        handleServiceResult(repo.delete(id), Actions.Delete)
        // TODO deleteProperty should cascade into deletePricesForProperty
    }

    /**
      * The update property action.
      */
    def update(property: Property) = Action.async { implicit request =>

        propertyForm.bindFromRequest.fold(
            errorForm => {
                handleFormError(errorForm, repo.getProperties(), Some(property))
            },
            property => {
                handleServiceResult(repo.update(property), Actions.Update)
            }
        )
    }

    object Actions extends Enumeration {
        type ActionName = Value
        val Create  = Value("create")
        val Update  = Value("update")
        val Delete  = Value("delete")
    }

    private def getCreateUpdateRoute(property: Option[Property]) = {
        property match {
            case None => routes.PropertyController.create
            case _    => routes.PropertyController.update(property.get)
        }
    }

}
