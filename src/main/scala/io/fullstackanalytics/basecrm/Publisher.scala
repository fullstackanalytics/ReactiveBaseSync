package io.fullstackanalytics.basecrm


import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{BasicHttpCredentials, ModeledCustomHeader, ModeledCustomHeaderCompanion}
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.ByteString

import scala.concurrent._

object Publisher extends Client {

  sealed trait State
  case object Closed extends State
  case class  Queued(id: SessionId, prev: Option[StatusCode]) extends State

  def apply (id: Uuid)(implicit s: ActorSystem, m: ActorMaterializer, ec: ExecutionContext) = //(handler: () => Future[Unit]) =

    Source.unfoldAsync[State, Result](Closed) {
      case Closed =>
        println("starting")
        start(HOST, TOKEN)(id) map (sid => Some(Queued(sid, None), List.empty[Row]))

      case Queued(sid, Some( StatusCodes.NoContent) ) =>
        println("queuing ended")
        Future.successful(Some((Closed, List.empty[Row])))

      case Queued(sid, Some( StatusCodes.OK) | None ) =>
        println("queuing started")
        val response = fetch(HOST, TOKEN)(id, sid)
        response map { case (code, result) =>
          Some((Queued(sid, Some(code)), result))
        }

      case _ => throw new Exception("invalid publisher state ...")
    }
    .mapConcat(x => x)

}