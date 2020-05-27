package app

import akka.actor.typed.ActorSystem
import shop.actor.PriceComparatorActor
import shop.cli.PriceComparatorCli
import shop.http.PriceComparatorServer
import shop.message.PriceQuery

object PriceComparator extends App {

  implicit val actorSystem: ActorSystem[PriceQuery] = ActorSystem(PriceComparatorActor(), "priceComparator")
  val cli: PriceComparatorCli = new PriceComparatorCli()
  val server: PriceComparatorServer = new PriceComparatorServer()

  cli.runAsync()
  server.startServer("localhost", 9090, actorSystem.classicSystem)

}
