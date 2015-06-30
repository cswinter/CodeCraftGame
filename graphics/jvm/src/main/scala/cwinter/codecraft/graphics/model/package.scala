package cwinter.codecraft.graphics

import cwinter.codecraft.util.maths.Vertex
import cwinter.codecraft.graphics.materials.Material

import language.existentials

package object model {
  type GenericMaterial = Material[_ <: Vertex, _ <: Vertex, _]
}
