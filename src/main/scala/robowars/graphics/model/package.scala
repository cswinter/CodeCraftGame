package robowars.graphics

import robowars.graphics.materials.Material

import language.existentials

package object model {
  type GenericMaterial = Material[T1 forSome { type T1 <: Vertex}, T2 forSome { type T2 <: Vertex }]
  type GenericModelBuilder = ModelBuilder[T1 forSome { type T1 <: Vertex}, T2 forSome { type T2 <: Vertex }]
}
