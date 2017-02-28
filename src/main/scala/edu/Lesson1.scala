package edu

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

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

  //using given stream blueprint twice, but using Sink.foreach materialization to sequence the executions
  def example3() = {
    import system.dispatcher //for futures' execution context
    val stream = Source(List(1, 2)).toMat(Sink.foreach(println))(Keep.right)
    Await.result(
      for {
        _ <- stream.run
        _ <- stream.run
      } yield (),
      Duration.Inf
    )
  }

  def call(example: Int) = example match {
    case 1 => example1()
    case 2 => example2()
    case 3 => example3()
    case _ => println("wrong example")
  }
}