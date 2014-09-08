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
  * Created : 01 September 2014
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

import java.util.concurrent.{TimeUnit, CountDownLatch}
import akka.testkit.{TestActorRef, ImplicitSender, TestKit}
import akka.actor.{Props, ActorSystem}
import org.scalatest.{BeforeAndAfterAll, MustMatchers, WordSpecLike}

object EventSourceSpy {
  // The latch gives us fast feedback when something happens
  val latch = new CountDownLatch(1)
}

trait EventSourceSpy extends  EventSource {
  def sendEvent[T](event: T):Unit = EventSourceSpy.latch.countDown()
  // We don't care about processing the messages that EventSource usually
  // processes so we simply don't worry about them.
  def eventSourceReceive = { case "" =>}
}

class AltimeterSpec extends TestKit(ActorSystem("AltimeterSpec"))
                              with ImplicitSender
                              with WordSpecLike
                              with MustMatchers
                              with BeforeAndAfterAll {
  import Altimeter._

  override def afterAll(): Unit = system.shutdown()

  // The slicedAltimeter constructs our Altimeter with the EventSourceSpy
  def slicedAltimeter = new Altimeter with EventSourceSpy

    // This is a helper method that will give us an ActorRef and our plain
    // ol' Altimeter that we can work with directly.
    def actor() = {
      val a = TestActorRef[Altimeter](Props(slicedAltimeter))
      (a, a.underlyingActor)
    }

  "Altimeter" should {
    "record rate of climb changes" in {
      val (_, real) = actor()
      real.receive(RateChange(1f))
      real.rateOfClimb must be (real.maxRateOfClimb)
    }
    "keep rate of climbing changes within bounds" in {
      val (_, real) = actor()
      real.receive(RateChange(2f))
      real.rateOfClimb must be (real.maxRateOfClimb)
    }
    "calculate altitude changes" in {
      val ref = system.actorOf(Props(Altimeter()))
      ref ! EventSource.RegisterListener(testActor)
      ref ! RateChange(1f)
      fishForMessage() {
        case AltitudeUpdate(altitude) if (altitude) == 0f => false
        case AltitudeUpdate(altitude) => true
      }

    }
    "send events" in {
      val (ref,_) = actor()
      EventSourceSpy.latch.await(1,TimeUnit.SECONDS) must be (true)
    }
  }
}

