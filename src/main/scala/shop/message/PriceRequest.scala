package shop.message

import akka.actor.typed.ActorRef

case class PriceRequest(query: PriceQuery, from: ActorRef[PriceOffer])
