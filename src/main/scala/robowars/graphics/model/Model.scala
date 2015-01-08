package robowars.graphics.model

/**
  * Plan for model hierachy:
  * Base trait:
  * - methods draw()
  * - method init(): Model
  * - method +(model: Mode): Model
  * - method project(material: Material) to get only the component belonging to a specific material
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
  * - sum all ModelBuilder components
  * - call init() to obtain drawable model
  */


trait Model {
   def draw(): Unit
   def init(): Model
   def +(model: Model): Model
   def project(material: Material): Model
   def hasMaterial(material: Material): Boolean
 }
