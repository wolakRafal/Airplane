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

import akka.actor.{Props, ActorRef, Actor}

// The Lead is going to construct its own subordinates.
// We'll have a policy to vary that
trait AttendantCreationPolicy {
  val numberOfAttendants: Int = 8

  def createAttendant: Actor = FlightAttendant()
}

// We'll also provide a mechanism for altering how we create the
// LeadFlightAttendant
trait LeadFlightAttendantProvider {
  def newFlightAttendant: Actor = LeadFlightAttendant()
}

object LeadFlightAttendant {

  case object GetFlightAttendant

  case class Attendant(a: ActorRef)

  def apply() = new LeadFlightAttendant with AttendantCreationPolicy
}

class LeadFlightAttendant extends Actor {
  this: AttendantCreationPolicy =>

  import LeadFlightAttendant._

  // After we've successfully spooled up the LeadFlightAttendant, we're
  // going to have it create all of its subordinates
  override def preStart() {
    import scala.collection.JavaConverters._
    val attendantNames = context.system.settings.config.getStringList("zzz.akka.avionics.flightcrew.attendantNames").asScala
    attendantNames take numberOfAttendants foreach { i =>
      // We create the actors within our context such that they are
      // children of this Actor
        context.actorOf(Props(createAttendant), i)
    }
  }

  // 'children' is an Iterable. This method returns a random one
  def randomAttendant(): ActorRef = {
    context.children.take(
      scala.util.Random.nextInt(numberOfAttendants) + 1).last
  }

  def receive = {
    case GetFlightAttendant =>
      sender ! Attendant(randomAttendant())
    case m =>
      randomAttendant() forward m
  }
}

