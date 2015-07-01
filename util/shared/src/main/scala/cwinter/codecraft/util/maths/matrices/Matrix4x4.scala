package cwinter.codecraft.util.maths.matrices

class Matrix4x4(val data: Array[Float]) {
  left =>

  assert(data.length == 16)

  def *(right: Matrix4x4): Matrix4x4 = {
    // code generated with the following expression:
    // (for (col <- 0 to 3; row <- 0 to 3) yield { s"result($row, $col) = " + (for (i <- 0 to 3) yield s"left($row, $i) * right($i, $col)").mkString(" + ") }).mkString("\n")
    // to prevent truncation of output by scala repl:
    // scala> :power
    // scala> vals.issettings.maxPrintString = 1000
    // TODO: benchmark. is this actually faster than while loop?
    // TODO: also, add mutating variant to eliminate array allocation
    // TODO: make it a macro?
    val result = new Matrix4x4(new Array[Float](16))
    result(0, 0) = left(0, 0) * right(0, 0) + left(0, 1) * right(1, 0) + left(0, 2) * right(2, 0) + left(0, 3) * right(3, 0)
    result(1, 0) = left(1, 0) * right(0, 0) + left(1, 1) * right(1, 0) + left(1, 2) * right(2, 0) + left(1, 3) * right(3, 0)
    result(2, 0) = left(2, 0) * right(0, 0) + left(2, 1) * right(1, 0) + left(2, 2) * right(2, 0) + left(2, 3) * right(3, 0)
    result(3, 0) = left(3, 0) * right(0, 0) + left(3, 1) * right(1, 0) + left(3, 2) * right(2, 0) + left(3, 3) * right(3, 0)
    result(0, 1) = left(0, 0) * right(0, 1) + left(0, 1) * right(1, 1) + left(0, 2) * right(2, 1) + left(0, 3) * right(3, 1)
    result(1, 1) = left(1, 0) * right(0, 1) + left(1, 1) * right(1, 1) + left(1, 2) * right(2, 1) + left(1, 3) * right(3, 1)
    result(2, 1) = left(2, 0) * right(0, 1) + left(2, 1) * right(1, 1) + left(2, 2) * right(2, 1) + left(2, 3) * right(3, 1)
    result(3, 1) = left(3, 0) * right(0, 1) + left(3, 1) * right(1, 1) + left(3, 2) * right(2, 1) + left(3, 3) * right(3, 1)
    result(0, 2) = left(0, 0) * right(0, 2) + left(0, 1) * right(1, 2) + left(0, 2) * right(2, 2) + left(0, 3) * right(3, 2)
    result(1, 2) = left(1, 0) * right(0, 2) + left(1, 1) * right(1, 2) + left(1, 2) * right(2, 2) + left(1, 3) * right(3, 2)
    result(2, 2) = left(2, 0) * right(0, 2) + left(2, 1) * right(1, 2) + left(2, 2) * right(2, 2) + left(2, 3) * right(3, 2)
    result(3, 2) = left(3, 0) * right(0, 2) + left(3, 1) * right(1, 2) + left(3, 2) * right(2, 2) + left(3, 3) * right(3, 2)
    result(0, 3) = left(0, 0) * right(0, 3) + left(0, 1) * right(1, 3) + left(0, 2) * right(2, 3) + left(0, 3) * right(3, 3)
    result(1, 3) = left(1, 0) * right(0, 3) + left(1, 1) * right(1, 3) + left(1, 2) * right(2, 3) + left(1, 3) * right(3, 3)
    result(2, 3) = left(2, 0) * right(0, 3) + left(2, 1) * right(1, 3) + left(2, 2) * right(2, 3) + left(2, 3) * right(3, 3)
    result(3, 3) = left(3, 0) * right(0, 3) + left(3, 1) * right(1, 3) + left(3, 2) * right(2, 3) + left(3, 3) * right(3, 3)
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

