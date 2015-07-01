package cwinter.codecraft.util.maths.matrices

class Matrix4x4(val data: Array[Float]) {
  left =>

  assert(data.length == 16)

  def *(right: Matrix4x4): Matrix4x4 = {
    val result = new Matrix4x4(new Array[Float](16))
    for (col <- 0 to 3; row <- 0 to 3)
      result(row, col) =
        (for (i <- 0 to 3) yield left(row, i) * right(i, col)).sum

    result
  }


  @inline final def apply(row: Int, col: Int): Float = data(row + col * 4)

  @inline final def update(row: Int, col: Int, value: Float): Unit = data(row + col * 4) = value

  def transpose(): Unit = {
    for (col <- 1 to 3; row <- 0 until col) {
      val tmp = this(col, row)
      this(col, row) = this(row, col)
      this(row, col) = tmp
    }
  }

  def transposed: Matrix4x4 = {
    val transpose = new Matrix4x4(new Array[Float](data.length))
    for (col <- 0 to 3; row <- 0 to 3) {
      transpose(col, row) = this(row, col)
    }
    transpose
  }

  override def toString: String = {
    for (col <- 0 to 3) yield {
      for (row <- 0 to 3) yield
        this(row, col)
    }.mkString("  ")
  }.mkString("\n", "\n", "\n")


  override def equals(other: Any): Boolean = {
    other match {
      case that: Matrix4x4 =>
        that.canEqual(this) &&
          (for {
            col <- 0 to 3
            row <- 0 to 3
          } yield {
            this(row, col) == that(row, col)
          }).forall(_ == true)
      case _ => false
    }
  }

  def canEqual(other: Any): Boolean =
    other.isInstanceOf[Matrix4x4]
}

