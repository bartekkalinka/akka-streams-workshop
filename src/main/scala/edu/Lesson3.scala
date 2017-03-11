package edu

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, OverflowStrategy, SourceShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, Merge, RunnableGraph, Sink, Source}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

case class Lesson3(implicit val system: ActorSystem, materializer: ActorMaterializer) {
  import system.dispatcher

  //This method is going to be used in Flow.map to delay and monitor the execution
  def syncCall(delay: Int)(elem: Int): Int = {
    println(s"start call for $elem with delay $delay")
    Thread.sleep(delay * 200)
    println(s"finished call for $elem with delay $delay")
    elem
  }

  //This is above method packed in Future to use in Flow.mapAsync
  def asyncCall(delay: Int)(elem: Int): Future[Int] = Future {
    syncCall(delay)(elem)
  }

  //Graph dsl - here you can take elements/stages and connect them with arrows
  //to achieve more complex graphs than linear flow from source to sink.
  //The source is broadcast into 2 flows, which are then merged into one sink
  def example1() = {
    //creating closed graph using graph dsl
    val graph = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._
      val in = Source(1 to 3)
      val out = Sink.foreach(println)

      //Broadcast and Merge elements are "stages" - something too low-level for flow dsl
      //that's why we need to add them to builder, to use their inlets and outlets
      val bcast = builder.add(Broadcast[Int](2))
      val merge = builder.add(Merge[Int](2))

      //We can still construct flows using straightforward flow dsl
      val flow1 = Flow[Int].map(syncCall(1))
      val flow2 = Flow[Int].map(syncCall(4))

      in ~> bcast ~> flow1 ~> merge ~> out
            bcast ~> flow2 ~> merge
      ClosedShape //this is "shape" of the graph we return - it's a closed RunnableGraph, so it has ClosedShape
    })
    //graph is RunnableGraph, so let's run it
    graph.run
  }

  //Experiment: how parallel can be both flows in above graph?
  //Let's use mapAsync on both of them
  def example2() = {
    //Creating source with graph dsl
    //Reason: when we use asynchronous methods,
    //it's neccessary to obtain Future[Done] materialization value.
    //It's easy if we build Source separately and then use source.toMat(sink)
    val source = Source.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._
      val in = Source(1 to 3)

      val bcast = builder.add(Broadcast[Int](2))
      val merge = builder.add(Merge[Int](2))

      //Flows use mapAsync
      val flow1 = Flow[Int].mapAsync(1)(asyncCall(1))
      val flow2 = Flow[Int].mapAsync(1)(asyncCall(4))

      in ~> bcast ~> flow1 ~> merge
      bcast ~> flow2 ~> merge
      SourceShape(merge.out) //returning source shape this time, connected to Merge outlet
    })
    val graph: RunnableGraph[Future[Done]] = source.toMat(Sink.foreach(println))(Keep.right)
    Await.result(graph.run, Duration.Inf)
  }

  //exercise: use Flow.buffer(3, OverflowStrategy.backpressure) to make both flows execute independently
  //(so the elements go to sink in order in which delays make them to)
  def exercise3() = {
    val source = Source.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._
      val in = Source(1 to 3)

      val bcast = builder.add(Broadcast[Int](2))
      val merge = builder.add(Merge[Int](2))

      //Flows use mapAsync
      val flow1 = Flow[Int].mapAsync(1)(asyncCall(1))
      val flow2 = Flow[Int].buffer(3, OverflowStrategy.backpressure).mapAsync(1)(asyncCall(4))

      in ~> bcast ~> flow1 ~> merge
      bcast ~> flow2 ~> merge
      SourceShape(merge.out) //returning source shape this time, connected to Merge outlet
    })
    val graph: RunnableGraph[Future[Done]] = source.toMat(Sink.foreach(println))(Keep.right)
    Await.result(graph.run, Duration.Inf)
  }

  //TODO show simpler ways of merging sources and broadcasting to sinks with flow dsl

  //TODO alsoTo, merge, zip, Source.combine

  def call(example: Int) = example match {
    case 1 => example1()
    case 2 => example2()
    case 3 => exercise3()
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

