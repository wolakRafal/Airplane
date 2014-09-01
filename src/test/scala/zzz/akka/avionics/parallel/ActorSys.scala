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

import scala.util.Random
import akka.testkit.{TestKit, ImplicitSender}
import akka.actor.ActorSystem

class ActorSys(name: String) extends TestKit(ActorSystem(name))
with ImplicitSender
with DelayedInit{

  def this() = this(s"TestSystem${Random.nextInt(5)}")
  def shutdown(): Unit = system.shutdown()
  def delayedInit(f: => Unit): Unit = {
    try {
      f
    } finally {
      shutdown()
    }
  }
}
