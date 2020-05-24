package shop.message

import akka.actor.typed.ActorRef

sealed trait PriceResponse

object PriceResponse {

  case class PriceOffer(price: Int, shop: ActorRef[PriceRequest]) extends PriceResponse

  case object PriceUnavailable extends PriceResponse

}

