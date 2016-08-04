package io.fullstackanalytics.basecrm


import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.Http.OutgoingConnection
import akka.http.scaladsl.model._
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, Materializer}

import scala.concurrent._

// todo: add oauth reinitialization on 400s
object Publisher extends Client {

  sealed trait State
  case object Closed extends State
  case class  Queued(id: SessionId, prev: Option[StatusCode]) extends State

  def apply
    (token: String, deviceId: UUID, conn: Flow[HttpRequest, HttpResponse, Future[OutgoingConnection]])
    (implicit s: ActorSystem, m: ActorMaterializer, ec: ExecutionContext) = //(handler: () => Future[Unit]) =

    Source.unfoldAsync[State, Result](Closed) {
      case Closed =>
        println("starting")
        start(conn, token, deviceId) map (sid => Some(Queued(sid, None), List.empty[Row]))

      case Queued(sid, Some( StatusCodes.NoContent) ) =>
        println("queuing ended")
        Future.successful(Some((Closed, List.empty[Row])))

      case Queued(sid, Some( StatusCodes.OK) | None ) =>
        println("queuing started")
        val response = fetch(conn, token, deviceId, sid)
        response map { case (code, result) =>
          Some((Queued(sid, Some(code)), result))
        }

      case _ => throw new Exception("invalid publisher state ...")
    }
    .mapConcat(x => x)

}