package edu

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{RunnableGraph, Sink, Source}

//introduction
case class Lesson1(implicit val system: ActorSystem, materializer: ActorMaterializer) {

  //first stream
	def example1() = {
    val stream: RunnableGraph[NotUsed] = Source(List(1, 2, 3)).to(Sink.foreach(println))
    val notUsed: NotUsed = stream.run
  }

  def call(example: Int) = example match {
    case 1 => example1()
    case _ => println("wrong example")
  }
}

object Main extends App {
  implicit val system = ActorSystem("Lesson1")
  implicit val materializer = ActorMaterializer()

  val call = "1.1"

  val Array(lesson, example) = call.split('.').map(_.toInt)

  lesson match {
    case 1 => Lesson1().call(example)
    case _ => "wrong lesson"
  }

  system.terminate()
}