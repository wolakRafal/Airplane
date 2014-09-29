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

import akka.actor.FSM.{CurrentState, SubscribeTransitionCallBack, Transition}
import akka.actor._
import zzz.akka.avionics.ControlSurfaces._
import zzz.akka.avionics.DrinkingBehaviour.{FeelingLikeZaphod, FeelingSober, FeelingTipsy}
import zzz.akka.avionics.FlyingBehaviour._
import zzz.akka.avionics.Pilots._

object Pilots {
  case object ReadyToGo
  case object RelinquishControl

  // Calculates the elevator changes when we're a bit tipsy
  val tipsyCalcElevator: Calculator = { (target, status) =>
    val msg = calcElevator(target, status)
    msg match {
      case StickForward(amt) => StickForward(amt * 1.03f)
      case StickBack(amt) => StickBack(amt * 1.03f)
      case m => m
    }
  }
  // Calculates the aileron changes when we're a bit tipsy
  val tipsyCalcAilerons: Calculator = { (target, status) =>
    val msg = calcAilerons(target, status)
    msg match {
      case StickLeft(amt) => StickLeft(amt * 1.03f)
      case StickRight(amt) => StickRight(amt * 1.03f)
      case m => m
    }
  }
  // Calculates the elevator changes when we're totally out of it
  val zaphodCalcElevator: Calculator = { (target, status) =>
    val msg = calcElevator(target, status)
    msg match {
      case StickForward(amt) => StickBack(1f)
      case StickBack(amt) => StickForward(1f)
      case m => m
    }
  }
  // Calculates the aileron changes when we're totally out of it
  val zaphodCalcAilerons: Calculator = { (target, status) =>
    val msg = calcAilerons(target, status)
    msg match {
      case StickLeft(amt) => StickRight(1f)
      case StickRight(amt) => StickLeft(1f)
      case m => m
    }
  }

  case class NewElevatorCalculator(c: Calculator)
  case class NewBankCalculator(c: Calculator)
}

class Pilot(plane: ActorRef,
            autopilot: ActorRef,
//            var controls: ActorRef,
            heading: ActorRef,
            altimeter: ActorRef) extends Actor {
  this: DrinkingProvider with FlyingProvider =>

  var copilot: ActorRef = context.system.deadLetters
  val copilotName = context.system.settings.config.getString("zzz.akka.avionics.flightcrew.copilotName")


  override def aroundPreStart(): Unit = {
    context.actorOf(newDrinkingBehaviour(self), "DrinkingBehaviour")
    context.actorOf(newFlyingBehaviour(plane, heading , altimeter), "DrinkingBehaviour")
  }

  def bootstrap: Receive = {
    case ReadyToGo =>
    val copilot = context.actorFor("../" + copilotName)
    val flyer = context.actorFor("FlyingBehaviour")
    flyer ! SubscribeTransitionCallBack(self)
    flyer ! Fly(CourseTarget(20000, 250, System.currentTimeMillis + 30000))
    context.become(sober(copilot, flyer))
  }

  // The 'sober' behaviour
  def sober(copilot: ActorRef, flyer: ActorRef): Receive = {
    case FeelingSober =>
    // We're already sober
    case FeelingTipsy =>
      becomeTipsy(copilot, flyer)
    case FeelingLikeZaphod =>
      becomeZaphod(copilot, flyer)
  }
  // The 'tipsy' behaviour
  def tipsy(copilot: ActorRef, flyer: ActorRef): Receive = {
    case FeelingSober =>
      becomeSober(copilot, flyer)
    case FeelingTipsy =>
    // We're already tipsy
    case FeelingLikeZaphod =>
      becomeZaphod(copilot, flyer)
  }
  // The 'zaphod' behaviour
  def zaphod(copilot: ActorRef, flyer: ActorRef): Receive = {
    case FeelingSober =>
      becomeSober(copilot, flyer)
    case FeelingTipsy =>
      becomeTipsy(copilot, flyer)
    case FeelingLikeZaphod =>
    // We're already Zaphod
  }
  // The 'idle' state is merely the state where the Pilot does nothing at all
  def idle: Receive = {
    case _ =>
  }
  // Updates the FlyingBehaviour with sober calculations and then
  // becomes the sober behaviour
  def becomeSober(copilot: ActorRef, flyer: ActorRef) = {
    flyer ! NewElevatorCalculator(calcElevator)
    flyer ! NewBankCalculator(calcAilerons)
    context.become(sober(copilot, flyer))
  }
  // Updates the FlyingBehaviour with tipsy calculations and then
  // becomes the tipsy behaviour
  def becomeTipsy(copilot: ActorRef, flyer: ActorRef) = {
    flyer ! NewElevatorCalculator(tipsyCalcElevator)
    flyer ! NewBankCalculator(tipsyCalcAilerons)
    context.become(tipsy(copilot, flyer))
  }
  // Updates the FlyingBehaviour with zaphod calculations and then
  // becomes the zaphod behaviour
  def becomeZaphod(copilot: ActorRef, flyer: ActorRef) = {
    flyer ! NewElevatorCalculator(zaphodCalcElevator)
    flyer ! NewBankCalculator(zaphodCalcAilerons)
    context.become(zaphod(copilot, flyer))
  }

  // At any time, the FlyingBehaviour could go back to an Idle state,
  // which means that our behavioural changes don't matter any more
  override def unhandled(msg: Any): Unit = {
    msg match {
      case Transition(_, _, Idle) =>
        context.become(idle)
      // Ignore these two messages from the FSM rather than have them
      // go to the log
      case Transition(_, _, _) =>
      case CurrentState(_, _) =>
      case m => super.unhandled(m)
    }
  }

  // Initially we start in the bootstrap state
  def receive = bootstrap
}

class CoPilot(plane: ActorRef, autopilot: ActorRef, altimeter: ActorRef) extends Actor {
  import zzz.akka.avionics.Pilots._

  var controls: ActorRef = context.system.deadLetters
  var pilot: ActorRef = context.system.deadLetters
  val pilotName = context.system.settings.config.getString(
    "zzz.akka.avionics.flightcrew.pilotName")

  def receive = {
    case ReadyToGo =>
      pilot = context.actorFor("../" + pilotName)
      context.watch(pilot)
    case Terminated(_) =>
      plane ! Plane.GiveMeControl
    case controlsSurface: ActorRef =>
      controls = controlsSurface
  }
}

class AutoPilot extends Actor with ActorLogging {
  def receive = {
    case ReadyToGo =>
      log.info("AutoPilot ready To Go!")
  }
}

trait PilotProvider {
  def newPilot(plane: ActorRef, autopilot: ActorRef, heading: ActorRef, altimeter: ActorRef): Actor =
    new Pilot(plane, autopilot, heading, altimeter) with DrinkingProvider with FlyingProvider

  def newCoPilot(plane: ActorRef, autopilot: ActorRef, altimeter: ActorRef): Actor = new CoPilot(plane, autopilot, altimeter)

  def newAutopilot: Actor = new AutoPilot
}