package io.fullstackanalytics.basecrm

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import com.typesafe.sslconfig.akka.AkkaSSLConfig

import scala.concurrent.duration._
import scala.util._
import java.util.UUID

import scala.concurrent.Await


object Runner {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    import system.dispatcher

    val token = args(0)
    val deviceId = UUID.randomUUID()
    println(deviceId)
    val badSslConfig = AkkaSSLConfig().mapSettings(s => s.withLoose(s.loose.withDisableSNI(true)))
    val badCtx = Http().createClientHttpsContext(badSslConfig)
    val conn = Http().outgoingConnectionHttps("api.getbase.com", connectionContext = badCtx)

    Publisher(token, deviceId, conn)
      .throttle(10, 20 seconds, 10, akka.stream.ThrottleMode.Shaping)
      .runWith(Sink.foreach(x => println(x)))

  }
}
