
package zzz.akka.avionics

import akka.actor.{Props, ActorRef}

trait DrinkingProvider {
  def newDrinkingBehaviour(drinker: ActorRef): Props = Props(DrinkingBehaviour(drinker))
}

trait FlyingProvider {
  def newFlyingBehaviour(plane: ActorRef,
                         heading: ActorRef,
                         altimeter: ActorRef): Props =
    Props(new FlyingBehaviour(plane, heading, altimeter))
}
