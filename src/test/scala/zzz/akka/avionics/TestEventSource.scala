package zzz.akka.avionics

import akka.actor.{Actor, ActorSystem}
import akka.testkit.{TestKit, TestActorRef}
import org.scalatest.{ParallelTestExecution, WordSpecLike, BeforeAndAfterAll, MustMatchers}

// We can't test a "trait" very easily, so we're going to create a specific
// EventSource derivation that conforms to the requirements of the trait so
// that we can test the production code.
class TestEventSource extends Actor with ProductionEventSource {

  def receive = eventSourceReceive

}

// "class"Spec is a decent convention we'll be following
class EventSourceSpec extends TestKit(ActorSystem("EventSourceSpec")) with WordSpecLike with MustMatchers with BeforeAndAfterAll with ParallelTestExecution {

  import EventSource._

  override protected def afterAll(): Unit = {
    system.shutdown()
  }

  "EventSource" should {
    "allow us to register a listener" in {
      val real = TestActorRef[TestEventSource].underlyingActor
      real.receive(RegisterListener(testActor))
      real.listeners must contain(testActor)
    }
    "allow us to unregister a listener" in {
      val real = TestActorRef[TestEventSource].underlyingActor
      real.receive(RegisterListener(testActor))
      real.receive(UnregisterListener(testActor))
      real.listeners.size must be(0)
    }
    "send the event to our test actor" in {
      val testA = TestActorRef[TestEventSource]
      testA ! RegisterListener(testActor)
      testA.underlyingActor.sendEvent("Fibonacci")
      expectMsg("Fibonacci")
    }
  }
}