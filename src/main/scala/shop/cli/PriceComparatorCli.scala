package shop.cli

import akka.actor.typed.ActorSystem
import shop.message.ProductName

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.StdIn

class PriceComparatorCli(priceComparator: ActorSystem[ProductName]) {

  def runAsync(): Future[Unit] = Future(run())

  def run(): Unit =
    while (true) {
      println("Check price for next product:")
      priceComparator ! ProductName(StdIn.readLine)
    }

}
