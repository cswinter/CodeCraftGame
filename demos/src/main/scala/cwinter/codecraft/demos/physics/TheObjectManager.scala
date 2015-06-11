package cwinter.codecraft.demos.physics

import cwinter.codecraft.physics.{PhysicsEngine, ConstantVelocityObject}
import cwinter.codecraft.graphics.application.DrawingCanvas
import cwinter.codecraft.util.maths.{Rectangle, Vector2}
import cwinter.codecraft.worldstate.{Simulator, WorldObjectDescriptor}


object TheObjectManager extends Simulator {
  type TObject = ConstantVelocityObject


  val N = 30
  val worldObjects = List.tabulate[MovingObject[TObject]](N * N)(i => {
    val x = i / N
    val y = i % N
    MovingObject(Vector2(-1500 + x * 3000 / N, -1500 + y * 3000 / N))
  })


  val physicsEngine = new PhysicsEngine[ConstantVelocityObject](Rectangle(-2000, 2000, -2000, 2000), 50)
  worldObjects.foreach(x => physicsEngine.addObject(x.objectDynamics.unwrap))


  override def computeWorldState: Iterable[WorldObjectDescriptor] =
    worldObjects.map(_.state)


  override def update(): Unit = {
    physicsEngine.update()
  }



  def main(args: Array[String]): Unit = {
    DrawingCanvas.run(this)
  }
}
