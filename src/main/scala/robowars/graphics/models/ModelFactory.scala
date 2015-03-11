package robowars.graphics.models

import robowars.graphics.engine.RenderStack
import robowars.worldstate._


object ModelFactory {
  def generateModel(worldObject: WorldObject)(implicit renderStack: RenderStack): WorldObjectModel = worldObject match {
    case mineral: MineralObject => new MineralObjectModel(mineral)
    case robot: RobotObject => new RobotObjectModel(robot)
    case lightFlash: LightFlash => new LightFlashObjectModel(lightFlash)
    case laserMissile: LaserMissile => new LaserMissileObjectModel(laserMissile)
  }
}
