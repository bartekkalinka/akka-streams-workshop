package edu

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}

//more operations on Source/Flow
case class Lesson2(implicit val system: ActorSystem, materializer: ActorMaterializer) {

  //filter
  def example1() = {
    //take natural numbers, add filter pssing even numbers though, take 5 of those and print out
    val stream = Source.fromIterator(() => Iterator.from(1)).filter(_ % 2 == 0).take(5).to(Sink.foreach(println))
    stream.run
  }

  def call(example: Int) = example match {
    case 1 => example1()
//    case 2 => example2()
//    case 3 => example3()
//    case 4 => example4()
//    case 5 => example5()
//    case 6 => example6()
//    case 7 => example7()
    case _ => println("wrong example")
  }
}

