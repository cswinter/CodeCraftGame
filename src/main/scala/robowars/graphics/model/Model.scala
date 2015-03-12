package robowars.graphics.model

import robowars.graphics.materials.Material

/**
 * Overview of model hierarchy:
 * - Base trait is Model
 * - Model has two subtraits: ComposableModel and DrawableModel
 *
 * ComposableModel:
 * - has + method for composition (this merges VBOs)
 * - has init() method to convert to initialised DrawableModel
 *
 * DrawableModel:
 * - has methods for drawing
 * - allows for product composition with * (no merging of models)
 *
 *
 * init() needs to be called to obtain a drawable object.
 *
 * Several models:
 * ModelBuilder just stores vertices, material
 * cannot be drawn, but can be initialised
 *
 * This returns an ActiveModel (or something).
 * This now only stores a handle to the uploaded vertex data and can be drawn but not modified.
 *
 * Composite model, which can be used to collect several models with different materials.
 *
 * Usage:
 * - create individual components as ModelBuilder
 * - sum all static ModelBuilder components
 * - call init() to obtain drawable model
 */

// TODO: add more doc on * composition, static vs animated, wrapper models

trait Model {
  def project(material: Material[_, _]): Model
  def hasMaterial(material: Material[_ , _]): Boolean
}
