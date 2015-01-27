package robowars.graphics.model

import robowars.graphics.materials.Material
import robowars.graphics.matrices._

trait ParameterizedConcreteModel[TParams] extends ConcreteModel with Parameterized[TParams] {

  val parameterSink: Parameterized[TParams]

  abstract override def draw(): Unit = {
    parameterSink.params = params
    super.draw()
  }
}



