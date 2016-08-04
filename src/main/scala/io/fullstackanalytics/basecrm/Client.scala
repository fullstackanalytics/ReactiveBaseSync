package io.fullstackanalytics.basecrm


import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.stream.scaladsl._
import akka.stream.ActorMaterializer
import akka.util.ByteString
import play.api.libs.json._

import scala.concurrent._
import scala.util.Try
import java.io.InputStream
import java.security.{KeyStore, SecureRandom}
import java.security.cert.{Certificate, CertificateFactory}
import javax.net.ssl.{KeyManagerFactory, SSLContext, SSLParameters, TrustManagerFactory}

import akka.http.scaladsl.Http.OutgoingConnection
import java.util.UUID

trait Client {

  import HttpMethods._
  import HttpProtocols._
  import MediaTypes._

  def start
    (conn: Flow[HttpRequest, HttpResponse, Future[OutgoingConnection]], token: String, deviceId: UUID)
    (implicit s: ActorSystem, materializer: ActorMaterializer, ec: ExecutionContext)
  : Future[SessionId] = {
    Source.single(
      HttpRequest(
        POST,
        uri = "/v2/sync/start",
        protocol = `HTTP/1.1`,
        headers = List(
          headers.Authorization(headers.OAuth2BearerToken(token)),
          RawHeader("Accept", "application/json"),
          RawHeader("X-Basecrm-Device-UUID", deviceId.toString)
        )
      )
    ).via(conn).runWith(Sink.head) flatMap {
      case HttpResponse(StatusCodes.Created, _, entity, _) =>
        entity.dataBytes.runFold(ByteString(""))(_ ++ _) map (content => {
          val p = Json.parse(content.decodeString(ByteString.UTF_8)).as[JsObject]
          (p \ "data" \ "id").validate[String].get
        })
      case HttpResponse(sc, _, _, _) =>
        Future.failed(new Exception(s"received status $sc"))
    }
  }

  def fetch
    (conn: Flow[HttpRequest, HttpResponse, Future[OutgoingConnection]], token: String, deviceId: UUID, sid: SessionId)
    (implicit s: ActorSystem, materializer: ActorMaterializer, ec: ExecutionContext)
  : Future[(StatusCode, Result)] =
    Source.single(
      HttpRequest(
        GET,
        uri = s"/v2/sync/$sid/queues/main",
        protocol = `HTTP/1.1`,
        headers = List(
          headers.Authorization(headers.OAuth2BearerToken(token)),
          RawHeader("Accept", "application/json"),
          RawHeader("X-Basecrm-Device-UUID", deviceId.toString)
        )
      )
    ).via(conn).runWith(Sink.head) flatMap {
      case HttpResponse(StatusCodes.NoContent, _, _, _) =>
        Future.successful((StatusCodes.NoContent, List.empty[Row]))
      case HttpResponse(StatusCodes.OK, _, entity, _) =>
        entity.dataBytes.runFold(ByteString(""))(_ ++ _) map (content => {
          val p = Json.parse(content.decodeString(ByteString.UTF_8)).as[JsObject]
          val rows: Result = (p \ "items").validate[List[JsObject]].get.map(x => Json.stringify(x))
          (StatusCodes.OK, rows)
        })
      case HttpResponse(sc, _, _, _) =>
        Future.failed(new Exception(s"received status $sc"))
    }

  def ack(host: String, token: String)(id: UUID, sid: SessionId): Future[StatusCode] = ???

}
