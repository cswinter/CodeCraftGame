package cwinter.codecraft.core.game

import cwinter.codecraft.core.api._
import cwinter.codecraft.core.replay.{DummyDroneController, NullReplayRecorder}
import cwinter.codecraft.util.maths.{Rectangle, Vector2}
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class TightCollisionTest extends FlatSpec with Matchers {
  val mockDroneSpec = new DroneSpec(storageModules = 2)
  val corneredDrone = new DummyDroneController
  val movingIntoCorner = new DummyDroneController

  val corner = Vector2(2000, 2000)
  val map = new WorldMap(
    Seq(),
    Rectangle(-2000, 2000, -2000, 2000),
    Seq(
      Spawn(mockDroneSpec, corner, BluePlayer),
      Spawn(mockDroneSpec, corner - mockDroneSpec.radius * Vector2(2, 2), BluePlayer)
    )
  )

  val config = map.createGameConfig(Seq(corneredDrone, movingIntoCorner))
  val simulator = new DroneWorldSimulator(config, forceReplayRecorder = Some(NullReplayRecorder))

  "A collision with a drone positioned on the corner" should
    "not send PhysicsEngine into an infinite loop" in {
    movingIntoCorner.moveInDirection(Vector2(1, 1))
    val runFor10Steps = Future {
      simulator.run(10)
    }
    Await.result(runFor10Steps, 1.seconds)
  }

  // TODO: fix this bug and enable test
  "A collision with a drone positioned on the world boundary" should
    "not cause the drones to move through each other" in {}
}
