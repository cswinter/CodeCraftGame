package cwinter.codecraft.graphics

import cwinter.codecraft.graphics.materials.Material
import cwinter.codecraft.util.maths.Vertex

import language.existentials

package object model {
  type GenericMaterial = Material[_ <: Vertex, _ <: Vertex, _]
}
