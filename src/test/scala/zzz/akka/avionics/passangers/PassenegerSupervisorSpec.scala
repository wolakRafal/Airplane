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

package zzz.akka.avionics.passangers

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest.matchers.MustMatchers
import org.scalatest.{WordSpecLike, BeforeAndAfterAll, WordSpec}
import zzz.akka.avionics.passengers.PassengerSupervisor.{GetPassengerBroadcaster, PassengerBroadcaster}
import zzz.akka.avionics.passengers.{PassengerSupervisor, PassengerProvider}

import scala.concurrent.duration._

// A specialized configuration we'll inject into the
// ActorSystem so we have a known quantity we can test with
object PassengerSupervisorSpec {
  val config = ConfigFactory.parseString( """
zzz.akka.avionics.passengers = [
[ "Kelly Franqui", "23", "A" ],
[ "Tyrone Dotts", "23", "B" ],
[ "Malinda Class", "23", "C" ],
[ "Kenya Jolicoeur", "24", "A" ],
[ "Christian Piche", "24", "B"]
] """)
}

// We don't want to work with "real" passengers. This mock
// passenger will be much easier to verify things with
trait TestPassengerProvider extends PassengerProvider {
  override def newPassenger(callButton: ActorRef): Actor =
    new Actor {
      def receive = {
        case m => callButton ! m
      }
    }
}

// The Test class injects the configuration into the
// ActorSystem
class PassengerSupervisorSpec extends TestKit(ActorSystem("PassengerSupervisorSpec", PassengerSupervisorSpec.config))
                                  with ImplicitSender
                                  with WordSpecLike
                                  with BeforeAndAfterAll
                                  with MustMatchers {

  // Clean up the system when all the tests are done
  override def afterAll() = {
    system.shutdown()
  }

  "PassengerSupervisor" should {
    "work" in {
      // Get our SUT
      val a = system.actorOf(Props(new PassengerSupervisor(testActor) with TestPassengerProvider))
      // Grab the BroadcastRouter
      a ! GetPassengerBroadcaster
      val broadcaster = expectMsgPF() {
        case PassengerBroadcaster(b) =>
          // Exercise the BroadcastRouter
          b ! "Hithere"
          // All 5 passengers should say "Hithere"
          expectMsg("Hithere")
          expectMsg("Hithere")
          expectMsg("Hithere")
          expectMsg("Hithere")
          expectMsg("Hithere")
          // And then nothing else!
          expectNoMsg(100.milliseconds)
          // Return the BroadcastRouter
          b
      }
      // Ensure that the cache works
      a ! GetPassengerBroadcaster
      expectMsg(PassengerBroadcaster(`broadcaster`))
    }
  }
}
