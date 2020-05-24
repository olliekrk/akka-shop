package shop.actor

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.util.Timeout
import shop.message.PriceResponse.{PriceOffer, PriceUnavailable}
import shop.message.{PriceRequest, PriceResponse, ProductName}

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

class PriceComparatorActor(shops: Seq[ActorRef[PriceRequest]], ctx: ActorContext[ProductName])
  extends AbstractBehavior[ProductName](ctx) {

  import PriceComparatorActor._

  private implicit val system: ActorSystem[_] = ctx.system
  private implicit val executionContext: ExecutionContextExecutor = system.executionContext

  override def onMessage(msg: ProductName): Behavior[ProductName] = {
    val client = ctx.spawnAnonymous(ClientActor())
    Future.sequence {
      shops
        .map(_.?[PriceResponse](PriceRequest(msg, _)))
        .map(_.recover(_ => PriceUnavailable))
    } onComplete {
      case Success(offers) =>
        val bestOffer = offers.foldLeft[PriceResponse](PriceUnavailable) {
          case (first, PriceUnavailable) => first
          case (PriceUnavailable, second) => second
          case (first: PriceOffer, second: PriceOffer) => if (first.price <= second.price) first else second
        }
        client ! bestOffer
      case Failure(exception) =>
        ctx.log.error("Failed to get the best price.", exception)
    }
    this
  }
}

object PriceComparatorActor {

  private implicit val timeout: Timeout = 300 milliseconds

  def apply(): Behavior[ProductName] = Behaviors.setup { ctx =>
    val shops = List(ctx.spawn(ShopActor(), "firstShop"), ctx.spawn(ShopActor(), "secondShop"))
    new PriceComparatorActor(shops, ctx)
  }

}
