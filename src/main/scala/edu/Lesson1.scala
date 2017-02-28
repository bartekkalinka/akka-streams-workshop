package edu

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}

//introduction
case class Lesson1(implicit val system: ActorSystem, materializer: ActorMaterializer) {

  //first stream
  def example1() = {
    //stream blueprint, nothing running yet
    //a source build from 3-elements list
    //connected to
    //a sink that for each incoming element prints it out
    val stream = Source(List(1, 2, 3)).to(Sink.foreach(println))
    //running the stream
    stream.run
  }

  //using given stream blueprint twice
  def example2() = {
    val stream = Source(List(1, 2)).to(Sink.foreach(println))
    stream.run
    stream.run
  }

  def call(example: Int) = example match {
    case 1 => example1()
    case 2 => example2()
    case _ => println("wrong example")
  }
}