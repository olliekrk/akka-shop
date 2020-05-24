package shop.message

import akka.actor.typed.ActorRef
import shop.message.PriceResponse.PriceOffer

case class PriceRequest(product: ProductName, from: ActorRef[PriceOffer])
