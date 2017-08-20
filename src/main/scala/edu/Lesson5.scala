package edu

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives
import akka.stream.{ActorMaterializer, ThrottleMode}
import akka.stream.scaladsl.{BroadcastHub, Flow, Keep, MergeHub, Sink, Source}

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

  //sbt "run 5.1"
  //and then open http://localhost:9000/index.html in browser
  //then, try to open same url in second tab
  //compare counts
  def example1() = {
    val flow = Flow.fromSinkAndSource[String, String](
      Sink.ignore,
      Source.unfold(0)(i => Some(i + 1, i)).throttle(1, 500.millis, 1, ThrottleMode.shaping).map(_.toString)
    )
    bind(flow)
  }

  //open 2 clients after running this example -> this does not allow to open new source
  def example2() = {
    val previousSource = Source.unfold(0)(i => Some(i + 1, i)).throttle(1, 500.millis, 1, ThrottleMode.shaping).map(_.toString)
    val hubSource = previousSource.runWith(BroadcastHub.sink(bufferSize = 256))
    val flow = Flow.fromSinkAndSource(Sink.ignore, hubSource)
    bind(flow)
  }

  //flow accepting input
  //non-empty input resets the counter
  def example3() = {
    val flow = Flow[String]
      .merge(Source.single("reset")) //artificial input to start the counter
      .expand(s => Iterator(s) ++ Iterator.continually("")) //emitting received values + empty values in between
      .throttle(1, 500.millis, 1, ThrottleMode.shaping) //tick
      .scan(0) { case (count, input) => if(input == "") count + 1 else 0 } //state of the counter
      .map(_.toString)
    bind(flow)
  }

  //above flow with global state for all clients
  //when you reset, you reset global counter
  def example4() = {
    val previousFlow = Flow[String]
      .merge(Source.single("reset")) //artificial input to start the counter
      .expand(s => Iterator(s) ++ Iterator.continually("")) //emitting received values + empty values in between
      .throttle(1, 500.millis, 1, ThrottleMode.shaping) //tick
      .scan(0) { case (count, input) => if(input == "") count + 1 else 0 } //state of the counter
      .map(_.toString)
    val (sink, source) =
      MergeHub.source[String](perProducerBufferSize = 16)
        .via(previousFlow)
        .toMat(BroadcastHub.sink(bufferSize = 256))(Keep.both)
        .run()
    val flow = Flow.fromSinkAndSource(sink, source)
    bind(flow)
  }

  def call(example: Int) = example match {
    case 1 => example1()
    case 2 => example2()
    case 3 => example3()
    case 4 => example4()
    case _ => println("wrong example")
  }
}

