package edu

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}

//more operations on Source/Flow
case class Lesson2(implicit val system: ActorSystem, materializer: ActorMaterializer) {

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

  def call(example: Int) = example match {
    case 1 => example1()
    case 2 => example2()
    case 3 => example3()
    case 4 => example4()
//    case 5 => example5()
//    case 6 => example6()
//    case 7 => example7()
    case _ => println("wrong example")
  }
}

