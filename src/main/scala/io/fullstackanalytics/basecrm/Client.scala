package io.fullstackanalytics.basecrm


import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{BasicHttpCredentials, ModeledCustomHeader, ModeledCustomHeaderCompanion}
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.ByteString
import play.api.libs.json._

import scala.concurrent._
import scala.util.Try

trait Client {

  val TOKEN = "9bf20ada1548ada39f84e3262e8541d7bf50be4605f599257413f4ebd91e4bdd"
  val HOST = "api.getbase.com"

  import HttpMethods._
  import HttpProtocols._
  import MediaTypes._

  val contentType: HttpHeader = headers.`Content-Type`(`application/json`)
  val auth = (t: String) => headers.Authorization(headers.OAuth2BearerToken(""))
  val uuidHeader = (id: Uuid) => BaseCrmHeader(id)

  def start
    (host: String, token: String)(id: Uuid)
    (implicit s: ActorSystem, materializer: ActorMaterializer, ec: ExecutionContext) : Future[SessionId] =
    Http().singleRequest(
      HttpRequest(
        POST,
        uri = Uri() withScheme "http" withHost host withPath Uri.Path("/v2/sync/start"),
        protocol = `HTTP/1.1`,
        headers = List(auth(token), uuidHeader(id))
      )
    ) flatMap {
      case HttpResponse(StatusCodes.OK, _, entity, _) =>
        entity.dataBytes.runFold(ByteString(""))(_ ++ _) map (content => {
          val p = Json.parse(content.decodeString(ByteString.UTF_8)).as[JsObject]
          (p \ "data" \ "id").validate[String].get
        })
      case HttpResponse(sc, _, _, _) =>
        Future.failed(new Exception(s"received status $sc"))
    }

  def fetch
    (host: String, token: String)(id: Uuid, sid: SessionId)
    (implicit s: ActorSystem, materializer: ActorMaterializer, ec: ExecutionContext) : Future[(StatusCode, Result)] =
    Http().singleRequest(
      HttpRequest(
        POST,
        uri = Uri() withScheme "https" withHost host withPath Uri.Path(s"/v2/sync/$sid/queues/main"),
        protocol = `HTTP/1.1`,
        headers = List(auth(token), uuidHeader(id))
      )
    ) flatMap {
      case HttpResponse(StatusCodes.OK, _, entity, _) =>
        entity.dataBytes.runFold(ByteString(""))(_ ++ _) map (content => {
          val p = Json.parse(content.decodeString(ByteString.UTF_8)).as[JsObject]
          val rows: Result = (p \ "items").validate[List[String]].get
          (StatusCodes.OK, rows)
        })
      case HttpResponse(sc, _, _, _) =>
        Future.failed(new Exception(s"received status $sc"))
    }

  def ack(host: String, token: String)(id: Uuid, sid: SessionId): Future[StatusCode] = ???

  final class BaseCrmHeader(token: String) extends ModeledCustomHeader[BaseCrmHeader] {
    override def renderInRequests = false
    override def renderInResponses = false
    override val companion = BaseCrmHeader
    override def value: String = token
  }
  object BaseCrmHeader extends ModeledCustomHeaderCompanion[BaseCrmHeader] {
    override val name = "X-Basecrm-Device-UUID"
    override def parse(value: String) = Try(new BaseCrmHeader(value))
  }
}
