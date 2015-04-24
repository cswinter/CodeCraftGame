package cwinter.graphics

import cwinter.codinggame.util.maths.Vertex
import cwinter.graphics.materials.Material

import language.existentials

package object model {
  type GenericMaterial = Material[_ <: Vertex, _ <: Vertex, _]
}
