package cwinter.codecraft.core.objects.drone

import cwinter.codecraft.core.api.{BluePlayer, DroneSpec, Player, RedPlayer, TheGameMaster}
import cwinter.codecraft.core.errors.Errors
import cwinter.codecraft.core.game.{DroneWorldSimulator, WorldConfig}
import cwinter.codecraft.core.objects.IDGenerator
import cwinter.codecraft.core.replay.{NullReplayRecorder, DummyDroneController}
import cwinter.codecraft.graphics.engine.Debug
import cwinter.codecraft.util.maths.{GlobalRNG, Rectangle, Vector2}


object DroneFactory {
  val mockSimulator = new DroneWorldSimulator(
    TheGameMaster.defaultMap, Seq(), t => Seq.empty, forceReplayRecorder = Some(NullReplayRecorder))
  val debug = new Debug
  val errors = new Errors(debug)
  val blueDroneContext = mockDroneContext(BluePlayer)
  val redDroneContext = mockDroneContext(RedPlayer)

  def mockDroneContext(player: Player): DroneContext= new DroneContext(
    player,
    WorldConfig(Rectangle(-100, 100, -100, 100)),
    1,
    None,
    new IDGenerator(player.id),
    GlobalRNG,
    true,
    false,
    mockSimulator,
    debug = debug,
    errors = errors
  )

  def blueDrone(spec: DroneSpec, position: Vector2): DroneImpl =
    new DroneImpl(spec, new DummyDroneController, blueDroneContext, position, 0)

  def redDrone(spec: DroneSpec, position: Vector2): DroneImpl =
    new DroneImpl(spec, new DummyDroneController, redDroneContext, position, 0)
}
