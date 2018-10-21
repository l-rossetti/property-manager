package controllers

import javax.inject.{Inject, Singleton}
import play.api.Logger
import play.api.mvc.{MessagesAbstractController, MessagesControllerComponents}
import services.services.PriceRepositoryImpl

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}


@Singleton
class PriceController @Inject()(repo: PriceRepositoryImpl, cc: MessagesControllerComponents)(implicit ec: ExecutionContext)
  extends MessagesAbstractController(cc) {


    import forms.PriceForm._
    import models.Price

    def getCreateUpdateRoute(price: Option[Price]) = {
        price match {
            case None => routes.PriceController.create
            case _    => routes.PriceController.update(price.get)
        }
    }

    /**
      * The load price manager action.
      */
    def loadPriceManager(propertyID: Long, price: Option[Price]) = Action.async { implicit request =>
        repo.getPropertyPrices(propertyID).map {
            prices =>  Ok(views.html.price_manager(
                preparePriceForm(price),
                propertyID,
                getCreateUpdateRoute(price),
                getFormButtonText(price),
                prices
            ))
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
                //Logger.info(propertyID.toString)
                priceForm.errors.map(e => Logger.info(e.message))
                priceForm.errors.map(e => Logger.info(e.format))
                //val propertyID = priceForm.data.get("propertyID").get.toLong
                repo.getPropertyPrices(1).map {
                    prices =>  BadRequest(views.html.price_manager(
                        errorForm,
                        1,
                        getCreateUpdateRoute(None),
                        getFormButtonText(None),
                        prices
                    ))
                }
            },
            price => {
                repo.create(price).map {
                    case Success(price) =>
                        Redirect(routes.PriceController.loadPriceManager(price.propertyID, None))
                          .flashing("success" -> s"new price created with id ${price.id.get}")
                    case Failure(e) =>
                        Redirect(routes.PriceController.loadPriceManager(price.propertyID, None))
                          .flashing("error" -> s"cannot create price ${e.getMessage}")
                }
            }
        )
    }

    /**
      * The delete price action.
      */
    def delete(id: Long, propertyID: Long) = Action.async { implicit request =>
        repo.delete(id).map {
            case false =>
                Redirect(routes.PriceController.loadPriceManager(propertyID, None))
                  .flashing("error" -> "price cannot be deleted")
            case true =>
                Redirect(routes.PriceController.loadPriceManager(propertyID, None))
                  .flashing("success" -> s"price with id ${id} has been deleted")
        }
    }

    /**
      * The update price action.
      */
    def update(price: Price) = Action.async { implicit request =>

        priceForm.bindFromRequest.fold(
            errorForm => {
                val propertyID = errorForm.data.get("propertyID").get.toLong
                repo.getPropertyPrices(propertyID).map {
                    prices =>  BadRequest(views.html.price_manager(
                        errorForm,
                        propertyID,
                        getCreateUpdateRoute(Some(price)),
                        getFormButtonText(Some(price)),
                        prices
                    ))
                }
            },
            price => {
                repo.update(price).map {
                    case None =>
                        Redirect(routes.PriceController.loadPriceManager(price.propertyID, Some(price)))
                          .flashing("error" -> s"price with id ${price.id.get} cannot be updated")
                    case _ =>
                        Redirect(routes.PriceController.loadPriceManager(price.propertyID, Some(price)))
                          .flashing("success" -> s"price with id  ${price.id.get} updated")
                }
            }
        )
    }

}

