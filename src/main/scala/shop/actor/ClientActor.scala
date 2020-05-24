package shop.actor

import akka.actor.typed.{Behavior, PostStop, Signal}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import shop.message.PriceResponse
import shop.message.PriceResponse.{PriceAvailable, PriceUnavailable}

class ClientActor(ctx: ActorContext[PriceResponse]) extends AbstractBehavior[PriceResponse](ctx) {

  override def onMessage(msg: PriceResponse): Behavior[PriceResponse] = msg match {
    case PriceAvailable(price, shop, popularity) =>
      ctx.log.info(s"Received response. The best price is: $price from shop: $shop. Query popularity: $popularity")
      Behaviors.stopped
    case PriceUnavailable =>
      ctx.log.warn(s"Failed to get the best price.")
      Behaviors.stopped
  }

  override def onSignal: PartialFunction[Signal, Behavior[PriceResponse]] = {
    case _: PostStop =>
      ctx.log.info(s"Client $this has terminated")
      Behaviors.same
    case _ =>
      Behaviors.ignore
  }
}

object ClientActor {

  def apply(): Behavior[PriceResponse] = Behaviors.setup(new ClientActor(_))

}
