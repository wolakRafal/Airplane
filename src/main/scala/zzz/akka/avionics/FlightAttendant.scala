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

import akka.actor.Actor
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


// This trait allows us to create different Flight Attendants with different
// levels of responsiveness.
trait AttendantResponsiveness {
  val maxResponseTimeMS: Int
  def responseDuration = scala.util.Random.nextInt(maxResponseTimeMS).millis
}

object FlightAttendant {
  case class GetDrink(drinkname: String)
  case class Drink(drinkname: String)
  // By default we will make attendants that respond within 5 minutes
  def apply() = new FlightAttendant with AttendantResponsiveness {
    val maxResponseTimeMS = 300000
  }
}

class FlightAttendant extends Actor { this: AttendantResponsiveness =>
  import FlightAttendant._

  def receive = {
    case GetDrink(drinkname) =>
      // We don't respond right away, but use the scheduler to ensure we do eventually
      context.system.scheduler.scheduleOnce(responseDuration, sender(),Drink(drinkname))
  }

}

