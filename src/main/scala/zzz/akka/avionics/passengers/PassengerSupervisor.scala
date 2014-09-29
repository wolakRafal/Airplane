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
  * Created : 28 September 2014
  * Purpose :
  *
  * $Rev$
  * $URL$
  *
  * Notes:
  *
  * ****************************************************************************
  */

package zzz.akka.avionics.passengers

import akka.actor.SupervisorStrategy.{Escalate, Resume, Stop}
import akka.actor._
import akka.routing.BroadcastRouter
import akka.util.Timeout

import scala.collection.immutable
import scala.concurrent.ExecutionContext.Implicits.global

object PassengerSupervisor {

  // Allows someone to request the BroadcastRouter
  case object GetPassengerBroadcaster

  // Returns the BroadcastRouter to the requestor
  case class PassengerBroadcaster(broadcaster: ActorRef)

  // Factory method for easy construction
  def apply(callButton: ActorRef) = new PassengerSupervisor(callButton) with PassengerProvider
}

class PassengerSupervisor(callButton: ActorRef) extends Actor {
  this: PassengerProvider =>

  import zzz.akka.avionics.passengers.PassengerSupervisor._

  // We'll resume our immediate children instead of restarting them
  // on an Exception
  override val supervisorStrategy = OneForOneStrategy() {
    case _: ActorKilledException => Escalate
    case _: ActorInitializationException => Escalate
    case _ => Resume
  }

  // Internal messages we use to communicate between this Actor
  // and its subordinate IsolatedStopSupervisor
  case class GetChildren(forSomeone: ActorRef)

  case class Children(children: immutable.Iterable[ActorRef], childrenFor: ActorRef)

  // We use preStart() to create our IsolatedStopSupervisor

  override def preStart() {
    context.actorOf(Props(new Actor {
      val config = context.system.settings.config

      override val supervisorStrategy = OneForOneStrategy() {
        case _: ActorKilledException => Escalate
        case _: ActorInitializationException => Escalate
        case _ => Stop
      }

      override def preStart() {
        import com.typesafe.config.ConfigList

import scala.collection.JavaConverters._
        // Get our passenger names from the configuration
        val passengers = config.getList("zzz.akka.avionics.passengers")
        // Iterate through them to create the passenger children
        passengers.asScala.foreach { nameWithSeat =>
          val id = nameWithSeat.asInstanceOf[ConfigList].unwrapped(
          ).asScala.mkString("").
            replaceAllLiterally(" ", "_")
          // Convert spaces to underscores to comply with URI standard
          context.actorOf(Props(newPassenger(callButton)), id)
        }
      }

      // Override the IsolatedStopSupervisor's receive
      // method so that our parent can ask us for our
      // created children
      override def receive = {
        case GetChildren(forSomeone: ActorRef) =>
          sender ! Children(context.children, forSomeone)
      }
    }), "PassengersSupervisor")
  }

  import scala.concurrent.duration._
  import akka.pattern.{ask, pipe}
  implicit val askTimeout = Timeout(5.seconds)

  def noRouter: Receive = {
    case GetPassengerBroadcaster =>
      val destinedFor = sender
      val actor = context.actorFor("PassengersSupervisor")
      (actor ? GetChildren).mapTo[Seq[ActorRef]] map { passengers =>
        (Props().withRouter(BroadcastRouter(passengers.asInstanceOf[immutable.Iterable[ActorRef]])), destinedFor)
      } pipeTo self

    case (props: Props, destinedFor: ActorRef) =>
      val router = context.actorOf(props, "Passengers")
      destinedFor ! PassengerBroadcaster(router)
      context.become(withRouter(router))
  }
  def withRouter(router: ActorRef): Receive = {
    case GetPassengerBroadcaster =>
      sender ! PassengerBroadcaster(router)
  }
  def receive = noRouter

}