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
  * Created : 14 September 2014
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

import com.typesafe.config.ConfigFactory
import org.scalatest.WordSpecLike
import org.scalatest.MustMatchers
import akka.actor.{ActorRef, Props, ActorSystem, Actor}
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.Await
import akka.pattern.ask
import zzz.akka.avionics.Plane.GiveMeControl


object PilotsSpec {
  val copilotName = "Mary"
  val pilotName = "Mark"
  val configStr = s"""
zzz.akka.avionics.flightcrew.copilotName = "$copilotName"
zzz.akka.avionics.flightcrew.pilotName = "$pilotName" """

}

class PilotsSpec extends TestKit(ActorSystem("PilotsSpec", ConfigFactory.parseString(PilotsSpec.configStr)))
with ImplicitSender
with WordSpecLike
with MustMatchers {

  import PilotsSpec._

  // Helper to make the NilActor easier to create
  def nilActor = system.actorOf(Props[NilActor])

  // These paths are going to prove useful
  val pilotPath = s"/user/TestPilots/$pilotName"
  val copilotPath = s"/user/TestPilots/$copilotName"

  // Helper function to construct the hierarchy we need
  // and ensure that the children are good to go by the
  // time we're done
  def pilotsReadyToGo():ActorRef = {
    // The 'ask' below needs a timeout value
    implicit val askTimeout = Timeout(4.seconds)

    // Much like the creation we're using in the Plane
    val a = system.actorOf(Props(new IsolatedStopSupervisor with OneForOneStrategyFactory {
      def childStarter() {
        context.actorOf(Props[FakePilot], pilotName)
        context.actorOf(Props(new CoPilot(testActor, nilActor, nilActor)), copilotName)
      }
    }), "TestPilots")
    // Wait for the mailboxes to be up and running for the children
    Await.result(a ? IsolatedLifeCycleSupervisor.WaitForStart, 3.seconds)
    // Tell the CoPilot that it's ready to go
    system.actorFor(copilotPath) ! Pilots.ReadyToGo
    a
  }

  // The Test code
  "CoPilot" should{
    "takes controle whe pilot dies" in {
      pilotsReadyToGo()
      // Kill the pilot
      system.actorFor(pilotPath) ! "throw"
      // Since the test class is the "Plane" we can
      // expect to see this request
      expectMsg(GiveMeControl)
      // The girl who sent it had better be Mary
      lastSender must be (system.actorFor(copilotPath))
    }
  }


  }

class FakePilot extends Actor {
  override def receive = {
    case _ =>
      throw new Exception("This exception is expected.")
  }
}

/**
 * We need a couple of ActorRefs around to keep the CoPilot’s constructor
happy (i.e., the AutoPilot and Altimeter instances). For this, we’ll
use a simple NilActor
 */
class NilActor extends Actor {
  def receive = {
    case _ =>
  }
}
