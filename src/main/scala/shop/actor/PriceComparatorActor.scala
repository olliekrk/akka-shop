package shop.actor

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.util.Timeout
import db.ShopDatabase
import shop.message.PriceQuery.{PriceQueryAnonymous, PriceQuerySigned}
import shop.message.PriceResponse.{PriceAvailable, PriceUnavailable}
import shop.message.{PriceOffer, PriceQuery, PriceRequest, PriceResponse}

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

class PriceComparatorActor(shops: Seq[ActorRef[PriceRequest]], shopDb: ShopDatabase)
                          (implicit ctx: ActorContext[PriceQuery])
  extends AbstractBehavior[PriceQuery](ctx) {

  import PriceComparatorActor._

  private implicit val system: ActorSystem[_] = ctx.system
  private implicit val executionContext: ExecutionContextExecutor = system.executionContext

  override def onMessage(msg: PriceQuery): Behavior[PriceQuery] = {
    Future.delegate(updateProductPopularity(msg.productName))
    msg match {
      case query: PriceQueryAnonymous => handleAnonymousQuery(query)
      case query: PriceQuerySigned => handleSignedQuery(query)
    }
    this
  }

  private def handleAnonymousQuery(query: PriceQueryAnonymous): Unit = {
    val client = ctx.spawnAnonymous(ClientActor())
    checkPrices(query) onComplete {
      case Success(offers) =>
        findBestOffer(offers) match {
          case PriceUnavailable => client ! PriceUnavailable
          case PriceAvailable(price, shop, _) => getProductPopularity(query.productName) onComplete {
            case Success(popularity) => client ! PriceAvailable(price, shop, popularity)
            case Failure(exception) =>
              ctx.stop(client)
              ctx.log.error("Failed to update popularity for a product", exception)
          }
        }
      case Failure(exception) =>
        ctx.stop(client)
        ctx.log.error("Failed to get the best price", exception)
    }
  }

  private def handleSignedQuery(query: PriceQuerySigned): Unit =
    checkPrices(query) onComplete {
      case Success(offers) =>
        findBestOffer(offers) match {
          case PriceUnavailable => query.replyTo ! PriceUnavailable
          case PriceAvailable(price, shop, _) => getProductPopularity(query.productName) onComplete {
            case Success(popularity) => query.replyTo ! PriceAvailable(price, shop, popularity)
            case Failure(exception) => ctx.log.error("Failed to update popularity for a product", exception)
          }
        }
      case Failure(exception) => ctx.log.error("Failed to get the best price", exception)
    }

  private def findBestOffer(offers: Seq[PriceResponse]): PriceResponse =
    offers.foldLeft[PriceResponse](PriceUnavailable) {
      case (first, PriceUnavailable) => first
      case (PriceUnavailable, second) => second
      case (first: PriceAvailable, second: PriceAvailable) => if (first.price <= second.price) first else second
    }

  private def checkPrices(query: PriceQuery): Future[Seq[PriceResponse]] = Future.sequence {
    shops
      .map(_.?[PriceOffer](PriceRequest(query, _)))
      .map(_.flatMap(offer => Future.successful(PriceAvailable(offer.price, offer.shop, 0))))
      .map(_.recover(_ => PriceUnavailable))
  }

  private def getProductPopularity(productName: String): Future[Int] =
    shopDb.getPopularity(productName).map(_.map(_.popularity).getOrElse(0))

  private def updateProductPopularity(productName: String): Future[Int] =
    shopDb.incrementPopularity(productName).map(_.map(_.popularity).get)
}

object PriceComparatorActor {

  private implicit val timeout: Timeout = 300.milliseconds

  def apply(): Behavior[PriceQuery] = Behaviors.setup { implicit ctx =>
    val db = ShopDatabase.create
    val shops = Seq(
      ctx.spawn(ShopActor(), "firstShop"),
      ctx.spawn(ShopActor(), "secondShop"),
    )
    new PriceComparatorActor(shops, db)
  }

}
