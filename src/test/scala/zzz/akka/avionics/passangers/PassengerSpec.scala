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

import akka.actor.{Props, ActorRef, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{MustMatchers, WordSpecLike}
import zzz.akka.avionics.passengers.Passenger.FastenSeatbelts
import zzz.akka.avionics.passengers.{Passenger, DrinkRequestProbability}
import scala.concurrent.duration._

trait TestDrinkRequestProbability extends DrinkRequestProbability {
  override val askThreshold = 0f
  override val requestMin = 0.milliseconds
  override val requestUpper = 2.milliseconds
}

class PassengersSpec extends TestKit(ActorSystem()) with ImplicitSender with WordSpecLike with MustMatchers {

  import akka.event.Logging.Info
  import akka.testkit.TestProbe

  var seatNumber = 9

  def newPassenger(): ActorRef = {
    seatNumber += 1
    system.actorOf(Props(new Passenger(testActor) with TestDrinkRequestProbability),
      s"Pat_Metheny-$seatNumber-B")
  }

  "Passengers" should {
    "fasten seatbelts when asked" in {
      val a = newPassenger()
      val p = TestProbe()
      system.eventStream.subscribe(p.ref, classOf[Info])
      a ! FastenSeatbelts
      p.expectMsgPF() {
        case Info(_, _, m) =>
          m.toString must include("fastening seatbelt")
      }
    }
  }
}

