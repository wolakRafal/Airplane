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

import akka.actor.{Actor, ActorRef}

// The ControlSurfaces object carries messages for controlling the plane
object ControlSurfaces {

  // amount is a value between -1 and 1. The altimeter ensures that any
  // value outside that range is truncated to be within it.
  case class StickBack(amount: Float)
  case class StickForward(amount: Float)

  //
  case class StickLeft(amount: Float)
  case class StickRight(amount: Float)
  case class HasControl(somePilot: ActorRef)

}

// Pass in the Altimeter as an ActorRef so that we can send messages to it
class ControlSurfaces(plane: ActorRef,
                      altimeter: ActorRef,
                      heading: ActorRef) extends Actor {

  import Altimeter._
  import ControlSurfaces._
  import HeadingIndicator._
  // Instantiate the receive method by saying that the ControlSurfaces
  // are controlled by the dead letter office. Effectively, this says
  // that nothing's currently in control
  def receive = controlledBy(context.system.deadLetters)

  // As control is transferred between different entities, we will
  // change the instantiated receive function with new variants. This
  // closure ensures that only the assigned pilot can control the plane
  def controlledBy(somePilot: ActorRef): Receive = {
    case StickBack(amount) if sender == somePilot =>
      altimeter ! RateChange(amount)
    case StickForward(amount) if sender == somePilot =>
      altimeter ! RateChange(1 * amount)
    case StickLeft(amount) if sender == somePilot =>
      heading ! BankChange(1 * amount)
    case StickRight(amount) if sender == somePilot =>
      heading ! BankChange(amount)
    // Only the plane can tell us who's currently in control
    case HasControl(entity) if sender == plane =>
      // Become a new instance, where the entity, which the plane told
      // us about, is now the entity that controls the plane
      context.become(controlledBy(entity))
  }

}

