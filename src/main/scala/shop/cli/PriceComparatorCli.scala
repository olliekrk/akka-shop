package shop.cli

import akka.actor.typed.ActorSystem
import shop.message.PriceQuery

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.StdIn

class PriceComparatorCli(priceComparator: ActorSystem[PriceQuery]) {

  def runAsync(): Future[Unit] = Future(run())

  def run(): Unit =
    while (true) {
      println("Check price for next product:")
      priceComparator ! PriceQuery(StdIn.readLine)
    }

}
