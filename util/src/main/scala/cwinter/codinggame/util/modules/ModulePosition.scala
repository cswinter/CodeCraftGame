package cwinter.codinggame.util.modules

import cwinter.codinggame.util.maths.{NullVectorXY, VertexXY, Geometry}


object ModulePosition {

  def apply(robotSize: Int, moduleIndex: Int): VertexXY = {
    ModulePosition(robotSize)(moduleIndex)
  }

  def apply(robotSize: Int, moduleIndex: Seq[Int]): Seq[VertexXY] = {
    moduleIndex.map(this(robotSize, _))
  }

  def center(robotSize: Int, modulePositions: Seq[Int]): VertexXY = {
    modulePositions.map(i => this(robotSize, i)).reduce(_ + _) / modulePositions.size
  }

  //noinspection ZeroIndexToHead
  val ModulePosition = Map[Int, IndexedSeq[VertexXY]](
    3 -> IndexedSeq(VertexXY(0, 0)),

    4 -> IndexedSeq(VertexXY(9, 9), VertexXY(-9, -9)),

    5 -> Geometry.polygonVertices2(4, radius = 17),

    6 -> permutation(
      Geometry.polygonVertices2(6, radius = 25) :+ NullVectorXY,
      IndexedSeq(1, 0, 5, 6, 4, 3, 2)
    ),

    7 -> permutation(
      Geometry.polygonVertices2(7, radius = 33) ++
        Geometry.polygonVertices(3, orientation = math.Pi.toFloat, radius = 13),
      IndexedSeq(0, 1, 2, 8, 3, 9, 4, 5, 7, 6)
    )
  )


  private def permutation[T](set: IndexedSeq[T], indices: IndexedSeq[Int]): IndexedSeq[T] = {
    IndexedSeq.tabulate(set.size)(i => set(indices(i)))
  }
}

