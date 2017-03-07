package edu

import java.util.concurrent.TimeUnit

import akka.Done
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ThrottleMode}
import akka.stream.scaladsl.{Keep, RunnableGraph, Sink, Source}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

//more operations on Source/Flow
case class Lesson2(implicit val system: ActorSystem, materializer: ActorMaterializer) {
  import system.dispatcher

  //filter
  def example1() = {
    //take natural numbers, add filter pssing even numbers though, take 5 of those and print out
    val naturalNumbersSource = Source.fromIterator(() => Iterator.from(1))
    val stream = naturalNumbersSource.filter(_ % 2 == 0).take(5).to(Sink.foreach(println))
    stream.run
  }

  //reduce
  def example2() = {
    val naturalNumbersSource = Source.fromIterator(() => Iterator.from(1))
    val stream = naturalNumbersSource.take(10).reduce(_ + _).to(Sink.foreach(println))
    stream.run
  }

  //scan: simple example - calculating factorial
  //scan is like fold, but emitting all intermediate states
  def example3() = {
    val naturalNumbersSource = Source.fromIterator(() => Iterator.from(1))
    val stream = naturalNumbersSource.take(10).scan(1)(_ * _).to(Sink.foreach(println))
    stream.run
  }

  //scan: modifying position according to directions
  def example4() = {
    val directions = Source("nsseewnn".toList)
    val modifyPosition: ((Int, Int), Char) => (Int, Int) = {
      case ((x, y), 'n') => (x - 1, y)
      case ((x, y), 's') => (x + 1, y)
      case ((x, y), 'e') => (x, y + 1)
      case ((x, y), 'w') => (x, y - 1)
    }
    val stream = directions
      .scan((0, 0))(modifyPosition)
      .to(Sink.foreach(println))
    stream.run
  }

  val animalsMap = Map(1 -> "1. tiger", 2 -> "2. lion", 3 -> "3. zebra")

  //dummy "remote" service call, which delays the answer by elem * 200 milliseconds
  def callRemoteService(elem: Int): Future[String] = Future {
    println(s"start call for $elem")
    Thread.sleep(elem * 200)
    println(s"finished call for $elem")
    animalsMap(elem)
  }

  //mapAsync
  def example5() = {
    //processing serveral elements through remote service call
    //mapAsync(1) means parallelism = 1
    //you can observe that there is one call at a time
    //toMat(Sink.foreach(println))(Keep.right) is needed to obtain Future[Done] during materialization
    //so we can await stream completion
    val stream: RunnableGraph[Future[Done]] = Source(List(3, 2, 1, 3, 2, 1)).mapAsync(1)(callRemoteService).toMat(Sink.foreach(println))(Keep.right)
    Await.result(stream.run, Duration.Inf)
  }

  //mapAsync with higher level of parallelism
  def example6() = {
    //now, with mapAsync(2) you can observe that remote service calls are processed in parallel
    //the results are still printed in order by which they came out of the source
    val stream: RunnableGraph[Future[Done]] = Source(List(3, 2, 1, 3, 2, 1)).mapAsync(2)(callRemoteService).toMat(Sink.foreach(println))(Keep.right)
    Await.result(stream.run, Duration.Inf)
  }

  //mapAsyncUnordered(2)
  def example7() = {
    //mapAsyncUnordered doesn't have to keep order on output
    val stream: RunnableGraph[Future[Done]] = Source(List(3, 2, 1, 3, 2, 1)).mapAsyncUnordered(2)(callRemoteService).toMat(Sink.foreach(println))(Keep.right)
    Await.result(stream.run, Duration.Inf)
  }

  //mapAsyncUnordered(3)
  def example8() = {
    //even higher level of parallelism
    val stream: RunnableGraph[Future[Done]] = Source(List(3, 2, 1, 3, 2, 1)).mapAsyncUnordered(3)(callRemoteService).toMat(Sink.foreach(println))(Keep.right)
    Await.result(stream.run, Duration.Inf)
  }

  //throttle - passing 1 element per specificic time interval
  def example9() = {
    //below example behaves basically like Source.tick with interval of 1 second:
    val stream = Source(List(1, 2, 3)).throttle(1, 1.seconds, 1, ThrottleMode.shaping).toMat(Sink.foreach(println))(Keep.right)
    Await.result(stream.run, Duration.Inf)
  }

  //exercise 1: use throttle to make stream ordered even with mapAsyncUnordered (in code from example8)
  //run with sbt "run 2.11"
  def exercise10() = {
    //modify below code, add throttle:
    val stream: RunnableGraph[Future[Done]] = Source(List(3, 2, 1, 3, 2, 1)).mapAsyncUnordered(3)(callRemoteService).toMat(Sink.foreach(println))(Keep.right)
    Await.result(stream.run, Duration.Inf)
  }

  //TODO more exercises

  def call(example: Int) = example match {
    case 1 => example1()
    case 2 => example2()
    case 3 => example3()
    case 4 => example4()
    case 5 => example5()
    case 6 => example6()
    case 7 => example7()
    case 8 => example8()
    case 9 => example9()
    case 10 => exercise10()
    case _ => println("wrong example")
  }
}

