package cwinter.codecraft.util.maths

private[codecraft] trait Vertex extends {
  def apply(i: Int): Float
}

private[codecraft] case class VertexXY(x: Float, y: Float) extends Vertex {
  def apply(i: Int) = i match {
    case 0 => x
    case 1 => y
    case _ => throw new IndexOutOfBoundsException("VertexXY only has 2 components.")
  }

  def +(other: VertexXY): VertexXY = VertexXY(x + other.x, y + other.y)
  def -(other: VertexXY): VertexXY = VertexXY(x - other.x, y - other.y)
  def /(a: Float): VertexXY = VertexXY(x / a, y / a)
  def *(a: Float): VertexXY = VertexXY(x * a, y * a)

  def unary_- = VertexXY(-x, -y)

  def dot(other: VertexXY): Float = x * other.x + y * other.y

  def size: Float = math.sqrt(x * x + y * y).toFloat

  def perpendicular = VertexXY(-y, x)

  def normalized: VertexXY = this * (1 / size)

  def zPos(z: Float): VertexXYZ = VertexXYZ(x, y, z)

  def direction = math.atan2(y, x).toFloat

  def toVector2: Vector2 = Vector2(x, y)
}


private[codecraft] object VertexXY {
  implicit class Scalar(val value: Float) extends AnyVal {
    def *(vertex: VertexXY) = vertex * value
  }

  def apply(angle: Double): VertexXY =
    VertexXY(math.cos(angle).toFloat, math.sin(angle).toFloat)
}


private[codecraft] object NullVectorXY extends VertexXY(0, 0)


private[codecraft] case class VertexUV(u: Float, y: Float) extends Vertex {
  def apply(i: Int) = i match {
    case 0 => u
    case 1 => y
    case _ => throw new IndexOutOfBoundsException("VertexUV only has 2 components.")
  }
}

private[codecraft] case class VertexXYZ(x: Float, y: Float, z: Float) extends Vertex {
  def apply(i: Int) = i match {
    case 0 => x
    case 1 => y
    case 2 => z
    case _ => throw new IndexOutOfBoundsException("VertexXYZ only has 3 components")
  }
}

private[codecraft] case class ColorRGB(r: Float, g: Float, b: Float) extends Vertex {
  def apply(i: Int) = i match {
    case 0 => r
    case 1 => g
    case 2 => b
    case _ => throw new IndexOutOfBoundsException("ColorRGB only has 3 components.")
  }

  def +(that: ColorRGB): ColorRGB = ColorRGB(r + that.r, g + that.g, b + that.b)
  def -(that: ColorRGB): ColorRGB = ColorRGB(r - that.r, g - that.g, b - that.b)
  def *(a: Float): ColorRGB = ColorRGB(a * r, a * g, a * b)
}

private[codecraft] object ColorRGB {
  implicit class Scalar(val value: Float) extends AnyVal {
    def *(colorRGB: ColorRGB) = ColorRGB(value * colorRGB.r, value * colorRGB.g, value * colorRGB.b)
  }
}

private[codecraft] case class ColorRGBA(r: Float, g: Float, b: Float, a: Float) extends Vertex {
  def *(float: Float): ColorRGBA = ColorRGBA(float * r, float * g, float * b, float * a)
  def +(other: ColorRGBA): ColorRGBA = ColorRGBA(r + other.r, g + other.g, b + other.b, a + other.a)

  def apply(i: Int) = i match {
    case 0 => r
    case 1 => g
    case 2 => b
    case 3 => a
    case _ => throw new IndexOutOfBoundsException(s"Index $i is invalid. ColorRGBA has only 4 components.")
  }
}

private[codecraft] object ColorRGBA {
  def apply(baseColor: ColorRGB, alpha: Float): ColorRGBA =
    ColorRGBA(baseColor.r, baseColor.g, baseColor.b, alpha)

  implicit class Scalar(val value: Float) extends AnyVal {
    def *(rhs: ColorRGBA): ColorRGBA = rhs * value
  }
}


private[codecraft] object EmptyVertex extends Vertex {
  def apply(i: Int) =
    throw new IndexOutOfBoundsException("EmptyVertex does not have any components.")
}


private[codecraft] trait VertexManifest[TVertex <: Vertex] {
  val nComponents: Int
}


private[codecraft] object VertexManifest {
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

