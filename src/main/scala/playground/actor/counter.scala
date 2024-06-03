package playground.actor

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration.*
import akka.actor.typed.javadsl.Behaviors
import akka.actor.typed.Behavior
import playground.actor.CounterActor.Command
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.util.Timeout
import akka.actor.typed.Scheduler
import akka.actor.typed.ActorSystem
import akka.actor.typed.SpawnProtocol
import java.util.concurrent.atomic.AtomicInteger

trait Counter:
  def increment(): Unit
  def decrement(): Unit
  def get(): Int
  def set(value: Int): Unit

class SimpleCounter extends Counter:
  private var value = 0

  def increment() = value += 1

  def decrement() = value -= 1

  def get() = value

  def set(value: Int) = this.value = value

class SyncCounter extends Counter:
  private var value = 0

  def increment() = synchronized { value += 1 }

  def decrement() = synchronized { value -= 1 }

  def get() = synchronized(value)

  def set(value: Int) = synchronized(this.value = value)

class AtomicCounter extends Counter:
  
  private val value = new AtomicInteger(0)

  def increment() = value.incrementAndGet()

  def decrement() = value.decrementAndGet()

  def get() = value.get()

  def set(value: Int) = this.value.set(value)

class CounterActor(ref: ActorRef[Command])(using sch: Scheduler) extends Counter:
  given Timeout = Timeout(10.seconds)

  def increment() = ref ! Command.Increment

  def decrement() = ref ! Command.Decrement

  def get() = Await.result(ref ? ((replyTo: ActorRef[Int]) => Command.GetValue(replyTo)), 10.seconds)

  def set(value: Int) = ref ! Command.SetValue(value)


object CounterActor:
  enum Command:
    case Increment, Decrement 
    case GetValue(replyTo: ActorRef[Int])
    case SetValue(value: Int)

  def make(value: Int = 0): Behavior[Command] = Behaviors.receiveMessage[Command] {
    case Command.Increment => make(value + 1)
    case Command.Decrement => make(value - 1)
    case Command.SetValue(newValue) => make(newValue)
    case Command.GetValue(repyTo) => 
      repyTo ! value
      Behaviors.same
  }


def time(name: String)(f: => Unit) = {
  val start = System.currentTimeMillis()
  f
  val end = System.currentTimeMillis()
  println("\u2500" * 50)
  println(s"[$name] Time taken: ${end - start} ms")
}

def test(name: String, counter: Counter)  =
  def increment(): Future[Unit] = Future { counter.increment() }
  def decrement(): Future[Unit] = Future { counter.decrement() }

  time(name) {
    val res = Future.traverse((1 to 100000).toList)(_ => increment())
    val dec = Future.traverse((1 to 50000).toList)(_ => decrement())
    val _ = Await.result(res, 10.seconds)
    val _ = Await.result(dec, 10.seconds)
  }
  println("\u2500" * 50)
  println("Counter value: " + counter.get())

@main def counterTest() =
  val system = ActorSystem(SpawnProtocol(), "CounterActor")
  given Scheduler = system.scheduler

  val counterRef = system.systemActorOf(CounterActor.make(), "CounterActor")

  val counter = SimpleCounter()
  val syncCounter = SyncCounter()
  val atomicCounter = AtomicCounter()
  val actorCounter = CounterActor(counterRef)

  test("WarmUp", counter)
  counter.set(0)

  test("SimpleCounter", counter)
  test("SyncCounter", syncCounter)
  test("AtomicCounter", atomicCounter)
  test("ActorCounter", actorCounter)

  system.terminate()
