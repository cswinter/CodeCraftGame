package robowars.graphics.model


trait Vertex extends {
  def apply(i: Int): Float
}

case class VertexXY(x: Float, y: Float) extends Vertex {
  def apply(i: Int) = i match {
    case 0 => x
    case 1 => y
    case _ => throw new IndexOutOfBoundsException("VertexXY only has 2 components.")
  }

  def +(other: VertexXY): VertexXY = VertexXY(x + other.x, y + other.y)
  def -(other: VertexXY): VertexXY = VertexXY(x - other.x, y - other.y)
  def /(a: Float): VertexXY = VertexXY(x / a, y / a)
  def *(a: Float): VertexXY = VertexXY(x * a, y * a)

  def dot(other: VertexXY): Float = x * other.x + y * other.y

  def size: Float = math.sqrt(x * x + y * y).toFloat

  def normalized: VertexXY = this * (1 / size)
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
    case _ => throw new IndexOutOfBoundsException("ColorRGB only has 3 components.")
  }
}

case class ColorRGBA(r: Float, g: Float, b: Float, a: Float) extends Vertex {
  def apply(i: Int) = i match {
    case 0 => r
    case 1 => g
    case 2 => b
    case 3 => a
    case _ => throw new IndexOutOfBoundsException(s"Index $i is invalid. ColorRGBA has only 4 components.")
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

  implicit object ColorRGBA extends VertexManifest[ColorRGBA] {
    val nComponents = 4
  }

  implicit object EmptyVertexManifest extends VertexManifest[EmptyVertex.type] {
    val nComponents = 0
  }
}

