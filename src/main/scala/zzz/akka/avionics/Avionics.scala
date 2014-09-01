package zzz.akka.avionics

import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import scala.concurrent.Await

object Avionics {
  // needed for '?' below
  implicit val timeout = Timeout(5.seconds)
  val system = ActorSystem("PlaneSimulation")
  val plane = system.actorOf(Props[Plane], "Plane")

  def main(args: Array[String]) {
    // Grab the controls
    val control = Await.result((plane ? Plane.GiveMeControl).mapTo[ActorRef], 5.seconds)
    // Takeoff!
    system.scheduler.scheduleOnce(200.millis) {
      control ! ControlSurfaces.StickBack(1f)
    }
    // Level out
    system.scheduler.scheduleOnce(1.second) {
      control ! ControlSurfaces.StickBack(0f)
    }
    // Climb
    system.scheduler.scheduleOnce(3.seconds) {
      control ! ControlSurfaces.StickBack(0.5f)
    }
    // Level Out
    system.scheduler.scheduleOnce(4.seconds) {
      control ! ControlSurfaces.StickBack(0f)
    }
    // Shut down
    system.scheduler.scheduleOnce(5.seconds) {
      system.shutdown()
    }
  }
}
