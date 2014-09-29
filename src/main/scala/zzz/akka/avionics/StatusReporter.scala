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
  * Created : 29 September 2014
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
import akka.util.Timeout

import scala.concurrent.Future


object StatusReporter {

  // The message indicating that status should be reported
  case object ReportStatus

  // The different types of status that can be reported
  sealed trait Status

  case object StatusOK extends Status

  case object StatusNotGreat extends Status

  case object StatusBAD extends Status

}

trait StatusReporter {
  this: Actor =>

  import zzz.akka.avionics.StatusReporter._

  // Abstract implementers  need to define this
  def currentStatus: Status

  // This must be combined with orElse into the
  // ultimate receive method
  def statusReceive: Receive = {
    case ReportStatus =>
      sender ! currentStatus
  }

}

class ExampleHowToCollectStatus extends Actor {

  /* Assuming you have all of this done for the HeadingIndicator, and the
    Altimeter, as well as for other Instruments you’d create (such as a Fuel-Level
    Indicator, Airspeed Indicator, and a whole host of other things), then you can
    get an indication of the status of all Instruments with:*/
  case object GetAllInstrumentStatus

  val instruments = Vector(context.actorFor("Altimeter"), context.actorFor("HeadingIndicator"))
  //actorFor("AirSpeed"), TODO
  //actorFor("FuelSupply")) TODO

  import akka.pattern.{ask, pipe}
  import zzz.akka.avionics.StatusReporter._
  import scala.concurrent.duration._
  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val to =  Timeout(1.second)

  def receive = {
    case GetAllInstrumentStatus =>
      Future.sequence(instruments map { i =>
        (i ? ReportStatus).mapTo[Status]
      }) map { results =>
        if (results.contains(StatusBAD)) StatusBAD
        else if (results.contains(StatusNotGreat)) StatusNotGreat
        else StatusOK
      } pipeTo sender
  }
}