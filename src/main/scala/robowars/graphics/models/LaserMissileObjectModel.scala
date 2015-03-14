package robowars.graphics.models

import robowars.graphics.engine.RenderStack
import robowars.graphics.model.{ColorRGBA, VertexXY, DrawableModel}
import robowars.graphics.primitives.OldQuadStrip
import robowars.worldstate.{WorldObject, LaserMissile}


class LaserMissileObjectModel(laserMissile: LaserMissile)(implicit val rs: RenderStack)
  extends WorldObjectModel(laserMissile) {

  var model: DrawableModel = null
  update(laserMissile)

  override def update(worldObject: WorldObject): this.type = {
    val midpoints = laserMissile.positions.map { case (x, y) => VertexXY(x, y)}
    val n = laserMissile.positions.length
    val colors = laserMissile.positions.zipWithIndex.map {
      case (_, index) => ColorRGBA(1, 1, 1, index / n.toFloat)
    }

    model =
      new OldQuadStrip(2, midpoints)(renderStack.TranslucentAdditive)
        .colorMidpoints(colors)
        .init()
    super.update(worldObject)

    this
  }
}
