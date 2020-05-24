package shop.actor

import akka.actor.TypedActor
import akka.actor.typed.internal.PoisonPill
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.util.Timeout
import db.ShopDatabase
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
    val client = ctx.spawnAnonymous(ClientActor())
    Future.sequence {
      shops
        .map(_.?[PriceOffer](PriceRequest(msg, _)))
        .map(_.flatMap(offer => Future.successful(PriceAvailable(offer.price, offer.shop, 0))))
        .map(_.recover(_ => PriceUnavailable))
    } onComplete {
      case Success(offers) =>
        offers.foldLeft[PriceResponse](PriceUnavailable) {
          case (first, PriceUnavailable) => first
          case (PriceUnavailable, second) => second
          case (first: PriceAvailable, second: PriceAvailable) => if (first.price <= second.price) first else second
        } match {
          case PriceAvailable(price, shop, _) =>
            updateProductPopularity(msg.productName) onComplete {
              case Success(popularity) => client ! PriceAvailable(price, shop, popularity)
              case Failure(exception) =>
                ctx.stop(client)
                ctx.log.error("Failed to update popularity for a product", exception)
            }
          case _ =>
            client ! PriceUnavailable
        }
      case Failure(exception) =>
        ctx.stop(client)
        ctx.log.error("Failed to get the best price", exception)
    }
    this
  }

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
