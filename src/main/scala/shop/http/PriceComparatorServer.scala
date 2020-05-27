package shop.http

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.Http
import akka.http.scaladsl.coding.Deflate
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.{HttpApp, Route}
import akka.util.{ByteString, Timeout}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import shop.message.PriceQuery.PriceQuerySigned
import shop.message.{PriceQuery, PriceResponse}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.jdk.CollectionConverters._
import scala.util.{Success, Try}

class PriceComparatorServer(implicit val system: ActorSystem[PriceQuery]) extends HttpApp {

  import PriceComparatorServer._

  implicit val executionContextExecutor: ExecutionContextExecutor = system.executionContext
  private val http = Http(system.classicSystem)

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
            onComplete(fetchAdvantagesFromReview(productName)) {
              case Success(advantages) => complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, advantages.toString))
              case _ => complete(HttpEntity(ContentTypes.`text/plain(UTF-8)`, failureMessage))
            }
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

  private def fetchAdvantagesFromReview(productName: String): Future[List[String]] =
    http
      .singleRequest(HttpRequest(uri = reviewRequestUrl(productName)))
      .flatMap {
        _.entity.dataBytes.runFold(ByteString(""))(_ ++ _).map(_.utf8String)
      }
      .flatMap(html => Future.fromTry(findAdvantages(Jsoup.parse(html))))

  private def findAdvantages(htmlDoc: Document): Try[List[String]] = Try {
    htmlDoc
      .selectFirst(sectionSelector)
      .select(pointSelector)
      .asScala
      .map(_.text)
      .toList
  }
}

object PriceComparatorServer {

  implicit val timeout: Timeout = 3.seconds

  private val sectionSelector = ".pl_attr"

  private val pointSelector = "li"

  private val failureMessage = "Failed to fetch advantages for this product"

  def reviewRequestUrl(productName: String): String = s"https://www.opineo.pl/?szukaj=$productName&s=2"

}
