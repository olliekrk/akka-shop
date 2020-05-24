package shop.actor

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import shop.message.PriceResponse

class ClientActor(ctx: ActorContext[PriceResponse]) extends AbstractBehavior[PriceResponse](ctx) {

  override def onMessage(msg: PriceResponse): Behavior[PriceResponse] = msg match {
    case PriceResponse.PriceOffer(price, shop) =>
      ctx.log.info(s"Received response. The best price is: $price from shop: $shop")
      this
    case PriceResponse.PriceUnavailable =>
      ctx.log.warn(s"Failed to get the best price.")
      Behaviors.stopped
  }

}

object ClientActor {

  def apply(): Behavior[PriceResponse] = Behaviors.setup(new ClientActor(_))

}
