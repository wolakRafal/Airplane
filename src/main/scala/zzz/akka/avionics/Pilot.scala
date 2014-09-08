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
  * Created : 08 September 2014
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

import akka.actor._
import zzz.akka.avionics.Pilots.ReadyToGo
import akka.actor.ActorIdentity
import scala.Some
import akka.actor.Identify

object Pilots {

  case object ReadyToGo

  case object RelinquishControl

}

class Pilot extends Actor {

  import Pilots._

  val copilotIdentifyId = "copilot"
  val autopilotIdentifyId = "autopilot"
  var controls: ActorRef = context.system.deadLetters
  var copilot: ActorRef = context.system.deadLetters
  var autopilot: ActorRef = context.system.deadLetters

  val copilotName = context.system.settings.config.getString("zzz.akka.avionics.flightcrew.copilotName")

  def receive = {
    case ReadyToGo =>
      context.parent ! Plane.GiveMeControl
      context.actorSelection("../" + copilotName) ! Identify(copilotIdentifyId)
      context.actorSelection("../AutoPilot") ! Identify(autopilotIdentifyId)
    //    case Controls(controlSurfaces) =>
    //      controls = controlSurfaces
    case ActorIdentity(`copilotIdentifyId`, Some(ref)) =>
      copilot = ref
    case ActorIdentity(`autopilotIdentifyId`, Some(ref)) =>
      autopilot = ref
    // Failure scenarios TODO
    //    case ActorIdentity(`copilotIdentifyId`, None) =>
    //    case ActorIdentity(`autopilotIdentifyId`, None) =>

  }
}

class CoPilot extends Actor {

  import Pilots._

  var controls: ActorRef = context.system.deadLetters
  var pilot: ActorRef = context.system.deadLetters
  var autopilot: ActorRef = context.system.deadLetters
  val pilotName = context.system.settings.config.getString(
    "zzz.akka.avionics.flightcrew.pilotName")

  def receive = {
    case ReadyToGo =>
      pilot = context.actorFor("../" + pilotName)
      autopilot = context.actorFor("../AutoPilot")
  }
}
class AutoPilot extends Actor with ActorLogging {
  def receive = {
    case ReadyToGo =>
      log.info("AutoPilot ready To Go!")
  }
}
trait PilotProvider {
  def pilot: Actor = new Pilot
  def copilot: Actor = new CoPilot
  def autopilot: Actor = new AutoPilot
}