package controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc.{MessagesAbstractController, MessagesControllerComponents}
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.data.format.Formats._
import play.api.libs.json.Json

import services.PropertyRepository

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PropertyController @Inject() (repo: PropertyRepository,
                                    cc: MessagesControllerComponents
                                   ) (implicit ec: ExecutionContext)
  extends MessagesAbstractController(cc) {

    /**
      * The mapping for the property form.
      */
    val propertyForm: Form[CreatePropertyForm] = Form {
        mapping(
            "address"       -> nonEmptyText,
            "postCode"      -> number.verifying(min(0)),
            "latitude"      -> of(doubleFormat),
            "longitude"     -> of(doubleFormat),
            "surface"       -> optional(number.verifying(min(0))),
            "bedRoomCount"  -> optional(number.verifying(min(0)))
        )(CreatePropertyForm.apply)(CreatePropertyForm.unapply)
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
        repo.list().map { property =>
            Ok(Json.toJson(property))
        }
    }

    /**
      * The add property action.
      *
      * This is asynchronous, since we're invoking the asynchronous methods on PropertyRepository.
      */
    def addProperty = Action.async { implicit request =>
        // Bind the form first, then fold the result, passing a function to handle errors, and a function to handle succes.
        propertyForm.bindFromRequest.fold(
            // The error function. We return the index page with the error form, which will render the errors.
            // We also wrap the result in a successful future, since this action is synchronous, but we're required to return
            // a future because the property creation function returns a future.
            errorForm => {
                Future.successful(Ok("error"/*views.html.index(errorForm)*/))
            },
            // There were no errors in the from, so create the person.
            property => {
                repo.create(property.address, property.postCode, property.latitude, property.longitude, property.surface, property.bedRoomCount).map { _ =>
                    // If successful, we simply redirect to the index page.
                    Redirect(routes.PropertyController.manage()).flashing("success" -> "property.created")
                }
            }
        )
    }
    // needed to return async results
    import play.api.libs.concurrent.Execution.Implicits.defaultContext

 /*   /**
      * Create a list of transactions, asynchronously.
      */
    def list = Action.async {
        val transactionsAsFuture = scala.concurrent.Future{ Property.find() }
        transactionsAsFuture.map(transactions => Ok(Json.toJson(transactions)))
    }

    def create = Action { implicit request =>
        transactionForm.bindFromRequest.fold(
            errors => {
                Ok(Json.toJson(Map("success" -> toJson(false), "msg" -> toJson("Boom!"), "id" -> toJson(0))))
            },
            transaction => {
                val id = Property.save(transaction)
                id match {
                    case Some(autoIncrementId) =>
                        val result = Map("success" -> toJson(true), "msg" -> toJson("Success!"), "id" -> toJson(autoIncrementId))
                        Ok(Json.toJson(result))
                    case None =>
                        // TODO inserts can fail; i need to handle this properly.
                        val result = Map("success" -> toJson(true), "msg" -> toJson("Success!"), "id" -> toJson(-1))
                        Ok(Json.toJson(result))
                }

            }
        )
    }*/
}

/**
  * The create property form.
  *
  * Generally for forms, you should define separate objects to your models, since forms very often need to present data
  * in a different way to your models.  In this case, it doesn't make sense to have an id parameter in the form, since
  * that is generated once it's created.
  */
case class CreatePropertyForm(address: String,
                              postCode: Int,
                              latitude: Double,
                              longitude: Double,
                              surface: Option[Int],
                              bedRoomCount: Option[Int])