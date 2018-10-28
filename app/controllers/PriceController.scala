package controllers

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.data.Form
import play.api.mvc.{MessagesAbstractController, MessagesControllerComponents, MessagesRequestHeader, Result}
import services.PriceRepository

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}


@Singleton
class PriceController @Inject()(repo: PriceRepository, cc: MessagesControllerComponents)(implicit ec: ExecutionContext)
  extends MessagesAbstractController(cc) {


    import forms.PriceForm._
    import models.Price

    /**
      * Handle services results in case of valid form
      *
      * @param result
      * @param actionType shown in the message
      * @return the HTML view
      */
    private def handleServiceResult(propertyID: Long, result: Future[Try[Any]], actionType: Actions.ActionName): Future[Result] = {
        result.map {
            case Failure(exc) =>
                Redirect(routes.PriceController.loadPriceManager(propertyID, None))
                  .flashing("error" -> s"cannot ${actionType} price: ${exc.getMessage}")
            case Success(_) =>
                Redirect(routes.PriceController.loadPriceManager(propertyID, None))
                  .flashing("success" -> s"price ${actionType}d")
        }
    }

    /**
      * Handle the case of validation errors for the price form
      *
      * @param errorForm form filled with errors
      * @param result the list of prices to populate the table
      * @param price optional Price (None in Create flow)
      * @return the HTML view
      */
    private def handleFormError(errorForm: Form[Price], result: Future[Try[Seq[Price]]], propertyID: Long, price: Option[Price])
                               (implicit request: MessagesRequestHeader): Future[Result] = {
        result.map {
            case Failure(exc) =>
                BadRequest(getPriceManagerView(propertyID, errorForm, Seq[Price](), price))
                  .flashing("error" -> s"error while getting list of prices: ${exc.getMessage}")
            case Success(prices) =>
                BadRequest(getPriceManagerView(propertyID, errorForm, prices, price))
        }
    }

    /**
      * Build the price-manager view filled with data
      *
      * @param priceForm handles both success and error form cases
      * @param prices the list of prices to populate the table
      * @param price optional Price (None in Create flow)
      * @return the HTML view
      */
    private def getPriceManagerView(propertyID: Long, priceForm: Form[Price], prices: Seq[Price], price: Option[Price])
                                      (implicit request: MessagesRequestHeader) = {
        views.html.price_manager(
            priceForm,
            propertyID,
            getCreateUpdateRoute(price),
            getFormButtonText(price),
            prices
        )(request)
    }

    /**
      * The load price manager action.
      */
    def loadPriceManager(propertyID: Long, price: Option[Price]) = Action.async { implicit request =>

        repo.getPropertyPrices(propertyID).map {
            case Success(prices) =>
                Ok(getPriceManagerView(propertyID, preparePriceForm(price), prices, price))

            case Failure(exc) =>
                BadRequest(getPriceManagerView(propertyID, preparePriceForm(price), Seq[Price](), price))
                  .flashing("error" -> s"cannot load prices ${exc.getMessage}")
        }
    }

    /**
      * The add price action.
      *
      * This is asynchronous, since we're invoking the asynchronous methods on PriceRepository.
      */
    def create = Action.async { implicit request =>
        // Bind the form first, then fold the result, passing a function to handle errors, and a function to handle success.
        priceForm.bindFromRequest.fold (

            errorForm => {
                errorForm.errors.map(e => Logger.info(e.key))
                errorForm.errors.map(e => Logger.info(e.message))
                errorForm.errors.map(e => Logger.info(e.format))
                val propertyID = errorForm.data.get("propertyID").get.toLong
                handleFormError(errorForm, repo.getPropertyPrices(propertyID), propertyID, None)
            },
            price => {
                handleServiceResult(price.propertyID, repo.create(price), Actions.Create)
            }
        )
    }

    /**
      * The delete price action.
      */
    def delete(id: Long, propertyID: Long) = Action.async { implicit request =>
        handleServiceResult(propertyID, repo.delete(id), Actions.Delete)
    }

    /**
      * The update price action.
      */
    def update(price: Price) = Action.async { implicit request =>

        priceForm.bindFromRequest.fold(
            errorForm => {
                val propertyID = errorForm.data.get("propertyID").get.toLong
                handleFormError(errorForm, repo.getPropertyPrices(propertyID), propertyID, Some(price))
            },
            price => {
                handleServiceResult(price.propertyID, repo.update(price), Actions.Update)
            }
        )
    }

    object Actions extends Enumeration {
        type ActionName = Value
        val Create  = Value("create")
        val Update  = Value("update")
        val Delete  = Value("delete")
    }

    def getCreateUpdateRoute(price: Option[Price]) = {
        price match {
            case None => routes.PriceController.create
            case _    => routes.PriceController.update(price.get)
        }
    }
}

