package robowars.graphics.model

/**
 * Created by clemens on 08.01.15.
 */
object EmptyModel extends Model {
   def draw() = { }
   def init() = this
   def +(model: Model) = model
   def project(material: Material) = this
   def hasMaterial(material: Material) = false
 }
