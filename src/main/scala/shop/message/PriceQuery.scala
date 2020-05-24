package shop.message

import akka.actor.typed.ActorRef

sealed trait PriceQuery {
  def productName: String
}

object PriceQuery {

  case class PriceQueryAnonymous(productName: String) extends PriceQuery

  case class PriceQuerySigned(productName: String, replyTo: ActorRef[PriceResponse]) extends PriceQuery

}
