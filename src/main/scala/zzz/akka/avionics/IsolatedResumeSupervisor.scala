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
  * Created : 13 September 2014
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

import scala.concurrent.duration.Duration
import akka.actor.{ActorInitializationException, ActorKilledException, SupervisorStrategy}
import akka.actor.SupervisorStrategy.{Stop, Resume, Escalate}


abstract class IsolatedResumeSupervisor(maxNrRetries: Int = 1, withinTimeRange: Duration = Duration.Inf) extends IsolatedLifeCycleSupervisor {
  this: SupervisionStrategyFactory =>

  override def supervisorStrategy: SupervisorStrategy = makeStrategy(maxNrRetries, withinTimeRange) {
    case _: ActorInitializationException => Stop
    case _: ActorKilledException => Stop
    case _: Exception => Resume
    case _ => Escalate
  }
}

abstract class IsolatedStopSupervisor(maxNrRetries: Int = 1, withinTimeRange: Duration = Duration.Inf) extends IsolatedLifeCycleSupervisor {
  this: SupervisionStrategyFactory =>

  override def supervisorStrategy: SupervisorStrategy = makeStrategy(maxNrRetries, withinTimeRange) {
    case _: ActorInitializationException => Stop
    case _: ActorKilledException => Stop
    case _: Exception => Stop
    case _ => Escalate
  }
}

