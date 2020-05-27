package db

import slick.jdbc.SQLiteProfile.api._
import slick.lifted.ProvenShape

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

class ShopDatabase() {

  import ShopDatabase._

  val db = Database.forConfig("shopdb")
  val products: TableQuery[Products] = TableQuery[Products]

  def getPopularity(productName: String)(implicit ctx: ExecutionContext): Future[Option[DBProduct]] = db.run {
    products.filter(_.name === productName).take(1).result.headOption
  }

  def incrementPopularity(productName: String)(implicit ctx: ExecutionContext): Future[Option[DBProduct]] = db.run {
    {
      for {
        existing <- products.filter(_.name === productName).result.headOption
        _ <- existing match {
          case Some(DBProduct(_, popularity)) => products.insertOrUpdate(DBProduct(productName, popularity + 1))
          case _ => products += DBProduct(productName, 1)
        }
        updated <- products.filter(_.name === productName).result.headOption
      } yield updated
    }.transactionally
  }

  private def setup: Future[Unit] = db.run {
    DBIO.seq {
      products.schema.createIfNotExists
    }
  }

}

object ShopDatabase {

  case class DBProduct(name: String, popularity: Int)

  class Products(tag: Tag) extends Table[DBProduct](tag, "PRODUCTS") {

    def name: Rep[String] = column[String]("NAME", O.PrimaryKey)

    def popularity: Rep[Int] = column[Int]("POPULARITY")

    def * : ProvenShape[DBProduct] = (name, popularity) <> (DBProduct.tupled, DBProduct.unapply)

  }

  def create: ShopDatabase = {
    val db = new ShopDatabase()
    Await.result(db.setup, Duration.Inf)
    db
  }

}
