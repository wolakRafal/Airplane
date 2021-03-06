/** ***************************************************************************
  * Copyright (c) 2013
  * ADVA Optical Networking
  *
  * All rights reserved. Any unauthorized disclosure or publication of the
  * confidential and proprietary information to any other party will constitute
  * an infringement of copyright laws.
  *
  * $Id$
  * Author  : Rafal Wolak, RWolak@advaoptical.com
  * Created : 30 August 2014
  * Purpose :
  *
  * $Rev$
  * $URL$
  *
  * Notes:
  *
  * ****************************************************************************
  */

package zzz.akka.avionics

import akka.actor.{Props, Actor, ActorLogging}
import akka.pattern.ask
import akka.routing.FromConfig
import zzz.akka.avionics.EventSource.RegisterListener
import zzz.akka.avionics.IsolatedLifeCycleSupervisor.WaitForStart
import zzz.akka.avionics.passengers.PassengerSupervisor
import scala.concurrent.Await
import scala.concurrent.duration._

object Plane {

  // Returns the control surface to the Actor that asks for them
  case object GiveMeControl

}

// We want the Plane to own the Altimeter and we're going to do that
// by passing in a specific factory we can use to build the Altimeter
class Plane extends Actor with ActorLogging {
  this: AltimeterProvider with PilotProvider with LeadFlightAttendantProvider with HeadingIndicatorProvider =>

  import Altimeter._
  import Plane._

  val config = context.system.settings.config

  val pilotName = config.getString("zzz.akka.avionics.flightcrew.pilotName")
  val copilotName = config.getString("zzz.akka.avionics.flightcrew.copilotName")
  val autopilotName = config.getString("AutoPilot")
  val attendantName = config.getString("zzz.akka.avionics.flightcrew.leadAttendantName")
  val altimeterName = "Altimeter"
  val headingIndicatorName = "HeadingIndicator"
  val controlSurfacesName = "ControlSurfaces"
  val flightAttendant = context.actorOf(Props(LeadFlightAttendant()),
    config.getString("zzz.akka.avionics.flightcrew.leadAttendantName"))

  implicit val timeout = akka.util.Timeout(3.seconds)

  // new methodschapter 8
  def startControls() = {
    val controls = context.actorOf(Props(new IsolatedResumeSupervisor with OneForOneStrategyFactory {
      def childStarter(): Unit = {
        val alt = context.actorOf(Props(newAltimeter), altimeterName)
        val heading = context.actorOf(Props(newHeadingIndicator), headingIndicatorName)
        context.actorOf(Props(newAutopilot), autopilotName)
        context.actorOf(Props(new ControlSurfaces(self, alt, heading)), controlSurfacesName)
      }
    }), "Controls")
    Await.result(controls ? WaitForStart, 1.second)
  }

  // Helps us look up Actors within the "Controls" Supervisor
  def actorForControls(name: String) = context.actorFor("Controls/" + name)

  def startPeople() {
    // Use the Router as defined in the configuration file
    // under the name "LeadFlightAttendant"
    val leadAttendant = context.actorOf(Props(newFlightAttendant).withRouter(FromConfig()), "LeadFlightAttendant")

    val plane = self
    // Note how we depend on the Actor structure beneath
    // us here by using actorFor(). This should be
    // resilient to change, since we'll probably be the
    // ones making the changes
    val controls = actorForControls(controlSurfacesName)
    val autopilot = actorForControls(autopilotName)
    val altimeter = actorForControls(altimeterName)
    val people = context.actorOf(Props(new IsolatedStopSupervisor
                                        with OneForOneStrategyFactory {
      def childStarter() {
        context.actorOf(Props(PassengerSupervisor(leadAttendant)), "Passenegers")
        context.actorOf(Props(newCoPilot(plane, autopilot, altimeter)), copilotName)
        context.actorOf(Props(newPilot(plane, autopilot, controls, altimeter)), pilotName)
      }
    }), "Pilots")
    // Use the default strategy here, which
    // restarts indefinitely
    context.actorOf(Props(newFlightAttendant), attendantName)
    Await.result(people ? WaitForStart, 1.second)
  }

  def receive = {
    case GiveMeControl =>
      log.info("Plane giving control.")
      sender ! actorForControls("ControlSurfaces")
    case AltitudeUpdate(altitude) =>
      log.info(s"Altitude is now: $altitude")
  }

  // Helps us look up Actors within the "Pilots" Supervisor
  def actorForPilots(name: String) = context.actorFor("Pilots/" + name)

  override def preStart() {
    // Get our children going. Order is important here.
    startControls()
    startPeople()
    // Bootstrap the system
    actorForControls(altimeterName) ! EventSource.RegisterListener(self)
    actorForPilots(pilotName) ! Pilots.ReadyToGo
    actorForPilots(copilotName) ! Pilots.ReadyToGo
  }
}

