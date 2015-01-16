package robowars.graphics.model

class ConcreteModelBuilder[TPosition <: Vertex, TColor <: Vertex]
(material: Material[TPosition, TColor], val vertexData: Seq[(TPosition, TColor)])
  extends ModelBuilder(material)