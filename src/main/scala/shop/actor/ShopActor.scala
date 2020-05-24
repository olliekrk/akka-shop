package shop.actor

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import shop.message.{PriceOffer, PriceRequest}

import scala.concurrent.duration._
import scala.util.Random

class ShopActor(ctx: ActorContext[PriceRequest]) extends AbstractBehavior[PriceRequest](ctx) {

  override def onMessage(msg: PriceRequest): Behavior[PriceRequest] = {
    val delay = Random.between(100, 501).milliseconds
    val response = PriceOffer(Random.between(1, 11), ctx.self)
    ctx.log.info(s"Price for ${msg.query.productName} is ${response.price}. Took $delay ms.")
    ctx.scheduleOnce(delay, msg.replyTo, response)
    this
  }

}

object ShopActor {

  def apply(): Behavior[PriceRequest] = Behaviors.setup(new ShopActor(_))

}
