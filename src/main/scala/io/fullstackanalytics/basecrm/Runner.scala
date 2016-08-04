package io.fullstackanalytics.basecrm

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._

import scala.concurrent.duration._


object Runner extends App {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  val p = Publisher("mattTest")
    .throttle(10, 20 seconds, 10, akka.stream.ThrottleMode.Shaping)
    .runWith(Sink.foreach(x => println(x)))



}