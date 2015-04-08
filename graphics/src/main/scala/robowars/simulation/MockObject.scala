package robowars.simulation

import robowars.worldstate.WorldObject


trait MockObject {
  val identifier = MockObject.genID()

  def update(): Unit
  def state(): WorldObject
  def dead: Boolean
}

object MockObject {
  private var objectCount = 0
  private def genID(): Int = {
    objectCount += 1
    objectCount
  }
}