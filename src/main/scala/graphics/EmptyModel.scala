package graphics


object EmptyModel extends Model {
   def draw() = { }
   def init() = this
   def +(model: Model) = model
   def project(material: Material) = this
   def hasMaterial(material: Material) = false
 }
