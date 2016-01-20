package cwinter.codecraft.core.objects.drone

import cwinter.codecraft.core.WorldConfig
import cwinter.codecraft.core.api.{DroneSpec, RedPlayer, BluePlayer, Player}
import cwinter.codecraft.core.objects.IDGenerator
import cwinter.codecraft.core.replay.DummyDroneController
import cwinter.codecraft.util.maths.{Vector2, Rectangle}


object DroneFactory {
  val blueDroneContext = mockDroneContext(BluePlayer)
  val redDroneContext = mockDroneContext(RedPlayer)

  def mockDroneContext(player: Player): DroneContext= new DroneContext(
    player,
    WorldConfig(Rectangle(-100, 100, -100, 100)),
    None,
    new IDGenerator(player.id),
    true
  )

  def blueDrone(spec: DroneSpec, position: Vector2): DroneImpl =
    new DroneImpl(spec, new DummyDroneController, blueDroneContext, position, 0)

  def redDrone(spec: DroneSpec, position: Vector2): DroneImpl =
    new DroneImpl(spec, new DummyDroneController, redDroneContext, position, 0)
}
