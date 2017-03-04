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

  //TODO stdin source without need to press enter
  def example4() = {
    val charactersPressedByUser = Source.fromIterator(() => io.Source.stdin.iter)
    def modifyPosition(char: Char, position: (Int, Int)): (Int, Int) = (char, position) match {
      case ('a', (x, y)) => (x - 1, y)
      case ('d', (x, y)) => (x + 1, y)
      case ('s', (x, y)) => (x, y + 1)
      case ('w', (x, y)) => (x, y - 1)
    }
    val stream = charactersPressedByUser
      .scan((0, 0)) { case (position, char) => modifyPosition(char, position) }
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

