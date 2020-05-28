package shop.message

import akka.actor.typed.ActorRef

case class PopularityRequest(productName: String, replyTo: ActorRef[PopularityResponse])
