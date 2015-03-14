package robowars.graphics.model

import robowars.graphics.materials.Material

class ConcreteModelBuilder[TPosition <: Vertex, TColor <: Vertex]
(material: Material[TPosition, TColor, _], val vertexData: Seq[(TPosition, TColor)])
  extends OldModelBuilder(material)