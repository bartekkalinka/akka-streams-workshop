package edu

import akka.actor.{ActorSystem, PoisonPill}
import akka.stream.{ActorMaterializer, OverflowStrategy, ThrottleMode}
import akka.stream.scaladsl.{Keep, Sink, Source}

import scala.concurrent.Await
import scala.concurrent.duration._

//flow dsl various
case class Lesson4(implicit val system: ActorSystem, materializer: ActorMaterializer) {

  import system.dispatcher

  //example of using materialized value which is a control value, rather than Future of result
  //Source.tick has materialized value of type Cancellable
  //we schedule cancel() call after 5 seconds
  def example1() = {
    val cancellableTick = Source.tick(0.seconds, 500.millis, ()).toMat(Sink.foreach(println))(Keep.both)
    val (cancellable, streamFuture) = cancellableTick.run
    system.scheduler.scheduleOnce(5.seconds)(cancellable.cancel())
    Await.result(streamFuture, Duration.Inf)
  }

  //counting words in a text file
  def example2() = {
    //I'm not using FileIO.fromPath, but scala.io.Source instead because of getLines method
    val fileSource = scala.io.Source.fromFile("input.txt")
    //flatMapConcat: for each element, transform it into a Source, then concatenate these Sources into one Source
    val wordsSource = Source.fromIterator(fileSource.getLines)
      .flatMapConcat(line => Source[String](line.split(Array(' ', '\n', '\t', '.')).toList))
      .filter(_.trim.length > 1)
    def countWord(map: Map[String, Int], word: String): Map[String, Int] =
      map.updated(word, map.getOrElse(word, 0) + 1)
    //folding wordcounts
    val stream =
      wordsSource.toMat(
        Sink.fold(Map[String, Int]())(countWord)
      )(Keep.right)
    val wordcounts = Await.result(stream.run, Duration.Inf)
    println(wordcounts.toSeq.sortBy(-_._2).take(100))
  }

  //inserting elements into stream's source by sending messages to an actor
  def example3() = {
    //Source.actorRef has materialization value of type ActorRef
    //it does not support backpressure, so we can only choose between other overflow strategies
    val source = Source.actorRef[Any](10, OverflowStrategy.dropHead)
    val stream = source.collect{ case i: Int => i + 1 }.toMat(Sink.foreach(println))(Keep.both)
    //actorRef is mat. value of Source.actorRef, streamFuture is mat. value of Sink.foreach
    val (actorRef, streamFuture) = stream.run
    actorRef ! 1
    actorRef ! 2
    actorRef ! 3
    //we have to send poison pill to actor, otherwise streamFuture will hang indefinitely
    //we have to send it with delay, otherwise the stream may fail to process its elements on time
    //(actor's death means completion of the stream)
    system.scheduler.scheduleOnce(1.second)(actorRef ! PoisonPill)
    Await.result(streamFuture, Duration.Inf)
  }

  //demonstration of lack of backpressure in Source.actorRef
  def example4() = {
    val source = Source.actorRef[Any](1, OverflowStrategy.dropHead)
    val mainStream = source
      .collect{ case i: Int => i + 1 }
      .throttle(1, 500.millis, 1, ThrottleMode.shaping)
      .toMat(Sink.foreach(println))(Keep.both)
    val (actorRef, streamFuture) = mainStream.run
    //faster stream writes to slower stream through actorRef
    val sendingFuture = Source(1 to 100)
      .concat(Source.single(PoisonPill))
      .throttle(1, 50.millis, 1, ThrottleMode.shaping)
      .runWith(Sink.foreach(actorRef ! _))
    Await.result(streamFuture.zip(sendingFuture), Duration.Inf)
  }

  def call(example: Int) = example match {
    case 1 => example1()
    case 2 => example2()
    case 3 => example3()
    case 4 => example4()
    case _ => println("wrong example")
  }
}

