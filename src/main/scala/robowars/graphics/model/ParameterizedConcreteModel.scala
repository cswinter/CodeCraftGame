package robowars.graphics.model


trait ParameterizedConcreteModel[TParams] extends ConcreteModel with Parameterized[TParams] {

  val parameterSink: Parameterized[TParams]

  abstract override def draw(): Unit = {
    parameterSink.params = params
    super.draw()
  }
}
