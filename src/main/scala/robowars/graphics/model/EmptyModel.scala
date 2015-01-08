package robowars.graphics.model

object EmptyModel extends Model {
   def draw() = { }
   def init(): InitialisedModel = new InitialisedModel(null, null) // TODO: fix Model hierarchy and do this properly
   def +(model: Model) = model
   def project(material: Material) = this
   def hasMaterial(material: Material) = false
 }
