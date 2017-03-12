package edu

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ThrottleMode}
import akka.stream.scaladsl.{Keep, Sink, Source}

import scala.concurrent.Await
import scala.concurrent.duration._

case class Lesson4(implicit val system: ActorSystem, materializer: ActorMaterializer) {

  import system.dispatcher

  //example of using materialized value which is a control value, rather than Future of result
  //Source.tick has materialized value of type Cancellable
  //we start another stream that returns Future[Done] to use it to signal the moment to cancel the first stream
  def example1() = {
    val cancellableTick = Source.tick(0.seconds, 500.millis, ()).toMat(Sink.foreach(println))(Keep.left)
    val cancelSignal = Source(1 to 5).throttle(1, 1.seconds, 1, ThrottleMode.shaping).toMat(Sink.ignore)(Keep.right)
    val cancellable = cancellableTick.run
    Await.result(cancelSignal.run.map(_ => cancellable.cancel()), Duration.Inf)
  }

  def call(example: Int) = example match {
    case 1 => example1()
//    case 2 => example2()
//    case 3 => exercise3()
//    case 4 => example4()
//    case 5 => example5()
//    case 6 => example6()
//    case 7 => example7()
    case _ => println("wrong example")
  }
}

