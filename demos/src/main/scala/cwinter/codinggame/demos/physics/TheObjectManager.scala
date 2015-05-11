package cwinter.codinggame.demos.physics

import cwinter.codinggame.graphics.application.DrawingCanvas
import cwinter.codinggame.physics.{ConstantVelocityObject, PhysicsEngine}
import cwinter.codinggame.util.maths.{Rectangle, Vector2}
import cwinter.codinggame.worldstate.{GameWorld, WorldObjectDescriptor}


object TheObjectManager extends GameWorld {
  type TObject = ConstantVelocityObject


  val N = 30
  val worldObjects = List.tabulate[MovingObject[TObject]](N * N)(i => {
    val x = i / N
    val y = i % N
    MovingObject(Vector2(-1500 + x * 3000 / N, -1500 + y * 3000 / N))
  })


  val physicsEngine = new PhysicsEngine[ConstantVelocityObject](Rectangle(-2000, 2000, -2000, 2000), 50)
  worldObjects.foreach(x => physicsEngine.addObject(x.objectDynamics.unwrap))


  def worldState: Iterable[WorldObjectDescriptor] =
    worldObjects.map(_.state)


  def update(): Unit = {
    physicsEngine.update()
  }



  def main(args: Array[String]): Unit = {
    DrawingCanvas.run(this)
  }

  def timestep = 0
}
