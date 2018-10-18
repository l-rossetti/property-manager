package services

import javax.inject.{Inject, Singleton}
import models.Property
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.jdbc.meta.MTable

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}


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
        def * = (id, address, postCode, latitude, longitude, surface, bedRoomCount) <> ((Property.apply _).tupled, Property.unapply)

    }

    /**
      * The starting point for all queries on the properties table.
      */
    private val properties = TableQuery[PropertyTable]
    private val tables = List(properties)
    private val existing = db.run(MTable.getTables)
    val f = existing.flatMap( v => {
        val names = v.map(mt => mt.name.name)
        val createIfNotExist = tables.filter( table =>
            (!names.contains(table.baseTableRow.tableName))).map(_.schema.create)
        db.run(DBIO.sequence(createIfNotExist))
    })
    Await.result(f, Duration.Inf)

    /**
      * Create a property with the given name and age.
      *
      * This is an asynchronous operation, it will return a future of the created property, which can be used to obtain the
      * id for that property.
      */
    def create(address: String,
               postCode: Int,
               latitude: Double,
               longitude: Double,
               surface: Option[Int],
               bedRoomCount: Option[Int]
              ): Future[Property] = db.run {
        // We create a projection of just the non primary columns, since we're not inserting a value for the id column
        (properties.map(p => (p.address, p.postCode, p.latitude, p.longitude, p.surface, p.bedRoomCount))
          // Now define it to return the id, because we want to know what id was generated for the property
          returning properties.map(_.id)
          // And we define a transformation for the returned value, which combines our original parameters with the
          // returned id
          into ((params, id) => Property(id, params._1, params._2, params._3, params._4, params._5, params._6))
          // And finally, insert the person into the database
          ) += (address, postCode, latitude, longitude, surface, bedRoomCount)
    }

    /**
      * List all the properties in the database.
      */
    def list(): Future[Seq[Property]] = db.run {
        properties.result
    }
}

trait PropertyRepository {
    def create(address: String, postCode: Int, latitude: Double, longitude: Double, surface: Option[Int], bedRoomCount: Option[Int]): Future[Property]
    def list(): Future[Seq[Property]]
}
