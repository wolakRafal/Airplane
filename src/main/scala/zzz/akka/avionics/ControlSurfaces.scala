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

}

// Pass in the Altimeter as an ActorRef so that we can send messages to it
class ControlSurfaces(altimeter: ActorRef) extends Actor {

  import Altimeter._
  import ControlSurfaces._

  def receive = {
    // Pilot pulled the stick back by a certain amount, and we inform
    // the Altimeter that we're climbing
    case StickBack(amount) =>
      altimeter ! RateChange(amount)
    // Pilot pushes the stick forward and we inform the Altimeter that
    // we're descending
    case StickForward(amount) =>
      altimeter ! RateChange(-1 * amount)
  }
}

