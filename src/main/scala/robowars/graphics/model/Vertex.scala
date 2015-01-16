package robowars.graphics.model


trait Vertex extends {
  def apply(i: Int): Float
}

case class VertexXY(x: Float, y: Float) extends Vertex {
  def apply(i: Int) = i match {
    case 0 => x
    case 1 => y
    case _ => throw new IndexOutOfBoundsException("VerteXY only has 2 components.")
  }
}

case class VertexUV(u: Float, y: Float) extends Vertex {
  def apply(i: Int) = i match {
    case 0 => u
    case 1 => y
    case _ => throw new IndexOutOfBoundsException("VertexUV only has 2 components.")
  }
}

case class VertexXYZ(x: Float, y: Float, z: Float) extends Vertex {
  def apply(i: Int) = i match {
    case 0 => x
    case 1 => y
    case 2 => z
    case _ => throw new IndexOutOfBoundsException("VertexXYZ only has 3 components")
  }
}

case class ColorRGB(r: Float, g: Float, b: Float) extends Vertex {
  def apply(i: Int) = i match {
    case 0 => r
    case 1 => g
    case 2 => b
    case _ => throw new IndexOutOfBoundsException("ColorRGB only has 2 components.")
  }
}


object EmptyVertex extends Vertex {
  def apply(i: Int) =
    throw new IndexOutOfBoundsException("EmptyVertex does not have any components.")
}


trait VertexManifest[TVertex <: Vertex] {
  val nComponents: Int
}


object VertexManifest {
  implicit object VertexXYZ extends VertexManifest[VertexXYZ] {
    val nComponents = 3
  }

  implicit object VertexXY extends VertexManifest[VertexXY] {
    val nComponents = 2
  }

  implicit object VertexUV extends VertexManifest[VertexUV] {
    val nComponents = 2
  }

  implicit object ColorRGB extends VertexManifest[ColorRGB] {
    val nComponents = 3
  }

  implicit object EmptyVertexManifest extends VertexManifest[EmptyVertex.type] {
    val nComponents = 0
  }
}

