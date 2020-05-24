package shop.http

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.Http
import akka.http.scaladsl.coding.Deflate
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.{HttpApp, Route}
import akka.util.Timeout
import shop.message.PriceQuery.PriceQuerySigned
import shop.message.{PriceQuery, PriceResponse}

import scala.concurrent.duration._

class PriceComparatorServer(implicit val system: ActorSystem[PriceQuery]) extends HttpApp {

  implicit val timeout: Timeout = 3.seconds

  override def routes: Route =
    concat(
      pathPrefix("price") {
        path(Segment) { productName =>
          encodeResponseWith(Deflate) {
            onSuccess(system.ask[PriceResponse](PriceQuerySigned(productName, _))) { response =>
              complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, response.toString))
            }
          }
        }
      },
      pathPrefix("review") {
        path(Segment) { productName =>
          encodeResponseWith(Deflate) {
            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
          }
        }
      }
    )

  override protected def postHttpBinding(binding: Http.ServerBinding): Unit = {
    super.postHttpBinding(binding)
    val sys = systemReference.get()
    sys.log.info(s"Running on [${sys.name}] actor system")
  }

  override protected def postHttpBindingFailure(cause: Throwable): Unit = {
    println(s"The server could not be started due to $cause")
  }

}
