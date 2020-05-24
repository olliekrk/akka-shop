package app

import akka.actor.typed.ActorSystem
import shop.actor.PriceComparatorActor
import shop.cli.PriceComparatorCli
import shop.message.PriceQuery

object PriceComparator extends App {

  val priceComparator: ActorSystem[PriceQuery] = ActorSystem(PriceComparatorActor(), "priceComparator")
  val cli: PriceComparatorCli = new PriceComparatorCli(priceComparator)

  cli.runAsync()

}
