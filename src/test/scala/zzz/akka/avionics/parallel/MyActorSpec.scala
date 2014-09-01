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

package zzz.akka.avionics.parallel

import org.scalatest.{ParallelTestExecution, MustMatchers, WordSpecLike}
import akka.actor.{ActorSystem, Actor, ActorRef, Props}

class MyActorSpec extends WordSpecLike with MustMatchers with ParallelTestExecution {

  def makeActor(system: ActorSystem): ActorRef = system.actorOf(Props[MyActor], "MyActor")

  "My Actor" should{
    "construct without exception" in new ActorSys {
      val a = makeActor(system)
      // The throw will cause the test to fail
    }

    "respond with a Pong to a Ping" in new ActorSys {
      val a = makeActor(system)
      a ! Ping
      expectMsg(Pong)
    }
}
}

  class MyActor extends Actor {

    def receive: Receive = {
      case Ping => sender ! Pong
    }
  }

case object Ping

case object Pong

