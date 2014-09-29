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

package zzz.akka.avionics.routers

import akka.actor.{ActorRef, ActorSystem, Props, SupervisorStrategy}
import akka.routing.{Routee, RoutingLogic, Router, RouterConfig}
import akka.dispatch.Dispatchers
import zzz.akka.avionics.LeadFlightAttendantProvider
import zzz.akka.avionics.passengers.Passenger

import scala.collection.immutable.IndexedSeq

abstract class SectionSpecificAttendantRouter extends RouterConfig {
//  this: LeadFlightAttendantProvider =>

  // The RouterConfig requires us to fill out these two
  // fields We know what the supervisorStrategy is but we're
  // only slightly aware of the Dispatcher, which we will be
  // meeting in detail later
  def routerDispatcher: String = Dispatchers.DefaultDispatcherId

  def supervisorStrategy: SupervisorStrategy = SupervisorStrategy.defaultStrategy

  // The createRoute method is what invokes the decision
  // making code. We instantiate the Actors we need and then
  // create the routing code
//  def createRoute(routeeProps: Props,
//                  routeeProvider: RouteeProvider): Route = {
//    // Create 5 flight attendants
//    val attendants = (1 to 5) map { n =>
//      routeeProvider.context.actorOf(Props(newFlightAttendant),
//        "Attendant-" + n)
//    }
//    // Register them with the provider Thisis important.
//    // If you forget to do this, nobody's really going to
//    // tell you about it :)
//    routeeProvider.registerRoutees(attendants)
//      // Now the partial function that calculates the route.
//      // We are going to route based on the name of the
//      // incoming sender. Of course, you would cache this or
//      // do something slicker. {
//
//    }

//  override def createRouter(system: ActorSystem): Router = {
//    // Create 5 flight attendants
//        val attendants = (1 to 5) map { n =>
//          new Routee {
//            val a = system.actorOf(Props(newFlightAttendant),"Attendant-" + n)
//            override def send(message: Any, sender: ActorRef): Unit = a.tell(message, sender)
//          }
//        }
//
//    val logic:RoutingLogic = new RoutingLogic {
//      override def select(message: Any, routees: IndexedSeq[Routee]): Routee = {
//        routees(1)// randmly pick 1 :)
//      }
//    }
//    Router(logic, attendants)
//}
}
