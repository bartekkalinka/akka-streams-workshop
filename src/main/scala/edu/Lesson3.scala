package edu

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, Merge, RunnableGraph, Sink, Source}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

//graph dsl and its substitutes
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
  def exercise3() =  {
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
      val flow2 = Flow[Int].buffer(3, OverflowStrategy.backpressure).mapAsync(1)(asyncCall(4)) //Flow[Int].mapAsync(1)(asyncCall(1))

      in ~> bcast ~> flow1 ~> merge
      bcast ~> flow2 ~> merge
      SourceShape(merge.out) //returning source shape this time, connected to Merge outlet
    })
    val graph: RunnableGraph[Future[Done]] = source.toMat(Sink.foreach(println))(Keep.right)
    Await.result(graph.run, Duration.Inf)
  }

  //Using alsoTo to broadcast one source to 2 sinks
  //The second sink will be Sink.last, which returns last value processed before completion.
  //To have access to materialization values we need to use alsoToMat version.
  def example4() = {
    val graph: RunnableGraph[(Future[Int], Future[Done])] = Source(1 to 30)
      .alsoToMat(Sink.last)(Keep.right)
      .toMat(Sink.foreach(println))(Keep.both) //Keep.both to keep both Sink.last and Sink.foreach materialization values
    val (futureLast, futureDone) = graph.run
    val (last, _) = Await.result(futureLast.zip(futureDone), Duration.Inf)
    println(s"last value was $last")
  }

  //Merging another source with Flow.merge
  def example5() = {
    val fasterSource = Source(1 to 3).throttle(1, 100.millis, 1, ThrottleMode.shaping)
    val slowerSource = Source(4 to 6).throttle(1, 300.millis, 1, ThrottleMode.shaping)
    //merge combines 2 sources asynchronously, so faster source elements go through faster
    val graph = fasterSource.via(Flow[Int].merge(slowerSource)).toMat(Sink.foreach(println))(Keep.right)
    Await.result(graph.run, Duration.Inf)
  }

  //Combining sources with Flow.zip
  def example6() = {
    val fasterSource = Source(1 to 3).throttle(1, 100.millis, 1, ThrottleMode.shaping)
    val slowerSource = Source(4 to 6).throttle(1, 300.millis, 1, ThrottleMode.shaping)
    //zip combines 2 sources synchronously, so both sources elements go through at once (and are combined into pairs)
    val graph = fasterSource.via(Flow[Int].zip(slowerSource)).toMat(Sink.foreach(println))(Keep.right)
    Await.result(graph.run, Duration.Inf)
  }

  //Another way of doing same thing: Source.combine
  def example7() = {
    val fasterSource = Source(1 to 3).throttle(1, 100.millis, 1, ThrottleMode.shaping)
    val slowerSource = Source(4 to 6).throttle(1, 300.millis, 1, ThrottleMode.shaping)
    //Merge(_) parameter tells it to use merge instead of zip
    val mergedSource = Source.combine(fasterSource, slowerSource)(Merge(_))
    val graph = mergedSource.toMat(Sink.foreach(println))(Keep.right)
    Await.result(graph.run, Duration.Inf)
  }

  def call(example: Int) = example match {
    case 1 => example1()
    case 2 => example2()
    case 3 => exercise3()
    case 4 => example4()
    case 5 => example5()
    case 6 => example6()
    case 7 => example7()
    case _ => println("wrong example")
  }
}

