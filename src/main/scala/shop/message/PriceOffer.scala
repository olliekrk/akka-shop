package shop.message

import akka.actor.typed.ActorRef

case class PriceOffer(price: Int, shop: ActorRef[PriceRequest])
