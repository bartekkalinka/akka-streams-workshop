package edu

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives
import akka.stream.{ActorMaterializer, ThrottleMode}
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.concurrent.Await
import scala.concurrent.duration._

case class Lesson5(implicit val system: ActorSystem, materializer: ActorMaterializer) extends Directives {
  private def route(flow: Flow[String, String, NotUsed]) = {
    def websocketFlow(sender: String): Flow[Message, Message, NotUsed] =
      Flow[Message]
        .collect { case TextMessage.Strict(msg) => msg }
        .via(flow)
        .map{ msg: String => TextMessage.Strict(msg) }
    pathSingleSlash {
      getFromResource("client/index.html")
    } ~
      path("websockets") {
        parameter('clientId) { clientId =>
          handleWebSocketMessages(websocketFlow(sender = clientId))
        }
      } ~
      getFromResourceDirectory("client")
  }

  private def bind(flow: Flow[String, String, NotUsed]) = {
    Http().bindAndHandle(route(flow), "0.0.0.0", 9000)
    println("press enter to stop...")
    io.Source.stdin.getLines.hasNext
  }

  def example1() = {
    val flow = Flow.fromSinkAndSource[String, String](
      Sink.ignore,
      Source.unfold(0)(i => Some(i + 1, i)).throttle(1, 500.millis, 1, ThrottleMode.shaping).map(_.toString)
    )
    bind(flow)
  }

  def call(example: Int) = example match {
    case 1 => example1()
//    case 2 => example2()
//    case 3 => example3()
//    case 4 => example4()
    case _ => println("wrong example")
  }
}

