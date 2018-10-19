package services

import javax.inject.{Inject, Singleton}
import models.Property
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.jdbc.meta.MTable

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}


/**
  * A repository for properties.
  *
  * @param dbConfigProvider The Play db config provider. Play will inject this.
  */
@Singleton
class PropertyRepositoryImpl @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends PropertyRepository {

    private val dbConfig = dbConfigProvider.get[JdbcProfile]


    // These imports are important, the first one brings db into scope, which will let you do the actual db operations.
    // The second one brings the Slick DSL into scope, which lets you define the table and other queries.
    import dbConfig._
    import profile.api._
    /**
      * The starting point for all queries on the properties table.
      */
    private val properties = TableQuery[PropertyTable]
    private val existing = db.run(MTable.getTables)
    val f = existing.flatMap( v => {
        val names = v.map(mt => mt.name.name)
        val createIfNotExist = List(properties).filter( table =>
            (!names.contains(table.baseTableRow.tableName))).map(_.schema.create)
        db.run(DBIO.sequence(createIfNotExist))
    })
    Await.result(f, Duration.Inf)

    /**
      * Here we define the table.
      */
    private class PropertyTable(tag: Tag) extends Table[Property](tag, "properties") {

        /** The ID column, which is the primary key, and auto incremented */
        def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

        /** The address column */
        def address = column[String]("address")

        /** The postCode column */
        def postCode = column[Int]("postCode")

        /** The latitude column */
        def latitude = column[Double]("latitude")

        /** The longitude column */
        def longitude = column[Double]("longitude")

        /** The surface column */
        def surface = column[Option[Int]]("surface", O.Default(None))

        /** The bedRoomCount column */
        def bedRoomCount = column[Option[Int]]("bedRoomCount", O.Default(None))

        /**
          * This is the tables default "projection".
          *
          * It defines how the columns are converted to and from the Property object.
          */
        def * = (id.?, address, postCode, latitude, longitude, surface, bedRoomCount) <> ((Property.apply _).tupled, Property.unapply)
    }

    /** CRUD */
    def create(property: Property): Future[Try[Property]] = db.run {
        val insertQuery = properties returning properties.map(_.id) into ((property, id) => property.copy(id = Some(id)))
        (insertQuery += property).asTry
    }

    def update(property: Property): Future[Option[Property]] = db.run {
        properties.filter(_.id === property.id).update(property).map {
            case 0 => None
            case 1 => Some(property)
        }
    }

    def delete(id: Long): Future[Boolean] = db.run {
        properties.filter(_.id === id).delete.map {
            case 0 => false
            case _ => true
        }
    }

    def getProperties(): Future[Seq[Property]] = db.run {
        properties.result
    }
}

trait PropertyRepository {
    /**
      * Create a property with the mandatory parameters.
      *
      * This is an asynchronous operation, it will return a future of the created property, which can be used to obtain the
      * id for that property.
      */
    def create(property: Property): Future[Try[Property]]

    /**
      * Update a property
      */
    def update(property: Property): Future[Option[Property]]
    /**
      * Delete a property by id
      */
    def delete(id: Long): Future[Boolean]
    /**
      * List all the properties in the database.
      */
    def getProperties(): Future[Seq[Property]]
}
