package edu

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

object Main extends App {
  implicit val system = ActorSystem("Lesson1")
  implicit val materializer = ActorMaterializer()

  val call = "1.2"

  val Array(lesson, example) = call.split('.').map(_.toInt)

  lesson match {
    case 1 => Lesson1().call(example)
    case _ => "wrong lesson"
  }

  system.terminate()
}