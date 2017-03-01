package edu

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, RunnableGraph, Sink, Source}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

//introduction
case class Lesson1(implicit val system: ActorSystem, materializer: ActorMaterializer) {

  //first stream
  def example1() = {
    //a source build from 3-elements list
    //(type of outgoing single element is Int, materialization type is NotUsed)
    val source: Source[Int, NotUsed] = Source(List(1, 2, 3))
    //a sink that prints out incoming elements
    //(type of incoming element may be Any, materialization type is Future[Done])
    val sink: Sink[Any, Future[Done]] = Sink.foreach(println)
    //stream blueprint, nothing running yet
    //(materialization type of whole graph is NotUsed)
    val stream: RunnableGraph[NotUsed] = source.to(sink)
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
    //materialization type of whole graph is taken from Sink.foreach stage
    val stream: RunnableGraph[Future[Done]] = Source(List(1, 2)).toMat(Sink.foreach(println))(Keep.right)
    Await.result(
      for {
        _ <- stream.run
        _ <- stream.run
      } yield (),
      Duration.Inf
    )
  }

  //simple sources
  def example4() = {
    val stream1 = Source.single(1).to(Sink.foreach(println))
    val stream2 = Source.repeat(1).take(5).to(Sink.foreach(println))
    val iterator = Iterator.from(1)
    val stream3 = Source.fromIterator(() => iterator).take(5).to(Sink.foreach(println))
    val stream4 = Source.tick(0.seconds, 100.millis, "a").to(Sink.foreach(println))
    stream3.run
    //TODO run other streams, one at a time
  }

  def call(example: Int) = example match {
    case 1 => example1()
    case 2 => example2()
    case 3 => example3()
    case 4 => example4()
    case _ => println("wrong example")
  }
}