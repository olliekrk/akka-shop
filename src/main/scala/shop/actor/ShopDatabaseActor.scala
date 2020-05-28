package shop.actor


import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import db.ShopDatabase
import shop.message.{PopularityRequest, PopularityResponse}

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.Success

class ShopDatabaseActor(shopDb: ShopDatabase)
                       (implicit ctx: ActorContext[PopularityRequest]) extends AbstractBehavior[PopularityRequest](ctx) {

  private implicit val executionContext: ExecutionContextExecutor = ctx.system.executionContext

  override def onMessage(msg: PopularityRequest): Behavior[PopularityRequest] = {
    updateProductPopularity(msg.productName) onComplete {
      case Success(popularity) => msg.replyTo ! PopularityResponse(popularity)
      case _ =>
    }
    this
  }

  private def updateProductPopularity(productName: String): Future[Int] =
    shopDb.incrementPopularity(productName).map(_.map(_.popularity).get)

}

object ShopDatabaseActor {

  def apply(shopDatabase: ShopDatabase): Behavior[PopularityRequest] =
    Behaviors.setup(new ShopDatabaseActor(shopDatabase)(_))

}
