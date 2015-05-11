package cwinter.codinggame.graphics

import cwinter.codinggame.graphics.materials.Material
import cwinter.codinggame.util.maths.Vertex

import language.existentials

package object model {
  type GenericMaterial = Material[_ <: Vertex, _ <: Vertex, _]
}
