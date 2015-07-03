package cwinter.codecraft.demos.physics

import cwinter.codecraft.graphics.engine.GraphicsEngine
import cwinter.codecraft.graphics.worldstate.{Simulator, WorldObjectDescriptor}
import cwinter.codecraft.physics.{ConstantVelocityObject, PhysicsEngine}
import cwinter.codecraft.util.maths.{Rectangle, Vector2}


object TheObjectManager extends Simulator {
  type TObject = ConstantVelocityObject


  val N = 15
  val worldObjects = List.tabulate[MovingObject[TObject]](N * N)(i => {
    val x = i / N
    val y = i % N
    MovingObject(Vector2(-1500 * N / 30 + x * 3000 / 30, -1500 * N / 30 + y * 3000 / 30))
  })


  val physicsEngine = new PhysicsEngine[ConstantVelocityObject](Rectangle(-N * 2000 / 30, N * 2000 / 30, -N * 2000 / 30, N * 2000 / 30), 50)
  worldObjects.foreach(x => physicsEngine.addObject(x.objectDynamics.unwrap))


  override def computeWorldState: Iterable[WorldObjectDescriptor] =
    worldObjects.map(_.state)


  override def update(): Unit = {
    physicsEngine.update()
  }



  def main(args: Array[String]): Unit = {
    GraphicsEngine.run(this)
  }
}

