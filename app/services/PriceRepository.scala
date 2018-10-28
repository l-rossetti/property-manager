package services

import javax.inject.{Inject, Singleton}
import models.{Price}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.jdbc.meta.MTable

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Try


/**
  * A repository for prices.
  *
  * @param dbConfigProvider The Play db config provider. Play will inject this.
  */
@Singleton
class PriceRepositoryImpl @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends PriceRepository {

    private val dbConfig = dbConfigProvider.get[JdbcProfile]


    // These imports are important, the first one brings db into scope, which will let you do the actual db operations.
    // The second one brings the Slick DSL into scope, which lets you define the table and other queries.
    import dbConfig._
    import profile.api._
    /**
      * The starting point for all queries on the price table.
      */
    private val prices = TableQuery[PriceTable]

    /**
      * Here we define the table.
      */
    private class PriceTable(tag: Tag) extends Table[Price](tag, "prices") {

        /** The ID column, which is the primary key, and auto incremented */
        def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
        /** The propertyID column */
        def propertyID = column[Long]("propertyID")
        /** The bedRoomCount column */
        def price = column[Double]("price")
        /** The bedRoomCount column */
        def date = column[String]("date")

        /**
          * This is the tables default "projection".
          *
          * It defines how the columns are converted to and from the Price object.
          */
        def * = (id.?, propertyID, price, date) <> ((Price.apply _).tupled, Price.unapply)
    }

    /**
      * Table creation
      */
    private val existing = db.run(MTable.getTables)
    val f = existing.flatMap( v => {
        val names = v.map(mt => mt.name.name)
        val createIfNotExist = List(prices)
          .filter( table => (!names.contains(table.baseTableRow.tableName))).map(_.schema.create)
        db.run(DBIO.sequence(createIfNotExist))
    })
    Await.result(f, Duration.Inf)

    /**
      * CRUD
      */
    def create(price: Price): Future[Try[Price]] = db.run {
        val insertQuery = prices returning prices.map(_.id) into ((price, id) => price.copy(id = Some(id)))
        (insertQuery += price).asTry
    }

    def update(price: Price): Future[Option[Price]] = db.run {
        prices.filter(_.id === price.id).update(price).map {
            case 0 => None
            case 1 => Some(price)
        }
    }

    def delete(id: Long): Future[Boolean] = db.run {
        prices.filter(_.id === id).delete.map {
            case 0 => false
            case _ => true
        }
    }

    def getPropertyPrices(propertyId: Long): Future[Seq[Price]] = db.run {
        prices.filter(_.propertyID === propertyId).result
    }
}

trait PriceRepository {
    /**
      * Create a price with the mandatory parameters.
      *
      * This is an asynchronous operation, it will return a future of the created price, which can be used to obtain the
      * id for that price.
      */
    def create(price: Price): Future[Try[Price]]

    /**
      * Update a price
      */
    def update(price: Price): Future[Option[Price]]
    /**
      * Delete a price by id
      */
    def delete(id: Long): Future[Boolean]
    /**
      * List all the prices for a given propertyID.
      */
    def getPropertyPrices(propertyId: Long): Future[Seq[Price]]
}
