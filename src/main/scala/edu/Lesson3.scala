package edu

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, SourceShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, Merge, RunnableGraph, Sink, Source}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

case class Lesson3(implicit val system: ActorSystem, materializer: ActorMaterializer) {
  import system.dispatcher

  def syncCall(delay: Int)(elem: Int): Int = {
    println(s"start call for $elem with delay $delay")
    Thread.sleep(delay * 200)
    println(s"finished call for $elem with delay $delay")
    elem
  }

  def asyncCall(delay: Int)(elem: Int): Future[Int] = Future {
    syncCall(delay)(elem)
  }

  //Graph dsl - here you can take elements/stages and connect them with arrows
  //to achieve more complex graphs than linear flow from source to sink.
  //the source broadcast into 2 flows, then merged into one sink
  def example1() = {
    val graph = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._
      val in = Source(1 to 3)
      val out = Sink.foreach(println)

      val bcast = builder.add(Broadcast[Int](2))
      val merge = builder.add(Merge[Int](2))

      val flow1 = Flow[Int].map(syncCall(1))
      val flow2 = Flow[Int].map(syncCall(4))

      in ~> bcast ~> flow1 ~> merge ~> out
            bcast ~> flow2 ~> merge
      ClosedShape
    })
    graph.run
  }

  def example2() = {
    val source = Source.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._
      val in = Source(1 to 3)

      val bcast = builder.add(Broadcast[Int](2))
      val merge = builder.add(Merge[Int](2))

      val flow1 = Flow[Int].mapAsync(1)(asyncCall(1))
      val flow2 = Flow[Int].mapAsync(1)(asyncCall(4))

      in ~> bcast ~> flow1 ~> merge
      bcast ~> flow2 ~> merge
      SourceShape(merge.out)
    })
    Await.result(source.toMat(Sink.foreach(println))(Keep.right).run, Duration.Inf)
  }

  //TODO show simpler ways of merging sources and broadcasting to sinks with flow dsl


  def call(example: Int) = example match {
    case 1 => example1()
    case 2 => example2()
//    case 3 => example3()
//    case 4 => example4()
//    case 5 => example5()
//    case 6 => example6()
//    case 7 => example7()
//    case 8 => example8()
//    case 9 => example9()
//    case 10 => exercise10()
    case _ => println("wrong example")
  }
}

