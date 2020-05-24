package app

import akka.actor.typed.ActorSystem
import shop.actor.PriceComparatorActor
import shop.cli.PriceComparatorCli
import shop.message.ProductName

object PriceComparator extends App {

  val priceComparator: ActorSystem[ProductName] = ActorSystem(PriceComparatorActor(), "priceComparator")
  val cli: PriceComparatorCli = new PriceComparatorCli(priceComparator)

  cli.runAsync()

}
