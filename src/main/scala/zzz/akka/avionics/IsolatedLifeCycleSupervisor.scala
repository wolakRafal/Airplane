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
  * Created : 13 September 2014
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

import akka.actor.Actor

object IsolatedLifeCycleSupervisor {

  // Messages we use in case we want people to be able to wait for
  // us to finish starting
  case object WaitForStart

  case object Started

}

trait IsolatedLifeCycleSupervisor extends Actor {

  import IsolatedLifeCycleSupervisor._

  def receive = {
    // Signify that we've started
    case WaitForStart =>
      sender ! Started
    // We don't handle anything else, but we give a decent
    // error message stating the error
    case m =>
      throw new Exception(s"Don't call ${self.path.name} directly ($m).")
  }

  // To be implemented by subclass
  def childStarter(): Unit

  // Only start the children when we're started
  final override def preStart() {
    childStarter()
  }

  // Don't call preStart(), which would be the default behaviour
  final override def postRestart(reason: Throwable) {}

  // Don't stop the children, which would be the default behaviour
  final override def preRestart(reason: Throwable, message: Option[Any]) {}
}