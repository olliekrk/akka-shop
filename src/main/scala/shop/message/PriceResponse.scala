package shop.message

import akka.actor.typed.ActorRef

sealed trait PriceResponse

object PriceResponse {

  case class PriceAvailable(price: Int, shop: ActorRef[PriceRequest], popularity: Option[Int]) extends PriceResponse

  case object PriceUnavailable extends PriceResponse

}

