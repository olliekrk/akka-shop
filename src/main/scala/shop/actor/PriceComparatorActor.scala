package shop.actor

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.util.Timeout
import db.ShopDatabase
import shop.message.PriceQuery.{PriceQueryAnonymous, PriceQuerySigned}
import shop.message.PriceResponse.{PriceAvailable, PriceUnavailable}
import shop.message._

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

class PriceComparatorActor(shops: Seq[ActorRef[PriceRequest]],
                           shopDb: ActorRef[PopularityRequest])
                          (implicit ctx: ActorContext[PriceQuery])
  extends AbstractBehavior[PriceQuery](ctx) {

  import PriceComparatorActor._

  private implicit val system: ActorSystem[_] = ctx.system
  private implicit val executionContext: ExecutionContextExecutor = system.executionContext

  override def onMessage(msg: PriceQuery): Behavior[PriceQuery] = {
    val popularity = shopDb.?[PopularityResponse](PopularityRequest(msg.productName, _))(1.minute, implicitly)
    msg match {
      case query: PriceQueryAnonymous => handleAnonymousQuery(query, popularity)
      case query: PriceQuerySigned => handleSignedQuery(query, popularity)
    }
    this
  }

  private def handleAnonymousQuery(query: PriceQueryAnonymous, popularityFuture: Future[PopularityResponse]): Unit = {
    val client = ctx.spawnAnonymous(ClientActor())
    checkPrices(query) onComplete {
      case Success(offers) =>
        findBestOffer(offers) match {
          case PriceUnavailable => client ! PriceUnavailable
          case PriceAvailable(price, shop, _) => popularityFuture.value match {
            case Some(Success(PopularityResponse(popularity))) =>
              client ! PriceAvailable(price, shop, Some(popularity))
            case _ =>
              ctx.log.error(s"Popularity could not be checked for product ${query.productName}.")
              client ! PriceAvailable(price, shop, None)
          }
        }
      case Failure(exception) =>
        ctx.stop(client)
        ctx.log.error("Failed to get the best price", exception)
    }
  }

  private def handleSignedQuery(query: PriceQuerySigned, popularityFuture: Future[PopularityResponse]): Unit =
    checkPrices(query) onComplete {
      case Success(offers) =>
        findBestOffer(offers) match {
          case PriceUnavailable => query.replyTo ! PriceUnavailable
          case PriceAvailable(price, shop, _) => popularityFuture.value match {
            case Some(Success(PopularityResponse(popularity))) =>
              query.replyTo ! PriceAvailable(price, shop, Some(popularity))
            case _ =>
              ctx.log.error(s"Popularity could not be checked for product ${query.productName}.")
              query.replyTo ! PriceAvailable(price, shop, None)
          }
        }
      case Failure(exception) =>
        ctx.log.error("Failed to get the best price", exception)
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
      .map(_.flatMap(offer => Future.successful(PriceAvailable(offer.price, offer.shop, None))))
      .map(_.recover(_ => PriceUnavailable))
  }

}

object PriceComparatorActor {

  private implicit val timeout: Timeout = 300.milliseconds

  def apply(): Behavior[PriceQuery] = Behaviors.setup { implicit ctx =>
    val db = ShopDatabase.create
    val shopDb = ctx.spawn(ShopDatabaseActor(db), "shopDbActor")
    val shops = Seq(
      ctx.spawn(ShopActor(), "firstShop"),
      ctx.spawn(ShopActor(), "secondShop"),
    )
    new PriceComparatorActor(shops, shopDb)
  }

}
