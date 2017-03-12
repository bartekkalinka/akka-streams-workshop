package edu

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

object Main extends App {
  implicit val system = ActorSystem("Lesson1")
  implicit val materializer = ActorMaterializer()

  val call = if(args.length > 0) args(0) else "1.3"

  val Array(lesson, example) = call.split('.').map(_.toInt)

  lesson match {
    case 1 => Lesson1().call(example)
    case 2 => Lesson2().call(example)
    case 3 => Lesson3().call(example)
    case 4 => Lesson4().call(example)
    case _ => "wrong lesson"
  }

  system.terminate()
}