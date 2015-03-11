package robowars.graphics.engine

import robowars.graphics.model._
import robowars.graphics.models._
import robowars.worldstate._


class Visualizer(implicit val renderStack: RenderStack) {
  var models = Map.empty[Int, WorldObjectModel]
  val testModel = new TestModel

  def computeModels(worldState: Iterable[WorldObject]): Iterable[DrawableModel] = {
    assert(
      worldState.groupBy(_.identifier).forall(_._2.size == 1),
      "All identifiers must be unique")

    models = worldState.map { worldObject =>
      if (models.contains(worldObject.identifier))
        (worldObject.identifier, models(worldObject.identifier).update(worldObject))
      else {
        val newModel = ModelFactory.generateModel(worldObject)
        newModel.update(worldObject)
        (worldObject.identifier, newModel)
      }
    }.toMap

    models.values.map(_.model).toSet + testModel.model
  }
}
























