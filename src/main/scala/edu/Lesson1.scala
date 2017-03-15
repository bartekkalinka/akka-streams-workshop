package edu

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, RunnableGraph, Sink, Source}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

//introduction
case class Lesson1(implicit val system: ActorSystem, materializer: ActorMaterializer) {

  //first stream
  def example1() = {
    //source: something with output
    //this source build from 3-elements list
    //(type of outgoing single element is Int, materialization type is NotUsed)
    val source: Source[Int, NotUsed] = Source(List(1, 2, 3))
    //sink: something with input
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

  //flow
  def example5() = {
    //flow: something with input and output
    //type of incoming element is Int, type of outgoing element is Int, materialization type is NotUsed
    //Flow[Int](.apply) is a helper constructor to create a Flow[Int, Int, ...] which outputs all inputs without modification
    val flow: Flow[Int, Int, NotUsed] = Flow[Int].map(_ + 1)
    val source = Source(List(1, 2, 3))
    val sink = Sink.foreach(println)
    val stream = source.via(flow).to(sink)
    stream.run
  }

  //source and sink from flow
  def example6() = {
    val flowToStr: Flow[Int, String, NotUsed] = Flow[Int].map(_.toString + "0")
    //notice the types: Source[Int, ...] via Flow[Int, String, ...] becomes Source[String, ...]
    val source: Source[String, NotUsed] = Source(List(1, 2, 3)).via(flowToStr)
    //another way to construct flow: fromFunction
    val flowFromString: Flow[String, Int, NotUsed] = Flow.fromFunction(_.toInt)
    //notice the types: Flow[String, Int, ...] to Sink[Any, ...] becomes Sink[String, ...]
    val sink: Sink[String, NotUsed] = flowFromString.to(Sink.foreach(println))
    val stream = source.to(sink)
    stream.run
  }

  //flow from sink and source
  def example7() = {
    //input of the flow is made from a sink, and output is made from source
    //the sink and source are not connected to each other
    val flow: Flow[Any, Int, NotUsed] = Flow.fromSinkAndSource(Sink.ignore, Source(List(5, 6, 7)))
    val stream = Source.repeat("aaa").via(flow).to(Sink.foreach(println))
    stream.run
  }

  def call(example: Int) = example match {
    case 1 => example1()
    case 2 => example2()
    case 3 => example3()
    case 4 => example4()
    case 5 => example5()
    case 6 => example6()
    case 7 => example7()
    case _ => println("wrong example")
  }
}