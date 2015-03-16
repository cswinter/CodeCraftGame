package robowars.graphics

import robowars.graphics.materials.Material

import language.existentials

package object model {
  type GenericMaterial = Material[_ <: Vertex, _ <: Vertex, _]
}
