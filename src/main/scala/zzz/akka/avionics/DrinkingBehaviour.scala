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
  * Created : 24 September 2014
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

import akka.actor.{Actor, ActorRef}

import scala.concurrent.duration.Duration

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object DrinkingBehaviour {

  // Internal message that increase / decrease the blood alcohol level
  case class LevelChanged(level: Float)

  // Outbound messages to tell their person how we're feeling
  case object FeelingSober

  case object FeelingTipsy

  case object FeelingLikeZaphod

  // Factory method to instantiate it with the production timer resolution
  def apply(drinker: ActorRef) = new DrinkingBehaviour(drinker) with DrinkingResolution
}

class DrinkingBehaviour(drinker: ActorRef) extends Actor {
  this: DrinkingResolution =>

  import DrinkingBehaviour._
  // Stores the current blood alcohol level
  var currentLevel = 0f

  // Just provides shorter access to the scheduler
  val scheduler = context.system.scheduler
  // As time passes our Pilot sobers up. This scheduler keeps that happening
  val sobering = scheduler.schedule(initialSobering, soberingInterval, self, LevelChanged(-0.0001f))
  // Don't forget to stop your timer when the Actor shuts down
  override def postStop() {
    sobering.cancel()
  }
  // We've got to start the ball rolling with a single drink
  override def preStart() {
    drink()
  }

  // The call to drink() is going to schedule a single event to self that
  // will increase the blood alcohol level by a small amount. It's OK if
  // we don't cancel this one only one message is going to the Dead
  // Letter Office
  def drink() = scheduler.scheduleOnce(drinkInterval(), self, LevelChanged(0.005f))

  def receive = {
    case LevelChanged(amount) =>
      currentLevel = (currentLevel + amount).max(0f)
    // Tell our drinker how we're feeling. It gets more exciting when
    // we start feeling like Zaphod himself, but at that point he stops
    // drinking and lets the sobering timer make him feel better.
    drinker ! (if (currentLevel <= 0.01) {
      drink()
      FeelingSober
    } else if (currentLevel <= 0.03) {
      drink()
      FeelingTipsy
    } else FeelingLikeZaphod)
  }
}

trait DrinkingResolution {
  import scala.util.Random
  def initialSobering: FiniteDuration = 1.second
  def soberingInterval: FiniteDuration = 1.second
  def drinkInterval(): FiniteDuration = Random.nextInt(300).seconds
}

