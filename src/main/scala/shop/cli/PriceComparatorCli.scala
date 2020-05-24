package shop.cli

import akka.actor.typed.ActorSystem
import shop.message.PriceQuery
import shop.message.PriceQuery.PriceQueryAnonymous

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.StdIn

class PriceComparatorCli(implicit val system: ActorSystem[PriceQuery]) {

  def runAsync(): Future[Unit] = Future(run())

  def run(): Unit =
    while (true) {
      println("Check price for next product:")
      system ! PriceQueryAnonymous(StdIn.readLine)
    }

}
