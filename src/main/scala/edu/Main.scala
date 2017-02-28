package edu

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{RunnableGraph, Sink, Source}

//introduction
case class Lesson1(implicit val system: ActorSystem, materializer: ActorMaterializer) {

  //first stream
	def example1 = {
    val stream: RunnableGraph[NotUsed] = Source(List(1, 2, 3)).to(Sink.foreach(println))
    val notUsed: NotUsed = stream.run
  }
}

object Main extends App {
  implicit val system = ActorSystem("Lesson1")
  implicit val materializer = ActorMaterializer()

  Lesson1().example1

  system.terminate()
}